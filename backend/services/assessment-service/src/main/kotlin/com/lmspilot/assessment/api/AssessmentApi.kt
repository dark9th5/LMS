package com.lmspilot.assessment.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.assessment.domain.*
import com.lmspilot.assessment.platform.AssessmentContextSpec
import com.lmspilot.assessment.platform.AssessmentContextType
import com.lmspilot.contracts.ExamSubmittedPayload
import com.lmspilot.contracts.EventTypes
import com.lmspilot.contracts.Permissions
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.InternalTokenAuthorizer
import com.lmspilot.support.security.ScopedAuthorizationClient
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
    @field:NotBlank @field:Size(max = 220) val title: String, val courseId: UUID? = null, val lessonId: UUID? = null,
    val contextType: AssessmentContextType? = null, val cohortId: UUID? = null, val autoGrade: Boolean = true,
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
    val contextType: AssessmentContextType,
    val cohortId: UUID?,
    val autoGrade: Boolean,
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
data class StartSessionRequest(val examId: UUID, val enrollmentId: UUID? = null)
data class SaveAnswersRequest(val answers: Map<String, JsonNode>)
data class SessionResponse(val id: UUID, val examId: UUID, val enrollmentId: UUID?, val courseId: UUID?, val lessonId: UUID?, val attemptNo: Int, val status: ExamSessionStatus, val startedAt: Instant, val expiresAt: Instant, val graceUntil: Instant, val lastHeartbeatAt: Instant, val suspiciousEventCount: Int, val submittedAt: Instant?, val answers: Map<String, JsonNode>, val questions: List<ExamQuestionView>)
data class SessionEventRequest(val type: ExamSessionEventType, val details: String? = null, val occurredAt: Instant? = null)
data class SessionEventResponse(val id: UUID, val type: ExamSessionEventType, val details: String?, val occurredAt: Instant, val storedAt: Instant)
data class GradingQuestionPayload(val questionId: UUID, val type: QuestionType, val prompt: String, val correctAnswers: List<String>, val points: Double)
data class GradingPayload(val sessionId: UUID, val examId: UUID, val userId: UUID, val passingScore: Double, val answers: Map<String, JsonNode>, val questions: List<GradingQuestionPayload>, val enrollmentId: UUID? = null, val courseId: UUID? = null, val lessonId: UUID? = null, val autoGrade: Boolean = true, val contextType: AssessmentContextType = AssessmentContextType.STANDALONE_EXAM, val scoreStrategy: ScoreStrategy = ScoreStrategy.HIGHEST, val durationMs: Long = 0, val submittedAt: Instant? = null)
data class EnrollmentValidation(val enrollmentId: UUID, val classId: UUID, val courseId: UUID, val courseVersion: Int, val userId: UUID, val status: String, val dueAt: Instant?)
data class CourseLearningMetadata(
    val courseId: UUID,
    val version: Int,
    val status: String,
    val ownerId: UUID? = null,
    val lessonIds: Set<UUID>,
    val requiredLessonIds: Set<UUID>,
    val lessonTypes: Map<UUID, String> = emptyMap(),
    val lessonFileIds: Set<UUID> = emptySet(),
)


@Service
class EnrollmentCourseClient(
    builder: RestClient.Builder,
    @Value("\${enrollment-service.url:http://localhost:8084}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client = builder.baseUrl(baseUrl).build()

    fun activeCourseIds(userId: UUID): Set<UUID> = courseIds("/internal/v1/course-access/users/{userId}/courses", userId)

    fun assignedCourseIds(userId: UUID): Set<UUID> = courseIds("/internal/v1/course-access/instructors/{userId}/courses", userId)

    fun enrollment(enrollmentId: UUID): EnrollmentValidation = client.get()
        .uri("/internal/v1/enrollments/{id}", enrollmentId)
        .header("X-Service-Token", serviceToken)
        .retrieve()
        .body(EnrollmentValidation::class.java)
        ?: throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ENROLLMENT_SERVICE_UNAVAILABLE", "Không nhận được dữ liệu ghi danh")

    fun activeEnrollments(userId: UUID, courseId: UUID): List<EnrollmentValidation> = client.get()
        .uri("/internal/v1/enrollments/users/{userId}/courses/{courseId}", userId, courseId)
        .header("X-Service-Token", serviceToken)
        .retrieve()
        .body(Array<EnrollmentValidation>::class.java)
        ?.toList()
        ?: emptyList()

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
class AssessmentCourseClient(
    builder: RestClient.Builder,
    @Value("\${course-service.url:http://localhost:8083}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client = builder.baseUrl(baseUrl).build()

    fun learningMetadata(courseId: UUID): CourseLearningMetadata = client.get()
        .uri("/internal/v1/courses/{id}/learning-metadata", courseId)
        .header("X-Service-Token", serviceToken)
        .retrieve()
        .body(CourseLearningMetadata::class.java)
        ?: throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "COURSE_SERVICE_UNAVAILABLE", "Không nhận được cấu trúc khóa học")
}

@Service
class AssessmentManagementService(
    private val questions: QuestionRepository,
    private val exams: ExamRepository,
    private val examQuestions: ExamQuestionRepository,
    private val sessions: ExamSessionRepository,
    private val sessionEvents: ExamSessionEventRepository,
    private val mapper: ObjectMapper,
    private val events: DomainEventPublisher,
    private val enrollmentCourses: EnrollmentCourseClient,
    private val courseLearning: AssessmentCourseClient,
    private val contexts: AssessmentContextRepository,
    private val competitions: CompetitionRepository,
    private val scopedAuthorization: ScopedAuthorizationClient,
    private val audience: AssessmentAudienceService,
) {
    @Transactional(readOnly = true)
    fun listQuestions() = questions.findAllByOwnerIdOrderByUpdatedAtDesc(CurrentUser.id())
        .filter { it.status != QuestionStatus.ARCHIVED }
        .sortedByDescending { it.updatedAt }
        .map { it.response(mapper) }

    @Transactional
    fun createQuestion(input: QuestionRequest): QuestionResponse {
        validateQuestion(input)
        val entity = questions.save(QuestionEntity(ownerId = CurrentUser.id(), type = input.type, prompt = input.prompt.trim(), optionsJson = mapper.writeValueAsString(input.options.map(String::trim)), correctAnswersJson = mapper.writeValueAsString(input.correctAnswers.map(String::trim)), explanation = input.explanation?.trim(), difficulty = input.difficulty, tagsCsv = input.tags.map(String::trim).filter(String::isNotBlank).joinToString(","), defaultPoints = input.defaultPoints))
        return entity.response(mapper)
    }

    @Transactional
    fun updateQuestion(id: UUID, input: QuestionRequest): QuestionResponse {
        validateQuestion(input)
        val entity = questions.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "Không tìm thấy câu hỏi") }
        requireOwner(entity.ownerId, "Không thể sửa câu hỏi ngoài phạm vi")
        if (entity.status == QuestionStatus.ARCHIVED) throw ApiException(HttpStatus.CONFLICT, "QUESTION_ARCHIVED", "Không thể sửa câu hỏi đã lưu trữ")
        entity.type = input.type; entity.prompt = input.prompt.trim(); entity.optionsJson = mapper.writeValueAsString(input.options.map(String::trim)); entity.correctAnswersJson = mapper.writeValueAsString(input.correctAnswers.map(String::trim))
        entity.explanation = input.explanation?.trim(); entity.difficulty = input.difficulty; entity.tagsCsv = input.tags.map(String::trim).filter(String::isNotBlank).joinToString(","); entity.defaultPoints = input.defaultPoints
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
        val context = contextSpec(input)
        requireNewAssessmentScope(context, Permissions.ASSESSMENTS_CREATE)
        val exam = exams.save(ExamEntity(title = input.title.trim(), courseId = context.courseId, lessonId = input.lessonId, durationMinutes = input.durationMinutes, opensAt = input.opensAt, closesAt = input.closesAt, maxAttempts = input.maxAttempts, waitMinutesBetweenAttempts = input.waitMinutesBetweenAttempts, passingScore = input.passingScore, shuffleQuestions = input.shuffleQuestions, shuffleAnswers = input.shuffleAnswers, scoreStrategy = input.scoreStrategy, status = input.status, ownerId = CurrentUser.id()))
        contexts.save(context.entity(exam.id))
        saveExamQuestions(exam.id, input.questions)
        return examResponse(exam)
    }

    @Transactional
    fun updateExam(id: UUID, input: ExamRequest): ExamResponse {
        validateExam(input)
        val exam = exams.findById(id).orElseThrow { examNotFound() }
        requireExamPermission(exam, Permissions.ASSESSMENTS_UPDATE, "Bài kiểm tra ngoài phạm vi quản lý")
        if (exam.status == ExamStatus.ARCHIVED) throw ApiException(HttpStatus.CONFLICT, "EXAM_ARCHIVED", "Không thể sửa bài kiểm tra đã lưu trữ")
        if (sessions.existsByExamId(id)) throw ApiException(HttpStatus.CONFLICT, "EXAM_HAS_ATTEMPTS", "Bài kiểm tra đã có lượt làm. Hãy tạo bài kiểm tra mới để thay đổi cấu trúc đề.")

        val context = contextSpec(input)
        requireCourseExamScope(context.courseId, Permissions.ASSESSMENTS_UPDATE)
        exam.title = input.title.trim()
        exam.courseId = context.courseId
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
        contexts.save(context.entity(id))
        examQuestions.deleteAllByExamId(id)
        examQuestions.flush()
        saveExamQuestions(id, input.questions)
        return examResponse(exam)
    }

    @Transactional
    fun archiveExam(id: UUID) {
        val exam = exams.findById(id).orElseThrow { examNotFound() }
        requireExamPermission(exam, Permissions.ASSESSMENTS_UPDATE, "Bài kiểm tra ngoài phạm vi quản lý")
        exam.status = ExamStatus.ARCHIVED
        exam.updatedAt = Instant.now()
    }

    @Transactional(readOnly = true)
    fun listExams(): List<ExamResponse> {
        val source = when {
            canManageAssessments() -> {
                val assignedCourses = enrollmentCourses.assignedCourseIds(CurrentUser.id())
                val scopedExams = scopedExamIds()
                exams.findAll().filter { exam ->
                    exam.status != ExamStatus.ARCHIVED &&
                        (exam.ownerId == CurrentUser.id() || exam.id in scopedExams || exam.courseId?.let(assignedCourses::contains) == true)
                }.sortedByDescending { it.updatedAt }
            }
            else -> {
                val activeCourses = if (canTakeAssessments()) enrollmentCourses.activeCourseIds(CurrentUser.id()) else emptySet()
                exams.findAllByStatusOrderByUpdatedAtDesc(ExamStatus.ACTIVE).filter { exam ->
                    when (assessmentContextType(exam)) {
                        AssessmentContextType.COMPETITION ->
                            canParticipateCompetitions() && audience.isEligible(exam.id, CurrentUser.id())
                        AssessmentContextType.COURSE_QUIZ, AssessmentContextType.COURSE_ASSIGNMENT ->
                            canTakeAssessments() && exam.courseId?.let(activeCourses::contains) == true
                        AssessmentContextType.STANDALONE_EXAM ->
                            canTakeAssessments() && audience.isEligible(exam.id, CurrentUser.id())
                    }
                }
            }
        }
        return source.map { examResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getExam(id: UUID): ExamResponse {
        val exam = exams.findById(id).orElseThrow { examNotFound() }
        if (canManageAssessments()) {
            requireExamPermission(exam, Permissions.ASSESSMENTS_READ, "Bài kiểm tra ngoài phạm vi quản lý")
        } else {
            val contextType = assessmentContextType(exam)
            val eligible = when (contextType) {
                AssessmentContextType.COMPETITION ->
                    canParticipateCompetitions() && audience.isEligible(exam.id, CurrentUser.id())
                AssessmentContextType.COURSE_QUIZ, AssessmentContextType.COURSE_ASSIGNMENT -> {
                    val activeCourses = if (canTakeAssessments()) enrollmentCourses.activeCourseIds(CurrentUser.id()) else emptySet()
                    canTakeAssessments() && exam.courseId?.let(activeCourses::contains) == true
                }
                AssessmentContextType.STANDALONE_EXAM ->
                    canTakeAssessments() && audience.isEligible(exam.id, CurrentUser.id())
            }
            if (exam.status != ExamStatus.ACTIVE || !eligible) throw ApiException(HttpStatus.NOT_FOUND, "EXAM_NOT_FOUND", "Không tìm thấy bài kiểm tra")
        }
        return examResponse(exam)
    }

    @Transactional
    fun start(input: StartSessionRequest): SessionResponse {
        val candidate = exams.findById(input.examId).orElseThrow { examNotFound() }
        val now = Instant.now()
        val userId = CurrentUser.id()
        if (candidate.status != ExamStatus.ACTIVE) throw ApiException(HttpStatus.CONFLICT, "EXAM_NOT_ACTIVE", "Bài kiểm tra chưa được kích hoạt")
        val contextType = assessmentContextType(candidate)
        if (contextType == AssessmentContextType.COMPETITION) {
            if (!canParticipateCompetitions()) {
                throw ApiException(HttpStatus.FORBIDDEN, "COMPETITION_PERMISSION_REQUIRED", "Bạn không có quyền tham gia cuộc thi")
            }
            val competition = competitions.findById(candidate.id).orElseThrow { examNotFound() }
            if (competition.registrationOpensAt?.isAfter(now) == true || competition.registrationClosesAt?.isBefore(now) == true) {
                throw ApiException(HttpStatus.CONFLICT, "COMPETITION_REGISTRATION_CLOSED", "Cuộc thi chưa mở hoặc đã hết thời gian đăng ký")
            }
        } else if (!canTakeAssessments()) {
            throw ApiException(HttpStatus.FORBIDDEN, "ASSESSMENT_PERMISSION_REQUIRED", "Bạn không có quyền làm bài thi")
        }
        val enrollment = candidate.courseId?.let { resolveEnrollment(input.enrollmentId, userId, it) }
        if (candidate.courseId == null && input.enrollmentId != null) {
            throw ApiException(HttpStatus.BAD_REQUEST, "UNEXPECTED_ENROLLMENT", "Bài thi độc lập không sử dụng ghi danh khóa học")
        }
        if (candidate.courseId == null && !audience.isEligible(candidate.id, CurrentUser.id(), now)) {
            throw ApiException(HttpStatus.FORBIDDEN, "EXAM_NOT_ASSIGNED", "Bạn không thuộc đối tượng được giao bài thi")
        }
        if (candidate.opensAt?.isAfter(now) == true || candidate.closesAt?.isBefore(now) == true) throw ApiException(HttpStatus.CONFLICT, "EXAM_NOT_OPEN", "Bài kiểm tra không nằm trong thời gian mở")
        val exam = exams.findStartSnapshotById(input.examId) ?: throw examNotFound()
        if (exam.examVersion != candidate.examVersion || exam.courseId != candidate.courseId || exam.lessonId != candidate.lessonId) {
            throw ApiException(HttpStatus.CONFLICT, "EXAM_CHANGED_RETRY", "Cấu hình bài thi vừa thay đổi; vui lòng bắt đầu lại")
        }
        if (exam.status != ExamStatus.ACTIVE || exam.opensAt?.isAfter(now) == true || exam.closesAt?.isBefore(now) == true) {
            throw ApiException(HttpStatus.CONFLICT, "EXAM_NOT_OPEN", "Bài kiểm tra không còn trong thời gian mở")
        }
        sessions.lockAttemptSequence("${exam.id}|${enrollment?.enrollmentId ?: userId}")
        val history = if (enrollment != null) {
            val scoped = sessions.findAllByExamIdAndEnrollmentIdOrderByAttemptNoAsc(exam.id, enrollment.enrollmentId)
            val legacy = sessions.findAllByExamIdAndUserIdAndEnrollmentIdIsNullOrderByAttemptNoAsc(exam.id, userId)
                .filter { it.courseId == exam.courseId }
            (scoped + legacy).distinctBy { it.id }.sortedBy { it.attemptNo }
        } else {
            sessions.findAllByExamIdAndUserIdAndEnrollmentIdIsNullOrderByAttemptNoAsc(exam.id, userId)
        }
        history.lastOrNull { it.status == ExamSessionStatus.IN_PROGRESS && it.graceUntil.isAfter(now) }
            ?.let { active ->
                if (enrollment != null && active.enrollmentId != null && active.enrollmentId != enrollment.enrollmentId) {
                    throw ApiException(HttpStatus.CONFLICT, "SESSION_ENROLLMENT_MISMATCH", "Phiên đang làm thuộc một ghi danh khác")
                }
                if (active.enrollmentId == null && enrollment != null) {
                    active.enrollmentId = enrollment.enrollmentId
                    active.courseId = enrollment.courseId
                    active.lessonId = exam.lessonId
                    active.updatedAt = now
                }
                return sessionResponse(active)
            }
        val attempts = history.size
        if (attempts >= exam.maxAttempts) throw ApiException(HttpStatus.CONFLICT, "ATTEMPT_LIMIT", "Bạn đã hết số lần làm bài")
        val previous = history.lastOrNull()
        if (previous != null && exam.waitMinutesBetweenAttempts > 0) {
            val allowedAt = (previous.submittedAt ?: previous.expiresAt).plusSeconds(exam.waitMinutesBetweenAttempts * 60L)
            if (allowedAt.isAfter(now)) throw ApiException(HttpStatus.CONFLICT, "RETAKE_WAIT", "Chưa đến thời điểm được làm lại: $allowedAt")
        }
        val expiresAt = now.plusSeconds(exam.durationMinutes * 60L)
        val session = sessions.save(
            ExamSessionEntity(
                examId = exam.id,
                examVersion = exam.examVersion,
                userId = userId,
                enrollmentId = enrollment?.enrollmentId,
                courseId = exam.courseId,
                lessonId = exam.lessonId,
                attemptNo = attempts + 1,
                startedAt = now,
                expiresAt = expiresAt,
                graceUntil = expiresAt.plusSeconds(120),
                lastHeartbeatAt = now,
            )
        )
        return sessionResponse(session)
    }

    @Transactional
    fun saveAnswers(sessionId: UUID, input: SaveAnswersRequest): SessionResponse {
        val session = ownedSessionForUpdate(sessionId)
        if (session.status != ExamSessionStatus.IN_PROGRESS) throw ApiException(HttpStatus.CONFLICT, "SESSION_LOCKED", "Phiên thi đã khóa")
        if (session.graceUntil.isBefore(Instant.now())) {
            session.status = ExamSessionStatus.EXPIRED
            session.updatedAt = Instant.now()
            return sessionResponse(session)
        }
        val allowedQuestionIds = examQuestions.findAllByExamIdOrderBySortOrderAsc(session.examId).map { it.questionId }.toSet()
        val submittedQuestionIds = input.answers.keys.map { key ->
            runCatching { UUID.fromString(key) }.getOrElse {
                throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_ANSWER_KEY", "Mã câu hỏi trong đáp án không hợp lệ")
            }
        }
        if (submittedQuestionIds.any { it !in allowedQuestionIds }) {
            throw ApiException(HttpStatus.BAD_REQUEST, "ANSWER_QUESTION_MISMATCH", "Đáp án chứa câu hỏi không thuộc phiên thi")
        }
        val serialized = mapper.writeValueAsString(input.answers)
        if (serialized.toByteArray(Charsets.UTF_8).size > 1_048_576) {
            throw ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "ANSWERS_TOO_LARGE", "Dữ liệu đáp án vượt quá giới hạn 1 MiB")
        }
        session.answersJson = serialized
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
        val session = ownedSessionForUpdate(sessionId)
        if (session.status == ExamSessionStatus.SUBMITTED) return sessionResponse(session)
        if (session.status != ExamSessionStatus.IN_PROGRESS) throw ApiException(HttpStatus.CONFLICT, "SESSION_LOCKED", "Phiên thi không thể nộp")
        val now = Instant.now()
        if (session.graceUntil.isBefore(now)) {
            session.status = ExamSessionStatus.EXPIRED
            session.submitIdempotencyKey = key
            session.updatedAt = now
            return sessionResponse(session)
        }
        session.status = ExamSessionStatus.SUBMITTED
        session.submittedAt = now; session.submitIdempotencyKey = key; session.updatedAt = now
        events.publish(EventTypes.EXAM_SUBMITTED, "assessment-service", session.id.toString(), ExamSubmittedPayload(session.id, session.examId, session.userId, now))
        return sessionResponse(session)
    }

    @Transactional
    fun resume(sessionId: UUID): SessionResponse {
        val session = ownedSession(sessionId)
        if (session.status == ExamSessionStatus.IN_PROGRESS && session.graceUntil.isBefore(Instant.now())) {
            session.status = ExamSessionStatus.EXPIRED
            session.updatedAt = Instant.now()
        }
        return sessionResponse(session)
    }

    @Transactional
    fun heartbeat(sessionId: UUID): SessionResponse {
        val session = ownedSession(sessionId)
        if (session.status != ExamSessionStatus.IN_PROGRESS) throw ApiException(HttpStatus.CONFLICT, "SESSION_LOCKED", "Phiên thi đã khóa")
        val now = Instant.now()
        if (session.graceUntil.isBefore(now)) {
            session.status = ExamSessionStatus.EXPIRED
            session.updatedAt = now
            return sessionResponse(session)
        }
        session.lastHeartbeatAt = now
        session.updatedAt = now
        sessionEvents.save(ExamSessionEventEntity(sessionId = session.id, userId = session.userId, type = ExamSessionEventType.HEARTBEAT, occurredAt = now))
        return sessionResponse(session)
    }

    @Transactional
    fun recordSessionEvent(sessionId: UUID, input: SessionEventRequest): SessionEventResponse {
        val session = ownedSession(sessionId)
        if (session.status != ExamSessionStatus.IN_PROGRESS) throw ApiException(HttpStatus.CONFLICT, "SESSION_LOCKED", "Phiên thi đã khóa")
        val now = Instant.now()
        val occurredAt = input.occurredAt ?: now
        if (occurredAt.isAfter(now.plusSeconds(60)) || occurredAt.isBefore(session.startedAt.minusSeconds(60))) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_EVENT_TIME", "Thời điểm sự kiện không hợp lệ")
        }
        val details = input.details?.trim()?.take(2000)
        val event = sessionEvents.save(ExamSessionEventEntity(sessionId = session.id, userId = session.userId, type = input.type, details = details, occurredAt = occurredAt, storedAt = now))
        if (input.type !in setOf(ExamSessionEventType.HEARTBEAT, ExamSessionEventType.NETWORK_RECONNECTED)) {
            session.suspiciousEventCount += 1
        }
        if (input.type == ExamSessionEventType.NETWORK_RECONNECTED) session.lastHeartbeatAt = now
        session.updatedAt = now
        return SessionEventResponse(event.id, event.type, event.details, event.occurredAt, event.storedAt)
    }

    @Transactional(readOnly = true)
    fun sessionEventHistory(sessionId: UUID): List<SessionEventResponse> {
        val session = sessions.findById(sessionId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Không tìm thấy phiên thi") }
        if (session.userId != CurrentUser.id()) requireExamPermission(exams.findById(session.examId).orElseThrow { examNotFound() }, Permissions.ASSESSMENTS_GRADE, "Phiên thi ngoài phạm vi")
        return sessionEvents.findAllBySessionIdOrderByOccurredAtAsc(sessionId).map { SessionEventResponse(it.id, it.type, it.details, it.occurredAt, it.storedAt) }
    }

    @Transactional(readOnly = true)
    fun manageableExamIds(userId: UUID): Set<UUID> {
        val assignedCourses = enrollmentCourses.assignedCourseIds(userId)
        val scopedExams = scopedAuthorization.scopeIdsForUser(userId, Permissions.ASSESSMENTS_GRADE, "EXAM") +
            scopedAuthorization.scopeIdsForUser(userId, Permissions.GRADING_MANAGE, "EXAM") +
            scopedAuthorization.scopeIdsForUser(userId, Permissions.EXAMS_MANAGE, "EXAM")
        return exams.findAll()
            .filter { it.ownerId == userId || it.id in scopedExams || it.courseId?.let(assignedCourses::contains) == true }
            .map { it.id }
            .toSet()
    }

    @Transactional(readOnly = true)
    fun gradingPayload(sessionId: UUID): GradingPayload {
        val session = sessions.findById(sessionId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Không tìm thấy phiên thi") }
        if (session.status !in setOf(ExamSessionStatus.SUBMITTED, ExamSessionStatus.GRADED)) {
            throw ApiException(HttpStatus.CONFLICT, "SESSION_NOT_SUBMITTED", "Phiên thi chưa được nộp hợp lệ")
        }
        val exam = exams.findById(session.examId).orElseThrow { examNotFound() }
        val context = contexts.findById(exam.id).orElse(null)
        val q = examQuestions.findAllByExamIdOrderBySortOrderAsc(exam.id).map { GradingQuestionPayload(it.questionId, it.type, it.promptSnapshot, mapper.readValue(it.correctAnswersSnapshotJson, object: TypeReference<List<String>>() {}), it.points) }
        return GradingPayload(
            session.id,
            exam.id,
            session.userId,
            exam.passingScore,
            readAnswers(session.answersJson),
            q,
            session.enrollmentId,
            session.courseId ?: exam.courseId,
            session.lessonId ?: exam.lessonId,
            context?.autoGrade ?: true,
            context?.contextType ?: if (exam.courseId == null) AssessmentContextType.STANDALONE_EXAM else AssessmentContextType.COURSE_QUIZ,
            exam.scoreStrategy,
            java.time.Duration.between(session.startedAt, session.submittedAt ?: session.updatedAt).toMillis().coerceAtLeast(0),
            session.submittedAt,
        )
    }

    @Transactional
    fun markSessionGraded(sessionId: UUID) {
        val session = sessions.findById(sessionId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Không tìm thấy phiên thi") }
        if (session.status == ExamSessionStatus.GRADED) return
        if (session.status != ExamSessionStatus.SUBMITTED) {
            throw ApiException(HttpStatus.CONFLICT, "SESSION_NOT_SUBMITTED", "Không thể chấm phiên chưa được nộp")
        }
        session.status = ExamSessionStatus.GRADED
        session.updatedAt = Instant.now()
    }

    private fun saveExamQuestions(examId: UUID, inputs: List<ExamQuestionInput>) {
        inputs.forEach { item ->
            val q = questions.findById(item.questionId).orElseThrow { ApiException(HttpStatus.BAD_REQUEST, "QUESTION_NOT_FOUND", "Câu hỏi ${item.questionId} không tồn tại") }
            requireOwner(q.ownerId, "Không thể dùng câu hỏi ngoài phạm vi")
            if (q.status == QuestionStatus.ARCHIVED) throw ApiException(HttpStatus.CONFLICT, "QUESTION_ARCHIVED", "Không thể dùng câu hỏi đã lưu trữ")
            examQuestions.save(ExamQuestionEntity(examId = examId, questionId = q.id, questionVersion = q.questionVersion, type = q.type, promptSnapshot = q.prompt, optionsSnapshotJson = q.optionsJson, correctAnswersSnapshotJson = q.correctAnswersJson, points = item.points, sortOrder = item.sortOrder))
        }
    }

    private fun contextSpec(input: ExamRequest): AssessmentContextSpec {
        val inferred = input.contextType ?: if (input.courseId == null) AssessmentContextType.STANDALONE_EXAM else AssessmentContextType.COURSE_QUIZ
        return runCatching {
            AssessmentContextSpec(inferred, input.courseId, input.cohortId, input.opensAt, input.closesAt, input.maxAttempts, input.autoGrade)
        }.getOrElse { throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_ASSESSMENT_CONTEXT", it.message ?: "Ngữ cảnh bài thi không hợp lệ") }
    }

    private fun AssessmentContextSpec.entity(id: UUID) = AssessmentContextEntity(
        assessmentId = id, contextType = type, courseId = courseId, cohortId = cohortId,
        opensAt = opensAt, closesAt = closesAt, maxAttempts = maxAttempts, autoGrade = autoGrade,
    )

    private fun validateExam(input: ExamRequest) {
        val context = contextSpec(input)
        if (input.closesAt != null && input.opensAt != null && !input.closesAt.isAfter(input.opensAt)) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXAM_WINDOW", "Thời gian đóng phải sau thời gian mở")
        }
        if (input.questions.isEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "EXAM_EMPTY", "Bài kiểm tra phải có câu hỏi")
        if (input.questions.size > 500) throw ApiException(HttpStatus.BAD_REQUEST, "EXAM_TOO_LARGE", "Bài kiểm tra chỉ được có tối đa 500 câu hỏi")
        if (input.questions.map { it.questionId }.toSet().size != input.questions.size) {
            throw ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_EXAM_QUESTION", "Một câu hỏi không thể xuất hiện hai lần trong cùng bài kiểm tra")
        }
        if (context.type == AssessmentContextType.COURSE_QUIZ && input.lessonId == null) {
            throw ApiException(HttpStatus.BAD_REQUEST, "COURSE_QUIZ_LESSON_REQUIRED", "Bài kiểm tra khóa học phải gắn với một bài học loại EXAM")
        }
        if (context.courseId == null && input.lessonId != null) {
            throw ApiException(HttpStatus.BAD_REQUEST, "STANDALONE_EXAM_LESSON", "Kỳ thi độc lập không thể gắn với bài học khóa học")
        }
        if (context.courseId != null && input.lessonId != null) {
            val metadata = courseLearning.learningMetadata(context.courseId)
            if (metadata.lessonTypes[input.lessonId] != "EXAM") {
                throw ApiException(HttpStatus.BAD_REQUEST, "EXAM_LESSON_MISMATCH", "Bài kiểm tra phải gắn với bài học loại EXAM trong đúng khóa học")
            }
        }
    }

    private fun validateQuestion(input: QuestionRequest) {
        if (input.prompt.trim().length > 10_000) throw ApiException(HttpStatus.BAD_REQUEST, "QUESTION_PROMPT_TOO_LONG", "Nội dung câu hỏi vượt quá 10.000 ký tự")
        if (input.options.size > 50 || input.correctAnswers.size > 50 || (input.options + input.correctAnswers).any { it.trim().length !in 1..2_000 }) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUESTION_CHOICES", "Phương án và đáp án phải có nội dung, tối đa 50 mục và 2.000 ký tự mỗi mục")
        }
        val normalizedOptions = input.options.map { it.trim() }
        val normalizedAnswers = input.correctAnswers.map { it.trim() }
        if (normalizedOptions.toSet().size != normalizedOptions.size || normalizedAnswers.toSet().size != normalizedAnswers.size) {
            throw ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_QUESTION_CHOICE", "Phương án và đáp án không được trùng lặp")
        }
        if (input.type in setOf(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE) && input.correctAnswers.isEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "ANSWER_REQUIRED", "Câu khách quan phải có đáp án")
        if (input.type in setOf(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE) && input.options.size < 2) throw ApiException(HttpStatus.BAD_REQUEST, "OPTIONS_REQUIRED", "Câu lựa chọn phải có ít nhất hai phương án")
        if (input.type in setOf(QuestionType.SINGLE_CHOICE, QuestionType.TRUE_FALSE) && normalizedAnswers.size != 1) {
            throw ApiException(HttpStatus.BAD_REQUEST, "SINGLE_ANSWER_REQUIRED", "Câu một lựa chọn phải có đúng một đáp án")
        }
        if (input.type in setOf(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE) && normalizedAnswers.any { it !in normalizedOptions }) {
            throw ApiException(HttpStatus.BAD_REQUEST, "ANSWER_NOT_IN_OPTIONS", "Đáp án đúng phải thuộc danh sách phương án")
        }
    }

    private fun examResponse(exam: ExamEntity): ExamResponse {
        val context = contexts.findById(exam.id).orElse(
            AssessmentContextEntity(
                assessmentId = exam.id,
                contextType = if (exam.courseId == null) AssessmentContextType.STANDALONE_EXAM else AssessmentContextType.COURSE_QUIZ,
                courseId = exam.courseId,
                opensAt = exam.opensAt,
                closesAt = exam.closesAt,
                maxAttempts = exam.maxAttempts,
            )
        )
        return ExamResponse(
        exam.id, exam.title, exam.courseId, exam.lessonId, context.contextType, context.cohortId, context.autoGrade, exam.durationMinutes, exam.opensAt, exam.closesAt,
        exam.maxAttempts, exam.waitMinutesBetweenAttempts, exam.passingScore, exam.shuffleQuestions,
        exam.shuffleAnswers, exam.scoreStrategy, exam.status, exam.examVersion,
        examQuestions.findAllByExamIdOrderBySortOrderAsc(exam.id).map { it.view(mapper) },
        )
    }
    private fun sessionResponse(session: ExamSessionEntity): SessionResponse {
        val exam = exams.findById(session.examId).orElseThrow { examNotFound() }
        var views = examQuestions.findAllByExamIdOrderBySortOrderAsc(session.examId).map { it.view(mapper) }
        if (exam.shuffleQuestions) views = views.shuffled(kotlin.random.Random(session.id.hashCode()))
        if (exam.shuffleAnswers) {
            views = views.map { question ->
                question.copy(options = question.options.shuffled(kotlin.random.Random(session.id.hashCode() xor question.id.hashCode())))
            }
        }
        return SessionResponse(
            session.id,
            session.examId,
            session.enrollmentId,
            session.courseId ?: exam.courseId,
            session.lessonId ?: exam.lessonId,
            session.attemptNo,
            session.status,
            session.startedAt,
            session.expiresAt,
            session.graceUntil,
            session.lastHeartbeatAt,
            session.suspiciousEventCount,
            session.submittedAt,
            readAnswers(session.answersJson),
            views,
        )
    }
    private fun readAnswers(json: String): Map<String, JsonNode> = mapper.readValue(json, object : TypeReference<Map<String, JsonNode>>() {})
    private fun ownedSession(id: UUID): ExamSessionEntity {
        val session = sessions.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Không tìm thấy phiên thi") }
        if (session.userId != CurrentUser.id()) throw ApiException(HttpStatus.FORBIDDEN, "SESSION_OWNER_MISMATCH", "Phiên thi không thuộc người dùng hiện tại")
        return session
    }

    private fun ownedSessionForUpdate(id: UUID): ExamSessionEntity {
        val session = sessions.findForUpdateById(id)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Không tìm thấy phiên thi")
        if (session.userId != CurrentUser.id()) throw ApiException(HttpStatus.FORBIDDEN, "SESSION_OWNER_MISMATCH", "Phiên thi không thuộc người dùng hiện tại")
        return session
    }

    private fun resolveEnrollment(requestedId: UUID?, userId: UUID, courseId: UUID): EnrollmentValidation {
        if (requestedId != null) {
            val enrollment = enrollmentCourses.enrollment(requestedId)
            if (enrollment.userId != userId) {
                throw ApiException(HttpStatus.FORBIDDEN, "ENROLLMENT_OWNER_MISMATCH", "Ghi danh không thuộc người dùng hiện tại")
            }
            if (enrollment.courseId != courseId) {
                throw ApiException(HttpStatus.BAD_REQUEST, "COURSE_ENROLLMENT_MISMATCH", "Ghi danh không thuộc khóa học của bài thi")
            }
            if (enrollment.status == "CANCELLED") {
                throw ApiException(HttpStatus.CONFLICT, "ENROLLMENT_INACTIVE", "Ghi danh đã bị hủy")
            }
            return enrollment
        }
        val active = enrollmentCourses.activeEnrollments(userId, courseId)
        if (active.isEmpty()) {
            throw ApiException(HttpStatus.FORBIDDEN, "EXAM_NOT_ASSIGNED", "Bài kiểm tra không thuộc khóa học được giao")
        }
        if (active.size > 1) {
            throw ApiException(HttpStatus.CONFLICT, "ENROLLMENT_REQUIRED", "Người học có nhiều ghi danh cho khóa học; cần mở bài kiểm tra từ đúng lần ghi danh")
        }
        return active.single()
    }
    private val assessmentManagementPermissions = setOf(
        Permissions.ASSESSMENTS_CREATE, Permissions.ASSESSMENTS_UPDATE, Permissions.ASSESSMENTS_GRADE,
        Permissions.EXAMS_MANAGE, Permissions.ASSESSMENT_MANAGE, Permissions.GRADING_MANAGE,
        Permissions.COMPETITIONS_MANAGE, Permissions.COMPETITIONS_REWARD,
    )

    private fun canManageAssessments() = CurrentUser.authorities().any { it in assessmentManagementPermissions }
    private fun canTakeAssessments() = CurrentUser.authorities().any { it == Permissions.ASSESSMENTS_TAKE || it == Permissions.ASSESSMENT_TAKE }
    private fun canParticipateCompetitions() = Permissions.COMPETITIONS_PARTICIPATE in CurrentUser.authorities()
    private fun assessmentContextType(exam: ExamEntity): AssessmentContextType =
        contexts.findById(exam.id).orElse(null)?.contextType
            ?: if (exam.courseId == null) AssessmentContextType.STANDALONE_EXAM else AssessmentContextType.COURSE_QUIZ
    private fun scopedExamIds(): Set<UUID> =
        scopedAuthorization.scopeIds(Permissions.EXAMS_MANAGE, "EXAM") +
            scopedAuthorization.scopeIds(Permissions.ASSESSMENTS_UPDATE, "EXAM") +
            scopedAuthorization.scopeIds(Permissions.ASSESSMENT_MANAGE, "EXAM") +
            scopedAuthorization.scopeIds(Permissions.ASSESSMENTS_GRADE, "EXAM") +
            scopedAuthorization.scopeIds(Permissions.COMPETITIONS_MANAGE, "EXAM")

    private fun requireNewAssessmentScope(context: AssessmentContextSpec, permission: String) {
        if (context.courseId != null) {
            requireCourseExamScope(context.courseId, permission)
            return
        }
        val systemPermission = permission in CurrentUser.authorities() ||
            Permissions.EXAMS_MANAGE in CurrentUser.authorities() ||
            Permissions.ASSESSMENT_MANAGE in CurrentUser.authorities() ||
            scopedAuthorization.allowed(permission, "SYSTEM", null) ||
            scopedAuthorization.allowed(Permissions.EXAMS_MANAGE, "SYSTEM", null) ||
            scopedAuthorization.allowed(Permissions.ASSESSMENT_MANAGE, "SYSTEM", null) ||
            (context.type == AssessmentContextType.COMPETITION &&
                scopedAuthorization.allowed(Permissions.COMPETITIONS_MANAGE, "SYSTEM", null))
        if (!systemPermission) {
            throw ApiException(HttpStatus.FORBIDDEN, "ASSESSMENT_CREATE_OUT_OF_SCOPE", "Cần quyền toàn hệ thống để tạo bài thi độc lập")
        }
    }

    private fun requireCourseExamScope(courseId: UUID?, permission: String) {
        if (courseId == null) return
        val assigned = courseId in enrollmentCourses.assignedCourseIds(CurrentUser.id())
        val scoped = scopedAuthorization.allowed(permission, "COURSE", courseId) ||
            scopedAuthorization.allowed(Permissions.EXAMS_MANAGE, "COURSE", courseId) ||
            scopedAuthorization.allowed(Permissions.ASSESSMENT_MANAGE, "COURSE", courseId)
        if (!assigned && !scoped) {
            throw ApiException(HttpStatus.FORBIDDEN, "COURSE_ASSESSMENT_OUT_OF_SCOPE", "Không được tạo bài kiểm tra cho khóa học này")
        }
    }

    private fun requireExamPermission(exam: ExamEntity, permission: String, message: String) {
        val assigned = exam.courseId?.let { it in enrollmentCourses.assignedCourseIds(CurrentUser.id()) } == true
        val scoped = scopedAuthorization.allowed(permission, "EXAM", exam.id) ||
            scopedAuthorization.allowed(Permissions.EXAMS_MANAGE, "EXAM", exam.id) ||
            scopedAuthorization.allowed(Permissions.ASSESSMENT_MANAGE, "EXAM", exam.id) ||
            scopedAuthorization.allowed(Permissions.COMPETITIONS_MANAGE, "EXAM", exam.id) ||
            (exam.courseId?.let { scopedAuthorization.allowed(permission, "COURSE", it) } == true)
        if (exam.ownerId != CurrentUser.id() && !assigned && !scoped) {
            throw ApiException(HttpStatus.FORBIDDEN, "OUT_OF_SCOPE", message)
        }
    }

    private fun requireOwner(ownerId: UUID, message: String) {
        if (ownerId != CurrentUser.id()) throw ApiException(HttpStatus.FORBIDDEN, "OUT_OF_SCOPE", message)
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
    @GetMapping @PreAuthorize("hasAnyAuthority('${Permissions.QUESTIONS_MANAGE}','${Permissions.ASSESSMENT_MANAGE}')") fun list() = service.listQuestions()
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('${Permissions.QUESTIONS_MANAGE}','${Permissions.ASSESSMENT_MANAGE}')") fun create(@Valid @RequestBody input: QuestionRequest) = service.createQuestion(input)
    @PutMapping("/{id}") @PreAuthorize("hasAnyAuthority('${Permissions.QUESTIONS_MANAGE}','${Permissions.ASSESSMENT_MANAGE}')") fun update(@PathVariable id: UUID, @Valid @RequestBody input: QuestionRequest) = service.updateQuestion(id, input)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyAuthority('${Permissions.QUESTIONS_MANAGE}','${Permissions.ASSESSMENT_MANAGE}')") fun archive(@PathVariable id: UUID) = service.archiveQuestion(id)
}

@RestController
@RequestMapping("/api/v1/exams")
class ExamController(private val service: AssessmentManagementService) {
    @GetMapping @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_READ}','${Permissions.ASSESSMENTS_CREATE}','${Permissions.ASSESSMENTS_UPDATE}','${Permissions.EXAMS_MANAGE}','${Permissions.ASSESSMENT_MANAGE}','${Permissions.COMPETITIONS_MANAGE}','${Permissions.ASSESSMENTS_TAKE}','${Permissions.ASSESSMENT_TAKE}','${Permissions.COMPETITIONS_PARTICIPATE}')") fun list() = service.listExams()
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_CREATE}','${Permissions.EXAMS_MANAGE}','${Permissions.ASSESSMENT_MANAGE}')") fun create(@Valid @RequestBody input: ExamRequest) = service.createExam(input)
    @PutMapping("/{id}") @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_UPDATE}','${Permissions.EXAMS_MANAGE}','${Permissions.ASSESSMENT_MANAGE}')") fun update(@PathVariable id: UUID, @Valid @RequestBody input: ExamRequest) = service.updateExam(id, input)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_UPDATE}','${Permissions.EXAMS_MANAGE}','${Permissions.ASSESSMENT_MANAGE}')") fun archive(@PathVariable id: UUID) = service.archiveExam(id)
    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_READ}','${Permissions.EXAMS_MANAGE}','${Permissions.ASSESSMENT_MANAGE}','${Permissions.COMPETITIONS_MANAGE}','${Permissions.ASSESSMENTS_TAKE}','${Permissions.ASSESSMENT_TAKE}','${Permissions.COMPETITIONS_PARTICIPATE}')") fun get(@PathVariable id: UUID) = service.getExam(id)
    @PostMapping("/start") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_TAKE}','${Permissions.ASSESSMENT_TAKE}','${Permissions.COMPETITIONS_PARTICIPATE}')") fun start(@Valid @RequestBody input: StartSessionRequest) = service.start(input)
}

@RestController
@RequestMapping("/api/v1/exam-sessions")
class SessionController(private val service: AssessmentManagementService) {
    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_TAKE}','${Permissions.ASSESSMENT_TAKE}','${Permissions.COMPETITIONS_PARTICIPATE}')") fun resume(@PathVariable id: UUID) = service.resume(id)
    @PostMapping("/{id}/heartbeat") @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_TAKE}','${Permissions.ASSESSMENT_TAKE}','${Permissions.COMPETITIONS_PARTICIPATE}')") fun heartbeat(@PathVariable id: UUID) = service.heartbeat(id)
    @PostMapping("/{id}/events") @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_TAKE}','${Permissions.ASSESSMENT_TAKE}','${Permissions.COMPETITIONS_PARTICIPATE}')") fun event(@PathVariable id: UUID, @RequestBody input: SessionEventRequest) = service.recordSessionEvent(id, input)
    @GetMapping("/{id}/events") @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_TAKE}','${Permissions.ASSESSMENT_TAKE}','${Permissions.ASSESSMENTS_GRADE}','${Permissions.GRADING_MANAGE}')") fun events(@PathVariable id: UUID) = service.sessionEventHistory(id)
    @PutMapping("/{id}/answers") @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_TAKE}','${Permissions.ASSESSMENT_TAKE}','${Permissions.COMPETITIONS_PARTICIPATE}')") fun save(@PathVariable id: UUID, @Valid @RequestBody input: SaveAnswersRequest) = service.saveAnswers(id, input)
    @PostMapping("/{id}/submit") @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_TAKE}','${Permissions.ASSESSMENT_TAKE}','${Permissions.COMPETITIONS_PARTICIPATE}')") fun submit(@PathVariable id: UUID, @RequestHeader("Idempotency-Key") key: String) = service.submit(id, key)
}

@RestController
@RequestMapping("/internal/v1/assessment")
class InternalAssessmentController(private val service: AssessmentManagementService, private val internal: InternalTokenAuthorizer) {
    @GetMapping("/sessions/{id}/grading-payload")
    fun grading(@PathVariable id: UUID, @RequestHeader("X-Service-Token", required=false) token: String?): GradingPayload {
        internal.require(token)
        return service.gradingPayload(id)
    }

    @PostMapping("/sessions/{id}/graded")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markGraded(@PathVariable id: UUID, @RequestHeader("X-Service-Token", required=false) token: String?) {
        internal.require(token)
        service.markSessionGraded(id)
    }

    @GetMapping("/exams/manageable/{userId}")
    fun manageable(@PathVariable userId: UUID, @RequestHeader("X-Service-Token", required=false) token: String?): Set<UUID> {
        internal.require(token)
        return service.manageableExamIds(userId)
    }
}

/** Internal import contract used by ai-service after a reviewed question set is approved. */
data class GeneratedQuestionImportRequest(
    val ownerId: UUID,
    val courseId: UUID,
    val questionSet: JsonNode,
)

@RestController
@RequestMapping("/internal/v1/questions")
class InternalQuestionImportController(
    private val questions: QuestionRepository,
    private val provenance: QuestionProvenanceRepository,
    private val mapper: ObjectMapper,
    private val internal: InternalTokenAuthorizer,
) {
    @PostMapping("/import-generated")
    @Transactional
    fun importGenerated(
        @RequestBody input: GeneratedQuestionImportRequest,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ): List<UUID> {
        internal.require(token)
        val root = input.questionSet
        val nodes = root.path("questions")
        if (!nodes.isArray || nodes.isEmpty) {
            throw ApiException(HttpStatus.BAD_REQUEST, "QUESTION_SET_EMPTY", "Bộ câu hỏi không có câu hỏi hợp lệ")
        }
        return nodes.mapIndexed { index, node ->
            val type = runCatching { QuestionType.valueOf(node.path("type").asText()) }.getOrElse {
                throw ApiException(HttpStatus.BAD_REQUEST, "QUESTION_TYPE_INVALID", "Loại câu hỏi không hợp lệ tại vị trí $index")
            }
            val options = node.path("options").map { option -> option.path("text").asText() }
            val optionById = node.path("options").associate { option -> option.path("id").asText() to option.path("text").asText() }
            val correctAnswers = node.path("correctOptionIds").mapNotNull { optionById[it.asText()] }
            val tags = node.path("tags").map { it.asText().trim() }.filter { it.isNotBlank() }.toMutableSet().apply {
                add("ai-generated")
                add("course:${input.courseId}")
            }
            val request = QuestionRequest(
                type = type,
                prompt = node.path("stem").asText().trim(),
                options = options,
                correctAnswers = correctAnswers,
                explanation = node.path("explanation").takeUnless(JsonNode::isMissingNode)?.asText(),
                difficulty = when (node.path("difficulty").asText().uppercase()) {
                    "EASY" -> 1
                    "MEDIUM" -> 3
                    "HARD" -> 5
                    else -> node.path("difficulty").asInt(1).coerceIn(1, 5)
                },
                tags = tags,
                defaultPoints = node.path("points").asDouble(1.0).coerceAtLeast(0.1),
            )
            validateGeneratedQuestion(request, index)
            val entity = questions.save(
                QuestionEntity(
                    ownerId = input.ownerId,
                    type = request.type,
                    prompt = request.prompt,
                    optionsJson = mapper.writeValueAsString(request.options),
                    correctAnswersJson = mapper.writeValueAsString(request.correctAnswers),
                    explanation = request.explanation,
                    difficulty = request.difficulty,
                    tagsCsv = request.tags.joinToString(","),
                    defaultPoints = request.defaultPoints,
                    status = QuestionStatus.DRAFT,
                )
            )
            provenance.save(
                QuestionProvenanceEntity(
                    questionId = entity.id,
                    courseId = input.courseId,
                    externalId = node.path("externalId").asText("q-${index + 1}"),
                    citationsJson = mapper.writeValueAsString(node.path("citations")),
                    sourceDocumentVersionsJson = mapper.writeValueAsString(root.path("source").path("documentVersionIds")),
                    generatorMetadataJson = mapper.writeValueAsString(
                        mapOf(
                            "schemaVersion" to root.path("schemaVersion").asText("1.0"),
                            "provider" to root.path("source").path("provider").asText(null),
                            "model" to root.path("source").path("model").asText(null),
                            "generatedAt" to root.path("source").path("generatedAt").asText(null),
                            "language" to root.path("language").asText("vi"),
                        )
                    ),
                    importedBy = input.ownerId,
                )
            )
            entity.id
        }
    }

    private fun validateGeneratedQuestion(input: QuestionRequest, index: Int) {
        if (input.prompt.isBlank()) throw ApiException(HttpStatus.BAD_REQUEST, "QUESTION_PROMPT_EMPTY", "Câu hỏi ${index + 1} không có nội dung")
        if (input.type in setOf(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE) && input.correctAnswers.isEmpty()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "ANSWER_REQUIRED", "Câu hỏi ${index + 1} không có đáp án đúng")
        }
        if (input.type in setOf(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE) && input.options.size < 2) {
            throw ApiException(HttpStatus.BAD_REQUEST, "OPTIONS_REQUIRED", "Câu hỏi ${index + 1} phải có ít nhất hai phương án")
        }
    }
}
