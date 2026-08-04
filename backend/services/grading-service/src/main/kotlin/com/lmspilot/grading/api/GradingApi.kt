package com.lmspilot.grading.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.*
import com.lmspilot.grading.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.*
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

data class GradingQuestionPayload(val questionId: UUID, val type: String, val prompt: String, val correctAnswers: List<String>, val points: Double)
data class GradingPayload(val sessionId: UUID, val examId: UUID, val userId: UUID, val passingScore: Double, val answers: Map<String, JsonNode>, val questions: List<GradingQuestionPayload>, val enrollmentId: UUID? = null, val courseId: UUID? = null, val lessonId: UUID? = null, val autoGrade: Boolean = true, val contextType: String = "STANDALONE_EXAM", val scoreStrategy: String = "HIGHEST", val durationMs: Long = 0, val submittedAt: Instant? = null)
data class GradeDetail(
    val questionId: UUID,
    val type: String,
    val awarded: Double,
    val maximum: Double,
    val requiresManual: Boolean,
    val prompt: String = "",
    val answer: JsonNode? = null,
)
data class GradeResponse(val id: UUID, val sessionId: UUID, val examId: UUID, val enrollmentId: UUID?, val courseId: UUID?, val lessonId: UUID?, val userId: UUID, val score: Double, val maxScore: Double, val percentage: Double, val passed: Boolean, val status: GradeStatus, val details: List<GradeDetail>, val feedback: String?, val updatedAt: Instant)
data class ManualGradeRequest(@field:DecimalMin("0.0") val score: Double, val feedback: String? = null, val reason: String = "Chấm thủ công")
data class GradeRevisionResponse(val id: UUID, val previousScore: Double, val newScore: Double, val previousPercentage: Double, val newPercentage: Double, val type: GradeRevisionType, val reason: String, val changedBy: UUID, val createdAt: Instant)
data class CreateGradeAppealRequest(val reason: String)
data class ResolveGradeAppealRequest(val status: GradeAppealStatus, val resolution: String, @field:DecimalMin("0.0") val correctedScore: Double? = null)
data class GradeAppealResponse(val id: UUID, val gradeId: UUID, val userId: UUID, val reason: String, val status: GradeAppealStatus, val resolution: String?, val resolvedBy: UUID?, val createdAt: Instant, val updatedAt: Instant, val resolvedAt: Instant?)

@Configuration
class GradingMessagingConfiguration {
    @Bean fun gradingQueue() = Queue("grading.exam-submitted", true)
    @Bean fun gradingBinding(queue: Queue, domainEventExchange: TopicExchange) = BindingBuilder.bind(queue).to(domainEventExchange).with(EventTypes.EXAM_SUBMITTED)
}

@Service
class GradingService(
    private val repository: GradeResultRepository,
    private val revisions: GradeRevisionRepository,
    private val appeals: GradeAppealRepository,
    private val mapper: ObjectMapper,
    private val publisher: DomainEventPublisher,
    @Value("\${services.assessment-url:http://localhost:8086}") assessmentUrl: String,
    @Value("\${lmspilot.internal-token}") private val internalToken: String,
) {
    private val assessment = RestClient.builder().baseUrl(assessmentUrl).build()

    @RabbitListener(queues = ["grading.exam-submitted"])
    @Transactional
    fun onExamSubmitted(event: DomainEventEnvelope) {
        val payload = mapper.treeToValue(event.payload, ExamSubmittedPayload::class.java)
        gradeAutomatically(payload.sessionId)
    }

    @Transactional
    fun gradeAutomatically(sessionId: UUID): GradeResponse {
        repository.lockSession(sessionId.toString())
        repository.findBySessionId(sessionId)?.let { existing ->
            if (existing.status == GradeStatus.COMPLETED) publish(existing)
            return existing.response(mapper)
        }
        val payload = assessment.get().uri("/internal/v1/assessment/sessions/{id}/grading-payload", sessionId)
            .header("X-Service-Token", internalToken).retrieve().body(GradingPayload::class.java)
            ?: throw ApiException(HttpStatus.BAD_GATEWAY, "ASSESSMENT_UNAVAILABLE", "Không lấy được dữ liệu bài thi")
        var earned = 0.0
        var max = 0.0
        var manual = false
        val details = payload.questions.map { q ->
            max += q.points
            val answer = payload.answers[q.questionId.toString()]
            val isManual = !payload.autoGrade || q.type == "ESSAY" || q.type == "SHORT_TEXT"
            val awarded = if (isManual) 0.0 else if (matches(answer, q.correctAnswers, q.type)) q.points else 0.0
            earned += awarded
            manual = manual || isManual
            GradeDetail(q.questionId, q.type, awarded, q.points, isManual, q.prompt, answer)
        }
        val percentage = if (max == 0.0) 0.0 else round(earned * 100.0 / max)
        val entity = repository.save(GradeResultEntity(sessionId = payload.sessionId, examId = payload.examId, enrollmentId = payload.enrollmentId, courseId = payload.courseId, lessonId = payload.lessonId, userId = payload.userId, score = round(earned), maxScore = round(max), percentage = percentage, passingScore = payload.passingScore, passed = !manual && percentage >= payload.passingScore, scoreStrategy = payload.scoreStrategy, status = if (manual) GradeStatus.PENDING_MANUAL else GradeStatus.COMPLETED, detailsJson = mapper.writeValueAsString(details)))
        if (!manual) {
            publish(entity)
            if (payload.contextType == "COMPETITION") recordCompetitionResult(entity, payload)
        }
        return entity.response(mapper)
    }

    @Transactional(readOnly = true)
    fun myGrades() = repository.findAllByUserIdOrderByCreatedAtDesc(CurrentUser.id()).map { it.response(mapper) }

    @Transactional(readOnly = true)
    fun queue(): List<GradeResponse> {
        val source = repository.findAllByStatusOrderByCreatedAtAsc(GradeStatus.PENDING_MANUAL)
        val scoped = if (isAdmin()) source else {
            val allowed = manageableExamIds(CurrentUser.id())
            source.filter { it.examId in allowed }
        }
        return scoped.map { it.response(mapper) }
    }

    @Transactional
    fun completeManual(id: UUID, input: ManualGradeRequest): GradeResponse {
        val entity = repository.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "GRADE_NOT_FOUND", "Không tìm thấy kết quả") }
        requireManageable(entity.examId)
        if (entity.status != GradeStatus.PENDING_MANUAL) throw ApiException(HttpStatus.CONFLICT, "GRADE_NOT_PENDING", "Kết quả không còn ở trạng thái chờ chấm")
        if (input.score > entity.maxScore) throw ApiException(HttpStatus.BAD_REQUEST, "GRADE_EXCEEDS_MAX", "Điểm không được vượt quá điểm tối đa")
        val previousScore = entity.score
        val previousPercentage = entity.percentage
        entity.score = round(input.score)
        entity.percentage = if (entity.maxScore == 0.0) 0.0 else round(entity.score * 100 / entity.maxScore)
        entity.passed = entity.percentage >= entity.passingScore
        entity.status = GradeStatus.COMPLETED
        entity.feedback = input.feedback
        entity.gradedBy = CurrentUser.id()
        entity.updatedAt = Instant.now()
        revisions.save(GradeRevisionEntity(
            gradeId = entity.id,
            previousScore = previousScore,
            newScore = entity.score,
            previousPercentage = previousPercentage,
            newPercentage = entity.percentage,
            type = GradeRevisionType.MANUAL_GRADE,
            reason = input.reason.trim().ifBlank { "Chấm thủ công" },
            changedBy = CurrentUser.id(),
        ))
        publish(entity)
        return entity.response(mapper)
    }

    @Transactional
    fun createAppeal(gradeId: UUID, input: CreateGradeAppealRequest): GradeAppealResponse {
        val grade = repository.findById(gradeId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "GRADE_NOT_FOUND", "Không tìm thấy kết quả") }
        val userId = CurrentUser.id()
        if (grade.userId != userId) throw ApiException(HttpStatus.FORBIDDEN, "GRADE_OWNER_MISMATCH", "Không thể phúc khảo kết quả của người khác")
        if (grade.status != GradeStatus.COMPLETED) throw ApiException(HttpStatus.CONFLICT, "GRADE_NOT_FINAL", "Kết quả chưa được chấm hoàn tất")
        val reason = input.reason.trim()
        if (reason.length !in 10..4000) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_APPEAL_REASON", "Lý do phúc khảo phải từ 10 đến 4000 ký tự")
        appeals.findByGradeIdAndUserIdAndActiveKey(gradeId, userId, "ACTIVE")?.let { return it.appealResponse() }
        val entity = appeals.save(GradeAppealEntity(gradeId = gradeId, userId = userId, reason = reason))
        publisher.publish(EventTypes.GRADE_APPEAL_OPENED, "grading-service", entity.id.toString(), mapOf("appealId" to entity.id, "gradeId" to gradeId, "userId" to userId))
        return entity.appealResponse()
    }

    @Transactional(readOnly = true)
    fun myAppeals() = appeals.findAllByUserIdOrderByCreatedAtDesc(CurrentUser.id()).map { it.appealResponse() }

    @Transactional(readOnly = true)
    fun appealQueue(): List<GradeAppealResponse> {
        val open = appeals.findAllByStatusInOrderByCreatedAtAsc(listOf(GradeAppealStatus.OPEN, GradeAppealStatus.UNDER_REVIEW))
        if (isAdmin()) return open.map { it.appealResponse() }
        val allowed = manageableExamIds(CurrentUser.id())
        val gradeById = repository.findAllById(open.map { it.gradeId }).associateBy { it.id }
        return open.filter { gradeById[it.gradeId]?.examId in allowed }.map { it.appealResponse() }
    }

    @Transactional
    fun resolveAppeal(id: UUID, input: ResolveGradeAppealRequest): GradeAppealResponse {
        if (input.status !in setOf(GradeAppealStatus.APPROVED, GradeAppealStatus.REJECTED)) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_APPEAL_RESOLUTION", "Chỉ có thể phê duyệt hoặc từ chối phúc khảo")
        }
        val appeal = appeals.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "APPEAL_NOT_FOUND", "Không tìm thấy yêu cầu phúc khảo") }
        if (appeal.status !in setOf(GradeAppealStatus.OPEN, GradeAppealStatus.UNDER_REVIEW)) {
            throw ApiException(HttpStatus.CONFLICT, "APPEAL_ALREADY_RESOLVED", "Yêu cầu phúc khảo đã được xử lý")
        }
        val grade = repository.findById(appeal.gradeId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "GRADE_NOT_FOUND", "Không tìm thấy kết quả") }
        requireManageable(grade.examId)
        val resolution = input.resolution.trim()
        if (resolution.length !in 3..4000) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_APPEAL_RESOLUTION", "Nội dung xử lý không hợp lệ")
        if (input.status == GradeAppealStatus.APPROVED && input.correctedScore != null) {
            if (input.correctedScore > grade.maxScore) throw ApiException(HttpStatus.BAD_REQUEST, "GRADE_EXCEEDS_MAX", "Điểm không được vượt quá điểm tối đa")
            val previousScore = grade.score
            val previousPercentage = grade.percentage
            grade.score = round(input.correctedScore)
            grade.percentage = if (grade.maxScore == 0.0) 0.0 else round(grade.score * 100 / grade.maxScore)
            grade.passed = grade.percentage >= grade.passingScore
            grade.gradedBy = CurrentUser.id()
            grade.updatedAt = Instant.now()
            revisions.save(GradeRevisionEntity(
                gradeId = grade.id,
                previousScore = previousScore,
                newScore = grade.score,
                previousPercentage = previousPercentage,
                newPercentage = grade.percentage,
                type = GradeRevisionType.APPEAL_CORRECTION,
                reason = resolution,
                changedBy = CurrentUser.id(),
            ))
            publish(grade)
        }
        appeal.status = input.status
        appeal.activeKey = "CLOSED-${appeal.id}"
        appeal.resolution = resolution
        appeal.resolvedBy = CurrentUser.id()
        appeal.resolvedAt = Instant.now()
        appeal.updatedAt = Instant.now()
        publisher.publish(EventTypes.GRADE_APPEAL_RESOLVED, "grading-service", appeal.id.toString(), mapOf("appealId" to appeal.id, "gradeId" to grade.id, "userId" to appeal.userId, "status" to appeal.status.name, "resolvedBy" to appeal.resolvedBy))
        return appeal.appealResponse()
    }

    @Transactional(readOnly = true)
    fun history(gradeId: UUID): List<GradeRevisionResponse> {
        val grade = repository.findById(gradeId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "GRADE_NOT_FOUND", "Không tìm thấy kết quả") }
        if (grade.userId != CurrentUser.id()) requireManageable(grade.examId)
        return revisions.findAllByGradeIdOrderByCreatedAtDesc(gradeId).map { it.revisionResponse() }
    }


    private fun recordCompetitionResult(entity: GradeResultEntity, payload: GradingPayload) {
        assessment.post()
            .uri("/internal/v1/competitions/{id}/results", entity.examId)
            .header("X-Service-Token", internalToken)
            .body(
                mapOf(
                    "userId" to entity.userId,
                    "attemptId" to entity.sessionId,
                    "score" to entity.percentage,
                    "durationMs" to payload.durationMs,
                    "submittedAt" to (payload.submittedAt ?: Instant.now()),
                )
            )
            .retrieve()
            .toBodilessEntity()
    }

    private fun manageableExamIds(userId: UUID): Set<UUID> {
        val values = assessment.get()
            .uri("/internal/v1/assessment/exams/manageable/{userId}", userId)
            .header("X-Service-Token", internalToken)
            .retrieve()
            .body(Array<String>::class.java)
            ?: emptyArray()
        return values.map(UUID::fromString).toSet()
    }

    private fun requireManageable(examId: UUID) {
        if (!isAdmin() && examId !in manageableExamIds(CurrentUser.id())) {
            throw ApiException(HttpStatus.FORBIDDEN, "GRADE_OUT_OF_SCOPE", "Kết quả ngoài phạm vi được phân công")
        }
    }

    private fun isAdmin() = CurrentUser.isSystemAdmin()

    private fun matches(answer: JsonNode?, expected: List<String>, type: String): Boolean {
        if (answer == null || answer.isNull) return false
        val actual = when {
            answer.isArray -> answer.map { it.asText().trim() }.toSet()
            else -> setOf(answer.asText().trim())
        }
        val target = expected.map { it.trim() }.toSet()
        return if (type == "MULTIPLE_CHOICE") actual == target else actual.firstOrNull().equals(target.firstOrNull(), ignoreCase = true)
    }

    private fun publish(entity: GradeResultEntity) {
        val attempts = if (entity.enrollmentId != null) {
            val scoped = repository.findAllByExamIdAndEnrollmentIdOrderByCreatedAtAsc(entity.examId, entity.enrollmentId!!)
            val legacy = repository.findAllByExamIdAndUserIdAndEnrollmentIdIsNullOrderByCreatedAtAsc(entity.examId, entity.userId)
                .filter { it.courseId == entity.courseId }
            (scoped + legacy).distinctBy { it.id }.sortedBy { it.createdAt }
        } else {
            repository.findAllByExamIdAndUserIdAndEnrollmentIdIsNullOrderByCreatedAtAsc(entity.examId, entity.userId)
        }
        val completedAttempts = attempts
            .filter { it.status == GradeStatus.COMPLETED }
        val effectivePercentage = when (entity.scoreStrategy) {
            "HIGHEST" -> completedAttempts.maxOfOrNull { it.percentage } ?: entity.percentage
            "AVERAGE" -> if (completedAttempts.isEmpty()) entity.percentage else completedAttempts.map { it.percentage }.average()
            else -> completedAttempts.maxByOrNull { it.createdAt }?.percentage ?: entity.percentage
        }
        val roundedEffectivePercentage = round(effectivePercentage)
        val effectivePassed = roundedEffectivePercentage >= entity.passingScore
        publisher.publish(
            EventTypes.EXAM_GRADED,
            "grading-service",
            entity.id.toString(),
            ExamGradedPayload(
                entity.sessionId,
                entity.examId,
                entity.userId,
                entity.score,
                entity.maxScore,
                entity.passed,
                entity.status.name,
                enrollmentId = entity.enrollmentId,
                courseId = entity.courseId,
                lessonId = entity.lessonId,
                effectivePassed = effectivePassed,
                effectivePercentage = roundedEffectivePercentage,
                scoreStrategy = entity.scoreStrategy,
            ),
        )
        assessment.post()
            .uri("/internal/v1/assessment/sessions/{id}/graded", entity.sessionId)
            .header("X-Service-Token", internalToken)
            .retrieve()
            .toBodilessEntity()
    }
    private fun round(value: Double) = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
}

private fun GradeRevisionEntity.revisionResponse() = GradeRevisionResponse(id, previousScore, newScore, previousPercentage, newPercentage, type, reason, changedBy, createdAt)
private fun GradeAppealEntity.appealResponse() = GradeAppealResponse(id, gradeId, userId, reason, status, resolution, resolvedBy, createdAt, updatedAt, resolvedAt)

private fun GradeResultEntity.response(mapper: ObjectMapper) = GradeResponse(id, sessionId, examId, enrollmentId, courseId, lessonId, userId, score, maxScore, percentage, passed, status, mapper.readValue(detailsJson, mapper.typeFactory.constructCollectionType(List::class.java, GradeDetail::class.java)), feedback, updatedAt)

@RestController
@RequestMapping("/api/v1/grades")
class GradeController(private val service: GradingService) {
    @GetMapping("/me") @PreAuthorize("hasAuthority('${Permissions.GRADES_READ_SELF}')") fun mine() = service.myGrades()
    @GetMapping("/queue") @PreAuthorize("hasAuthority('${Permissions.GRADING_MANAGE}')") fun queue() = service.queue()
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('${Permissions.GRADING_MANAGE}')") fun manual(@PathVariable id: UUID, @Valid @RequestBody input: ManualGradeRequest) = service.completeManual(id, input)
    @GetMapping("/{id}/history") @PreAuthorize("hasAnyAuthority('${Permissions.GRADES_READ_SELF}','${Permissions.GRADING_MANAGE}')") fun history(@PathVariable id: UUID) = service.history(id)
    @PostMapping("/{id}/appeals") @PreAuthorize("hasAuthority('${Permissions.GRADE_APPEALS_CREATE}')") fun appeal(@PathVariable id: UUID, @Valid @RequestBody input: CreateGradeAppealRequest) = service.createAppeal(id, input)
    @GetMapping("/appeals/me") @PreAuthorize("hasAuthority('${Permissions.GRADE_APPEALS_CREATE}')") fun myAppeals() = service.myAppeals()
    @GetMapping("/appeals") @PreAuthorize("hasAuthority('${Permissions.GRADE_APPEALS_MANAGE}')") fun appealQueue() = service.appealQueue()
    @PutMapping("/appeals/{id}") @PreAuthorize("hasAuthority('${Permissions.GRADE_APPEALS_MANAGE}')") fun resolveAppeal(@PathVariable id: UUID, @Valid @RequestBody input: ResolveGradeAppealRequest) = service.resolveAppeal(id, input)
}
