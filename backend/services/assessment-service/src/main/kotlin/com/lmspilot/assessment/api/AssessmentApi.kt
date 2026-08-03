package com.lmspilot.assessment.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.assessment.domain.*
import com.lmspilot.contracts.ExamSubmittedPayload
import com.lmspilot.contracts.EventTypes
import com.lmspilot.contracts.Permissions
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.InternalTokenAuthorizer
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.UUID

data class QuestionRequest(
    val type: QuestionType,
    @field:NotBlank val prompt: String,
    val options: List<String> = emptyList(),
    val correctAnswers: List<String> = emptyList(),
    val explanation: String? = null,
    @field:Min(1) @field:Max(5) val difficulty: Int = 1,
    val tags: Set<String> = emptySet(),
    @field:DecimalMin("0.0", inclusive = false) val defaultPoints: Double = 1.0,
)
data class QuestionResponse(val id: UUID, val type: QuestionType, val prompt: String, val options: List<String>, val correctAnswers: List<String>, val explanation: String?, val difficulty: Int, val tags: Set<String>, val defaultPoints: Double, val status: QuestionStatus, val version: Int)
data class ExamQuestionInput(val questionId: UUID, @field:DecimalMin("0.0", inclusive=false) val points: Double, @field:Min(0) val sortOrder: Int)
data class ExamRequest(
    @field:NotBlank val title: String, val courseId: UUID, val lessonId: UUID? = null,
    @field:Min(1) @field:Max(480) val durationMinutes: Int = 30,
    val opensAt: Instant? = null, val closesAt: Instant? = null,
    @field:Min(1) @field:Max(20) val maxAttempts: Int = 1,
    @field:Min(0) val waitMinutesBetweenAttempts: Int = 0,
    @field:DecimalMin("0.0") @field:DecimalMax("100.0") val passingScore: Double = 70.0,
    val shuffleQuestions: Boolean = false, val shuffleAnswers: Boolean = false,
    val scoreStrategy: ScoreStrategy = ScoreStrategy.HIGHEST,
    val status: ExamStatus = ExamStatus.DRAFT,
    val questions: List<ExamQuestionInput>,
)
data class ExamQuestionView(val id: UUID, val type: QuestionType, val prompt: String, val options: List<String>, val points: Double, val sortOrder: Int)
data class ExamResponse(
    val id: UUID,
    val title: String,
    val courseId: UUID?,
    val lessonId: UUID?,
    val durationMinutes: Int,
    val opensAt: Instant?,
    val closesAt: Instant?,
    val maxAttempts: Int,
    val waitMinutesBetweenAttempts: Int,
    val passingScore: Double,
    val shuffleQuestions: Boolean,
    val shuffleAnswers: Boolean,
    val scoreStrategy: ScoreStrategy,
    val status: ExamStatus,
    val version: Int,
    val questions: List<ExamQuestionView>,
)
data class StartSessionRequest(val examId: UUID)
data class SaveAnswersRequest(val answers: Map<String, JsonNode>)
data class SessionResponse(val id: UUID, val examId: UUID, val attemptNo: Int, val status: ExamSessionStatus, val startedAt: Instant, val expiresAt: Instant, val submittedAt: Instant?, val answers: Map<String, JsonNode>, val questions: List<ExamQuestionView>)
data class GradingQuestionPayload(val questionId: UUID, val type: QuestionType, val correctAnswers: List<String>, val points: Double)
data class GradingPayload(val sessionId: UUID, val examId: UUID, val userId: UUID, val passingScore: Double, val answers: Map<String, JsonNode>, val questions: List<GradingQuestionPayload>, val courseId: UUID? = null)


@Service
class EnrollmentCourseClient(
    builder: RestClient.Builder,
    @Value("\${enrollment-service.url:http://localhost:8084}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client = builder.baseUrl(baseUrl).build()

    fun activeCourseIds(userId: UUID): Set<UUID> = courseIds("/internal/v1/classes/user/{userId}/courses", userId)

    fun assignedCourseIds(userId: UUID): Set<UUID> = courseIds("/internal/v1/classes/assigned/{userId}/courses", userId)

    private fun courseIds(path: String, userId: UUID): Set<UUID> {
        val values = client.get()
            .uri(path, userId)
            .header("X-Service-Token", serviceToken)
            .retrieve()
            .body(Array<String>::class.java)
            ?: emptyArray()
        return values.map(UUID::fromString).toSet()
    }
}

@Service
class AssessmentManagementService(
    private val questions: QuestionRepository,
    private val exams: ExamRepository,
    private val examQuestions: ExamQuestionRepository,
    private val sessions: ExamSessionRepository,
    private val mapper: ObjectMapper,
    private val events: DomainEventPublisher,
    private val enrollmentCourses: EnrollmentCourseClient,
) {
    @Transactional(readOnly = true)
    fun listQuestions() = (if (isAdmin()) questions.findAll() else questions.findAllByOwnerIdOrderByUpdatedAtDesc(CurrentUser.id()))
        .filter { it.status != QuestionStatus.ARCHIVED }
        .sortedByDescending { it.updatedAt }
        .map { it.response(mapper) }

    @Transactional
    fun createQuestion(input: QuestionRequest): QuestionResponse {
        validateQuestion(input)
        val entity = questions.save(QuestionEntity(ownerId = CurrentUser.id(), type = input.type, prompt = input.prompt.trim(), optionsJson = mapper.writeValueAsString(input.options), correctAnswersJson = mapper.writeValueAsString(input.correctAnswers), explanation = input.explanation, difficulty = input.difficulty, tagsCsv = input.tags.joinToString(","), defaultPoints = input.defaultPoints))
        return entity.response(mapper)
    }

    @Transactional
    fun updateQuestion(id: UUID, input: QuestionRequest): QuestionResponse {
        validateQuestion(input)
        val entity = questions.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "Không tìm thấy câu hỏi") }
        requireOwner(entity.ownerId, "Không thể sửa câu hỏi ngoài phạm vi")
        if (entity.status == QuestionStatus.ARCHIVED) throw ApiException(HttpStatus.CONFLICT, "QUESTION_ARCHIVED", "Không thể sửa câu hỏi đã lưu trữ")
        entity.type = input.type; entity.prompt = input.prompt.trim(); entity.optionsJson = mapper.writeValueAsString(input.options); entity.correctAnswersJson = mapper.writeValueAsString(input.correctAnswers)
        entity.explanation = input.explanation; entity.difficulty = input.difficulty; entity.tagsCsv = input.tags.joinToString(","); entity.defaultPoints = input.defaultPoints
        entity.questionVersion += 1; entity.updatedAt = Instant.now()
        return entity.response(mapper)
    }

    @Transactional
    fun archiveQuestion(id: UUID) {
        val entity = questions.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "Không tìm thấy câu hỏi") }
        requireOwner(entity.ownerId, "Không thể lưu trữ câu hỏi ngoài phạm vi")
        entity.status = QuestionStatus.ARCHIVED
        entity.updatedAt = Instant.now()
    }

    @Transactional
    fun createExam(input: ExamRequest): ExamResponse {
        validateExam(input)
        val exam = exams.save(ExamEntity(title = input.title.trim(), courseId = input.courseId, lessonId = input.lessonId, durationMinutes = input.durationMinutes, opensAt = input.opensAt, closesAt = input.closesAt, maxAttempts = input.maxAttempts, waitMinutesBetweenAttempts = input.waitMinutesBetweenAttempts, passingScore = input.passingScore, shuffleQuestions = input.shuffleQuestions, shuffleAnswers = input.shuffleAnswers, scoreStrategy = input.scoreStrategy, status = input.status, ownerId = CurrentUser.id()))
        saveExamQuestions(exam.id, input.questions)
        return examResponse(exam)
    }

    @Transactional
    fun updateExam(id: UUID, input: ExamRequest): ExamResponse {
        validateExam(input)
        val exam = exams.findById(id).orElseThrow { examNotFound() }
        requireOwner(exam.ownerId, "Bài kiểm tra ngoài phạm vi quản lý")
        if (exam.status == ExamStatus.ARCHIVED) throw ApiException(HttpStatus.CONFLICT, "EXAM_ARCHIVED", "Không thể sửa bài kiểm tra đã lưu trữ")
        if (sessions.existsByExamId(id)) throw ApiException(HttpStatus.CONFLICT, "EXAM_HAS_ATTEMPTS", "Bài kiểm tra đã có lượt làm. Hãy tạo bài kiểm tra mới để thay đổi cấu trúc đề.")

        exam.title = input.title.trim()
        exam.courseId = input.courseId
        exam.lessonId = input.lessonId
        exam.durationMinutes = input.durationMinutes
        exam.opensAt = input.opensAt
        exam.closesAt = input.closesAt
        exam.maxAttempts = input.maxAttempts
        exam.waitMinutesBetweenAttempts = input.waitMinutesBetweenAttempts
        exam.passingScore = input.passingScore
        exam.shuffleQuestions = input.shuffleQuestions
        exam.shuffleAnswers = input.shuffleAnswers
        exam.scoreStrategy = input.scoreStrategy
        exam.status = input.status
        exam.examVersion += 1
        exam.updatedAt = Instant.now()
        examQuestions.deleteAllByExamId(id)
        examQuestions.flush()
        saveExamQuestions(id, input.questions)
        return examResponse(exam)
    }

    @Transactional
    fun archiveExam(id: UUID) {
        val exam = exams.findById(id).orElseThrow { examNotFound() }
        requireOwner(exam.ownerId, "Bài kiểm tra ngoài phạm vi quản lý")
        exam.status = ExamStatus.ARCHIVED
        exam.updatedAt = Instant.now()
    }

    @Transactional(readOnly = true)
    fun listExams(): List<ExamResponse> {
        val source = when {
            CurrentUser.authorities().contains(Permissions.ASSESSMENT_MANAGE) && isAdmin() -> exams.findAll().filter { it.status != ExamStatus.ARCHIVED }.sortedByDescending { it.updatedAt }
            CurrentUser.authorities().contains(Permissions.ASSESSMENT_MANAGE) -> exams.findAllByOwnerIdOrderByUpdatedAtDesc(CurrentUser.id()).filter { it.status != ExamStatus.ARCHIVED }
            else -> {
                val activeCourses = enrollmentCourses.activeCourseIds(CurrentUser.id())
                exams.findAllByStatusOrderByUpdatedAtDesc(ExamStatus.ACTIVE).filter { it.courseId?.let(activeCourses::contains) == true }
            }
        }
        return source.map { examResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getExam(id: UUID): ExamResponse {
        val exam = exams.findById(id).orElseThrow { examNotFound() }
        if (CurrentUser.authorities().contains(Permissions.ASSESSMENT_MANAGE)) {
            requireOwner(exam.ownerId, "Bài kiểm tra ngoài phạm vi quản lý")
        } else {
            val activeCourses = enrollmentCourses.activeCourseIds(CurrentUser.id())
            if (exam.status != ExamStatus.ACTIVE || exam.courseId?.let(activeCourses::contains) != true) throw ApiException(HttpStatus.NOT_FOUND, "EXAM_NOT_FOUND", "Không tìm thấy bài kiểm tra")
        }
        return examResponse(exam)
    }

    @Transactional
    fun start(input: StartSessionRequest): SessionResponse {
        val exam = exams.findById(input.examId).orElseThrow { examNotFound() }
        val now = Instant.now()
        if (exam.status != ExamStatus.ACTIVE) throw ApiException(HttpStatus.CONFLICT, "EXAM_NOT_ACTIVE", "Bài kiểm tra chưa được kích hoạt")
        val activeCourses = enrollmentCourses.activeCourseIds(CurrentUser.id())
        if (exam.courseId?.let(activeCourses::contains) != true) throw ApiException(HttpStatus.FORBIDDEN, "EXAM_NOT_ASSIGNED", "Bài kiểm tra không thuộc khóa học được giao")
        if (exam.opensAt?.isAfter(now) == true || exam.closesAt?.isBefore(now) == true) throw ApiException(HttpStatus.CONFLICT, "EXAM_NOT_OPEN", "Bài kiểm tra không nằm trong thời gian mở")
        val userId = CurrentUser.id()
        val history = sessions.findAllByExamIdAndUserIdOrderByAttemptNoAsc(exam.id, userId)
        history.lastOrNull { it.status == ExamSessionStatus.IN_PROGRESS && it.expiresAt.plusSeconds(30).isAfter(now) }
            ?.let { return sessionResponse(it) }
        val attempts = history.size
        if (attempts >= exam.maxAttempts) throw ApiException(HttpStatus.CONFLICT, "ATTEMPT_LIMIT", "Bạn đã hết số lần làm bài")
        val previous = history.lastOrNull()
        if (previous != null && exam.waitMinutesBetweenAttempts > 0) {
            val allowedAt = (previous.submittedAt ?: previous.expiresAt).plusSeconds(exam.waitMinutesBetweenAttempts * 60L)
            if (allowedAt.isAfter(now)) throw ApiException(HttpStatus.CONFLICT, "RETAKE_WAIT", "Chưa đến thời điểm được làm lại: $allowedAt")
        }
        val session = sessions.save(ExamSessionEntity(examId = exam.id, examVersion = exam.examVersion, userId = userId, attemptNo = attempts + 1, startedAt = now, expiresAt = now.plusSeconds(exam.durationMinutes * 60L)))
        return sessionResponse(session)
    }

    @Transactional
    fun saveAnswers(sessionId: UUID, input: SaveAnswersRequest): SessionResponse {
        val session = ownedSession(sessionId)
        if (session.status != ExamSessionStatus.IN_PROGRESS) throw ApiException(HttpStatus.CONFLICT, "SESSION_LOCKED", "Phiên thi đã khóa")
        if (session.expiresAt.plusSeconds(30).isBefore(Instant.now())) throw ApiException(HttpStatus.CONFLICT, "SESSION_EXPIRED", "Phiên thi đã hết giờ")
        session.answersJson = mapper.writeValueAsString(input.answers)
        session.updatedAt = Instant.now()
        return sessionResponse(session)
    }

    @Transactional
    fun submit(sessionId: UUID, key: String): SessionResponse {
        if (key.isBlank() || key.length > 160) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key không hợp lệ")
        }
        val userId = CurrentUser.id()
        sessions.findBySubmitIdempotencyKey(key)?.let { previous ->
            if (previous.id != sessionId || previous.userId != userId) {
                throw ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "Idempotency-Key đã được dùng cho phiên thi khác")
            }
            return sessionResponse(previous)
        }
        val session = ownedSession(sessionId)
        if (session.status == ExamSessionStatus.SUBMITTED) return sessionResponse(session)
        if (session.status != ExamSessionStatus.IN_PROGRESS) throw ApiException(HttpStatus.CONFLICT, "SESSION_LOCKED", "Phiên thi không thể nộp")
        val now = Instant.now()
        if (session.expiresAt.plusSeconds(30).isBefore(now)) session.status = ExamSessionStatus.EXPIRED else session.status = ExamSessionStatus.SUBMITTED
        session.submittedAt = now; session.submitIdempotencyKey = key; session.updatedAt = now
        events.publish(EventTypes.EXAM_SUBMITTED, "assessment-service", session.id.toString(), ExamSubmittedPayload(session.id, session.examId, session.userId, now))
        return sessionResponse(session)
    }

    @Transactional(readOnly = true)
    fun manageableExamIds(userId: UUID): Set<UUID> {
        val assignedCourses = enrollmentCourses.assignedCourseIds(userId)
        return exams.findAll()
            .filter { it.ownerId == userId || it.courseId?.let(assignedCourses::contains) == true }
            .map { it.id }
            .toSet()
    }

    @Transactional(readOnly = true)
    fun gradingPayload(sessionId: UUID): GradingPayload {
        val session = sessions.findById(sessionId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Không tìm thấy phiên thi") }
        val exam = exams.findById(session.examId).orElseThrow { examNotFound() }
        val q = examQuestions.findAllByExamIdOrderBySortOrderAsc(exam.id).map { GradingQuestionPayload(it.questionId, it.type, mapper.readValue(it.correctAnswersSnapshotJson, object: TypeReference<List<String>>() {}), it.points) }
        return GradingPayload(session.id, exam.id, session.userId, exam.passingScore, readAnswers(session.answersJson), q, exam.courseId)
    }

    private fun saveExamQuestions(examId: UUID, inputs: List<ExamQuestionInput>) {
        inputs.forEach { item ->
            val q = questions.findById(item.questionId).orElseThrow { ApiException(HttpStatus.BAD_REQUEST, "QUESTION_NOT_FOUND", "Câu hỏi ${item.questionId} không tồn tại") }
            requireOwner(q.ownerId, "Không thể dùng câu hỏi ngoài phạm vi")
            if (q.status == QuestionStatus.ARCHIVED) throw ApiException(HttpStatus.CONFLICT, "QUESTION_ARCHIVED", "Không thể dùng câu hỏi đã lưu trữ")
            examQuestions.save(ExamQuestionEntity(examId = examId, questionId = q.id, questionVersion = q.questionVersion, type = q.type, promptSnapshot = q.prompt, optionsSnapshotJson = q.optionsJson, correctAnswersSnapshotJson = q.correctAnswersJson, points = item.points, sortOrder = item.sortOrder))
        }
    }

    private fun validateExam(input: ExamRequest) {
        if (input.closesAt != null && input.opensAt != null && !input.closesAt.isAfter(input.opensAt)) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXAM_WINDOW", "Thời gian đóng phải sau thời gian mở")
        }
        if (input.questions.isEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "EXAM_EMPTY", "Bài kiểm tra phải có câu hỏi")
        if (input.questions.map { it.questionId }.toSet().size != input.questions.size) {
            throw ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_EXAM_QUESTION", "Một câu hỏi không thể xuất hiện hai lần trong cùng bài kiểm tra")
        }
    }

    private fun validateQuestion(input: QuestionRequest) {
        if (input.type in setOf(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE) && input.correctAnswers.isEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "ANSWER_REQUIRED", "Câu khách quan phải có đáp án")
        if (input.type in setOf(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE) && input.options.size < 2) throw ApiException(HttpStatus.BAD_REQUEST, "OPTIONS_REQUIRED", "Câu lựa chọn phải có ít nhất hai phương án")
    }

    private fun examResponse(exam: ExamEntity): ExamResponse = ExamResponse(
        exam.id, exam.title, exam.courseId, exam.lessonId, exam.durationMinutes, exam.opensAt, exam.closesAt,
        exam.maxAttempts, exam.waitMinutesBetweenAttempts, exam.passingScore, exam.shuffleQuestions,
        exam.shuffleAnswers, exam.scoreStrategy, exam.status, exam.examVersion,
        examQuestions.findAllByExamIdOrderBySortOrderAsc(exam.id).map { it.view(mapper) },
    )
    private fun sessionResponse(session: ExamSessionEntity): SessionResponse = SessionResponse(session.id, session.examId, session.attemptNo, session.status, session.startedAt, session.expiresAt, session.submittedAt, readAnswers(session.answersJson), examQuestions.findAllByExamIdOrderBySortOrderAsc(session.examId).map { it.view(mapper) })
    private fun readAnswers(json: String): Map<String, JsonNode> = mapper.readValue(json, object : TypeReference<Map<String, JsonNode>>() {})
    private fun ownedSession(id: UUID): ExamSessionEntity {
        val session = sessions.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Không tìm thấy phiên thi") }
        if (session.userId != CurrentUser.id()) throw ApiException(HttpStatus.FORBIDDEN, "SESSION_OWNER_MISMATCH", "Phiên thi không thuộc người dùng hiện tại")
        return session
    }
    private fun isAdmin() = CurrentUser.roles().contains("ADMIN")
    private fun requireOwner(ownerId: UUID, message: String) {
        if (!isAdmin() && ownerId != CurrentUser.id()) throw ApiException(HttpStatus.FORBIDDEN, "OUT_OF_SCOPE", message)
    }
    private fun examNotFound() = ApiException(HttpStatus.NOT_FOUND, "EXAM_NOT_FOUND", "Không tìm thấy bài kiểm tra")
}

private fun QuestionEntity.response(mapper: ObjectMapper) = QuestionResponse(
    id,
    type,
    prompt,
    mapper.readValue(optionsJson, object: TypeReference<List<String>>() {}),
    mapper.readValue(correctAnswersJson, object: TypeReference<List<String>>() {}),
    explanation,
    difficulty,
    tagsCsv.split(',').filter { it.isNotBlank() }.toSet(),
    defaultPoints,
    status,
    questionVersion,
)
private fun ExamQuestionEntity.view(mapper: ObjectMapper) = ExamQuestionView(questionId, type, promptSnapshot, mapper.readValue(optionsSnapshotJson, object: TypeReference<List<String>>() {}), points, sortOrder)

@RestController
@RequestMapping("/api/v1/questions")
class QuestionController(private val service: AssessmentManagementService) {
    @GetMapping @PreAuthorize("hasAuthority('${Permissions.ASSESSMENT_MANAGE}')") fun list() = service.listQuestions()
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('${Permissions.ASSESSMENT_MANAGE}')") fun create(@Valid @RequestBody input: QuestionRequest) = service.createQuestion(input)
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('${Permissions.ASSESSMENT_MANAGE}')") fun update(@PathVariable id: UUID, @Valid @RequestBody input: QuestionRequest) = service.updateQuestion(id, input)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('${Permissions.ASSESSMENT_MANAGE}')") fun archive(@PathVariable id: UUID) = service.archiveQuestion(id)
}

@RestController
@RequestMapping("/api/v1/exams")
class ExamController(private val service: AssessmentManagementService) {
    @GetMapping @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENT_MANAGE}','${Permissions.ASSESSMENT_TAKE}')") fun list() = service.listExams()
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('${Permissions.ASSESSMENT_MANAGE}')") fun create(@Valid @RequestBody input: ExamRequest) = service.createExam(input)
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('${Permissions.ASSESSMENT_MANAGE}')") fun update(@PathVariable id: UUID, @Valid @RequestBody input: ExamRequest) = service.updateExam(id, input)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('${Permissions.ASSESSMENT_MANAGE}')") fun archive(@PathVariable id: UUID) = service.archiveExam(id)
    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENT_MANAGE}','${Permissions.ASSESSMENT_TAKE}')") fun get(@PathVariable id: UUID) = service.getExam(id)
    @PostMapping("/start") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('${Permissions.ASSESSMENT_TAKE}')") fun start(@Valid @RequestBody input: StartSessionRequest) = service.start(input)
}

@RestController
@RequestMapping("/api/v1/exam-sessions")
class SessionController(private val service: AssessmentManagementService) {
    @PutMapping("/{id}/answers") @PreAuthorize("hasAuthority('${Permissions.ASSESSMENT_TAKE}')") fun save(@PathVariable id: UUID, @Valid @RequestBody input: SaveAnswersRequest) = service.saveAnswers(id, input)
    @PostMapping("/{id}/submit") @PreAuthorize("hasAuthority('${Permissions.ASSESSMENT_TAKE}')") fun submit(@PathVariable id: UUID, @RequestHeader("Idempotency-Key") key: String) = service.submit(id, key)
}

@RestController
@RequestMapping("/internal/v1/assessment")
class InternalAssessmentController(private val service: AssessmentManagementService, private val internal: InternalTokenAuthorizer) {
    @GetMapping("/sessions/{id}/grading-payload")
    fun grading(@PathVariable id: UUID, @RequestHeader("X-Service-Token", required=false) token: String?): GradingPayload {
        internal.require(token)
        return service.gradingPayload(id)
    }

    @GetMapping("/exams/manageable/{userId}")
    fun manageable(@PathVariable userId: UUID, @RequestHeader("X-Service-Token", required=false) token: String?): Set<UUID> {
        internal.require(token)
        return service.manageableExamIds(userId)
    }
}
