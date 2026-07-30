package com.lmspilot.reporting.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.*
import com.lmspilot.reporting.domain.*
import com.lmspilot.support.security.CurrentUser
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestClientBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@Configuration
class ReportingMessagingConfiguration {
    @Bean fun reportingQueue() = Queue("reporting.domain-events", true)
    @Bean fun reportingBinding(reportingQueue: Queue, domainEventExchange: TopicExchange) = BindingBuilder.bind(reportingQueue).to(domainEventExchange).with("#")
}

data class DashboardResponse(val enrolled: Long, val inProgress: Long, val completed: Long, val overdue: Long, val averageProgress: Double, val lastSynchronizedAt: Instant)
data class LearnerCourseRow(val enrollmentId: UUID, val classId: UUID, val courseId: UUID, val userId: UUID, val progressPercent: Int, val completed: Boolean, val dueAt: Instant?, val lastScore: Double?, val passed: Boolean?, val updatedAt: Instant)


@Service
class EnrollmentScopeClient(
    builder: RestClientBuilder,
    @Value("\${enrollment-service.url:http://localhost:8084}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client = builder.baseUrl(baseUrl).build()

    fun assignedClassIds(userId: UUID): Set<UUID> = runCatching {
        client.get()
            .uri("/internal/v1/classes/assigned/{userId}", userId)
            .header("X-Service-Token", serviceToken)
            .retrieve()
            .body(Array<String>::class.java)
            ?.map(UUID::fromString)
            ?.toSet()
            ?: emptySet()
    }.getOrDefault(emptySet())
}

@Service
class ReportingProjectionService(
    private val events: ReportEventRepository,
    private val readModels: LearnerCourseReadModelRepository,
    private val mapper: ObjectMapper,
    private val enrollmentScope: EnrollmentScopeClient,
) {
    @RabbitListener(queues = ["reporting.domain-events"])
    @Transactional
    fun project(event: DomainEventEnvelope) {
        if (events.existsByEventId(event.eventId)) return
        events.save(ReportEventEntity(eventId = event.eventId, eventType = event.eventType, aggregateId = event.aggregateId, occurredAt = event.occurredAt, payloadJson = mapper.writeValueAsString(event.payload)))
        when (event.eventType) {
            EventTypes.ENROLLED -> {
                val p = mapper.treeToValue(event.payload, EnrolledPayload::class.java)
                if (readModels.findByEnrollmentId(p.enrollmentId) == null) readModels.save(LearnerCourseReadModel(enrollmentId = p.enrollmentId, classId = p.classId, courseId = p.courseId, userId = p.userId, dueAt = p.dueAt, updatedAt = event.occurredAt))
            }
            EventTypes.LESSON_COMPLETED -> {
                val p = mapper.treeToValue(event.payload, LessonCompletedPayload::class.java)
                readModels.findByEnrollmentId(p.enrollmentId)?.apply { progressPercent = maxOf(progressPercent, p.progressPercent); lastActivityAt = event.occurredAt; updatedAt = event.occurredAt }
            }
            EventTypes.COURSE_COMPLETED -> {
                val p = mapper.treeToValue(event.payload, CourseCompletedPayload::class.java)
                readModels.findByEnrollmentId(p.enrollmentId)?.apply { completed = true; progressPercent = 100; completedAt = p.completedAt; lastActivityAt = p.completedAt; updatedAt = event.occurredAt }
            }
            EventTypes.EXAM_GRADED -> {
                val p = mapper.treeToValue(event.payload, ExamGradedPayload::class.java)
                val userRows = readModels.findAllByUserId(p.userId)
                val candidates = p.courseId?.let { courseId -> userRows.filter { it.courseId == courseId } } ?: userRows
                // Do not attach a score to an arbitrary enrollment when the same learner has
                // multiple matching classes. The BA does not define that association yet.
                candidates.singleOrNull()?.apply {
                    lastScore = if (p.maxScore == 0.0) 0.0 else p.score * 100.0 / p.maxScore
                    passed = p.passed
                    updatedAt = event.occurredAt
                }
            }
        }
    }

    @Transactional(readOnly = true)
    fun dashboard(): DashboardResponse {
        val rows = scopedRows()
        val enrolled = rows.size.toLong()
        val completed = rows.count { it.completed }.toLong()
        val overdue = rows.count { !it.completed && it.dueAt?.isBefore(Instant.now()) == true }.toLong()
        val inProgress = rows.count { !it.completed && it.progressPercent > 0 && it.dueAt?.isBefore(Instant.now()) != true }.toLong()
        val average = if (rows.isEmpty()) 0.0 else rows.map { it.progressPercent }.average()
        return DashboardResponse(enrolled, inProgress, completed, overdue, average, rows.maxOfOrNull { it.updatedAt } ?: Instant.now())
    }

    @Transactional(readOnly = true)
    fun rows(selfOnly: Boolean): List<LearnerCourseRow> {
        val source = if (selfOnly) readModels.findAllByUserId(CurrentUser.id()) else scopedRows()
        return source.map { LearnerCourseRow(it.enrollmentId, it.classId, it.courseId, it.userId, it.progressPercent, it.completed, it.dueAt, it.lastScore, it.passed, it.updatedAt) }
    }

    private fun scopedRows(): List<LearnerCourseReadModel> {
        if (CurrentUser.roles().contains("ADMIN")) return readModels.findAll()
        val assigned = enrollmentScope.assignedClassIds(CurrentUser.id())
        return readModels.findAll().filter { it.classId in assigned }
    }

    fun exportCsv(rows: List<LearnerCourseRow>): ByteArray {
        val header = listOf("enrollmentId", "classId", "courseId", "userId", "progressPercent", "completed", "dueAt", "lastScore", "passed", "updatedAt")
            .joinToString(",")
        val body = rows.joinToString("\r\n") { row ->
            listOf(
                row.enrollmentId, row.classId, row.courseId, row.userId, row.progressPercent,
                row.completed, row.dueAt ?: "", row.lastScore ?: "", row.passed ?: "", row.updatedAt,
            ).joinToString(",") { csvCell(it) }
        }
        // UTF-8 BOM keeps Vietnamese data readable in common spreadsheet applications.
        return ("\uFEFF" + header + "\r\n" + body + if (body.isNotEmpty()) "\r\n" else "").toByteArray(StandardCharsets.UTF_8)
    }

    private fun csvCell(value: Any): String {
        var text = value.toString()
        // Prevent spreadsheet formula execution when future report columns contain user input.
        if (text.firstOrNull() in setOf('=', '+', '-', '@', '\t', '\r')) text = "'" + text
        return "\"" + text.replace("\"", "\"\"") + "\""
    }
}

@RestController
@RequestMapping("/api/v1/reports")
class ReportingController(private val service: ReportingProjectionService) {
    @GetMapping("/dashboard") @PreAuthorize("hasAuthority('${Permissions.REPORTS_READ}')") fun dashboard() = service.dashboard()
    @GetMapping("/learning") @PreAuthorize("hasAuthority('${Permissions.REPORTS_READ}')") fun learning() = service.rows(false)
    @GetMapping("/me") @PreAuthorize("isAuthenticated()") fun me() = service.rows(true)
    @GetMapping("/learning/export.csv") @PreAuthorize("hasAuthority('${Permissions.REPORTS_EXPORT}')")
    fun export(): ResponseEntity<ByteArray> = ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=learning-report.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(service.exportCsv(service.rows(false)))
}
