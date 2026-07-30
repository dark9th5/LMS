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

data class GradingQuestionPayload(val questionId: UUID, val type: String, val correctAnswers: List<String>, val points: Double)
data class GradingPayload(val sessionId: UUID, val examId: UUID, val userId: UUID, val passingScore: Double, val answers: Map<String, JsonNode>, val questions: List<GradingQuestionPayload>, val courseId: UUID? = null)
data class GradeDetail(val questionId: UUID, val type: String, val awarded: Double, val maximum: Double, val requiresManual: Boolean)
data class GradeResponse(val id: UUID, val sessionId: UUID, val examId: UUID, val courseId: UUID?, val userId: UUID, val score: Double, val maxScore: Double, val percentage: Double, val passed: Boolean, val status: GradeStatus, val details: List<GradeDetail>, val feedback: String?, val updatedAt: Instant)
data class ManualGradeRequest(@field:DecimalMin("0.0") val score: Double, val feedback: String? = null)

@Configuration
class GradingMessagingConfiguration {
    @Bean fun gradingQueue() = Queue("grading.exam-submitted", true)
    @Bean fun gradingBinding(queue: Queue, domainEventExchange: TopicExchange) = BindingBuilder.bind(queue).to(domainEventExchange).with(EventTypes.EXAM_SUBMITTED)
}

@Service
class GradingService(
    private val repository: GradeResultRepository,
    private val mapper: ObjectMapper,
    private val publisher: DomainEventPublisher,
    @Value("\${services.assessment-url:http://localhost:8086}") assessmentUrl: String,
    @Value("\${lmspilot.internal-token}") private val internalToken: String,
) {
    private val assessment = RestClient.builder().baseUrl(assessmentUrl).build()

    @RabbitListener(queues = ["grading.exam-submitted"])
    fun onExamSubmitted(event: DomainEventEnvelope) {
        val payload = mapper.treeToValue(event.payload, ExamSubmittedPayload::class.java)
        gradeAutomatically(payload.sessionId)
    }

    @Transactional
    fun gradeAutomatically(sessionId: UUID): GradeResponse {
        repository.findBySessionId(sessionId)?.let { return it.response(mapper) }
        val payload = assessment.get().uri("/internal/v1/assessment/sessions/{id}/grading-payload", sessionId)
            .header("X-Service-Token", internalToken).retrieve().body(GradingPayload::class.java)
            ?: throw ApiException(HttpStatus.BAD_GATEWAY, "ASSESSMENT_UNAVAILABLE", "Không lấy được dữ liệu bài thi")
        var earned = 0.0
        var max = 0.0
        var manual = false
        val details = payload.questions.map { q ->
            max += q.points
            val answer = payload.answers[q.questionId.toString()]
            val isManual = q.type == "ESSAY" || q.type == "SHORT_TEXT"
            val awarded = if (isManual) 0.0 else if (matches(answer, q.correctAnswers, q.type)) q.points else 0.0
            earned += awarded
            manual = manual || isManual
            GradeDetail(q.questionId, q.type, awarded, q.points, isManual)
        }
        val percentage = if (max == 0.0) 0.0 else round(earned * 100.0 / max)
        val entity = repository.save(GradeResultEntity(sessionId = payload.sessionId, examId = payload.examId, courseId = payload.courseId, userId = payload.userId, score = round(earned), maxScore = round(max), percentage = percentage, passingScore = payload.passingScore, passed = !manual && percentage >= payload.passingScore, status = if (manual) GradeStatus.PENDING_MANUAL else GradeStatus.COMPLETED, detailsJson = mapper.writeValueAsString(details)))
        if (!manual) publish(entity)
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
        entity.score = round(input.score)
        entity.percentage = if (entity.maxScore == 0.0) 0.0 else round(entity.score * 100 / entity.maxScore)
        entity.passed = entity.percentage >= entity.passingScore
        entity.status = GradeStatus.COMPLETED
        entity.feedback = input.feedback
        entity.gradedBy = CurrentUser.id()
        entity.updatedAt = Instant.now()
        publish(entity)
        return entity.response(mapper)
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

    private fun isAdmin() = CurrentUser.roles().contains("ADMIN")

    private fun matches(answer: JsonNode?, expected: List<String>, type: String): Boolean {
        if (answer == null || answer.isNull) return false
        val actual = when {
            answer.isArray -> answer.map { it.asText().trim() }.toSet()
            else -> setOf(answer.asText().trim())
        }
        val target = expected.map { it.trim() }.toSet()
        return if (type == "MULTIPLE_CHOICE") actual == target else actual.firstOrNull().equals(target.firstOrNull(), ignoreCase = true)
    }

    private fun publish(entity: GradeResultEntity) = publisher.publish(
        EventTypes.EXAM_GRADED,
        "grading-service",
        entity.id.toString(),
        ExamGradedPayload(entity.sessionId, entity.examId, entity.userId, entity.score, entity.maxScore, entity.passed, entity.status.name, courseId = entity.courseId),
    )
    private fun round(value: Double) = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
}

private fun GradeResultEntity.response(mapper: ObjectMapper) = GradeResponse(id, sessionId, examId, courseId, userId, score, maxScore, percentage, passed, status, mapper.readValue(detailsJson, mapper.typeFactory.constructCollectionType(List::class.java, GradeDetail::class.java)), feedback, updatedAt)

@RestController
@RequestMapping("/api/v1/grades")
class GradeController(private val service: GradingService) {
    @GetMapping("/me") @PreAuthorize("hasAuthority('${Permissions.GRADES_READ_SELF}')") fun mine() = service.myGrades()
    @GetMapping("/queue") @PreAuthorize("hasAuthority('${Permissions.GRADING_MANAGE}')") fun queue() = service.queue()
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('${Permissions.GRADING_MANAGE}')") fun manual(@PathVariable id: UUID, @Valid @RequestBody input: ManualGradeRequest) = service.completeManual(id, input)
}
