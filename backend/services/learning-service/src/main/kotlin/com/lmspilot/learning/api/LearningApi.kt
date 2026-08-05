package com.lmspilot.learning.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.CourseCompletedPayload
import com.lmspilot.contracts.DomainEventEnvelope
import com.lmspilot.contracts.EnrolledPayload
import com.lmspilot.contracts.EventTypes
import com.lmspilot.contracts.ExamGradedPayload
import com.lmspilot.contracts.LessonCompletedPayload
import com.lmspilot.contracts.Permissions
import com.lmspilot.learning.domain.CourseProgressEntity
import com.lmspilot.learning.domain.CourseProgressRepository
import com.lmspilot.learning.domain.IdempotencyRecordEntity
import com.lmspilot.learning.domain.IdempotencyRecordRepository
import com.lmspilot.learning.domain.LearningStatus
import com.lmspilot.learning.domain.LessonProgressEntity
import com.lmspilot.learning.domain.LessonProgressRepository
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.InternalTokenAuthorizer
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

data class ProgressUpdateRequest(
    val enrollmentId: UUID,
    val courseId: UUID,
    val lessonId: UUID,
    val completed: Boolean,
    @field:Min(0) @field:Max(86400) val learningSecondsDelta: Long = 0,
    val position: String? = null,
)

data class LessonProgressResponse(
    val lessonId: UUID,
    val completed: Boolean,
    val learningSeconds: Long,
    val position: String?,
    val completedAt: Instant?,
)

data class InternalCourseProgressSummary(val enrollmentId: UUID, val courseId: UUID, val courseVersion: Int, val progressPercent: Int, val status: LearningStatus, val completedAt: Instant?)

data class CourseProgressResponse(
    val enrollmentId: UUID,
    val courseId: UUID,
    val courseVersion: Int,
    val userId: UUID,
    val progressPercent: Int,
    val status: LearningStatus,
    val lastLessonId: UUID?,
    val lastPosition: String?,
    val totalLearningSeconds: Long,
    val lastAccessedAt: Instant?,
    val completedAt: Instant?,
    val lessons: List<LessonProgressResponse> = emptyList(),
)

data class EnrollmentValidation(
    val enrollmentId: UUID,
    val classId: UUID,
    val courseId: UUID,
    val courseVersion: Int,
    val userId: UUID,
    val status: String,
    val dueAt: Instant?,
)

data class CourseLearningMetadata(
    val courseId: UUID,
    val version: Int,
    val status: String,
    val ownerId: UUID,
    val lessonIds: Set<UUID>,
    val requiredLessonIds: Set<UUID>,
    val lessonTypes: Map<UUID, String> = emptyMap(),
    val lessonFileIds: Set<UUID> = emptySet(),
)

@Configuration
class LearningMessagingConfiguration {
    @Bean
    fun learningEnrollmentQueue() = Queue("learning.enrolled", true)

    @Bean
    fun learningEnrollmentBinding(@Qualifier("learningEnrollmentQueue") queue: Queue, domainEventExchange: TopicExchange): Binding =
        BindingBuilder.bind(queue).to(domainEventExchange).with(EventTypes.ENROLLED)

    @Bean
    fun learningExamGradedQueue() = Queue("learning.exam-graded", true)

    @Bean
    fun learningExamGradedBinding(@Qualifier("learningExamGradedQueue") queue: Queue, domainEventExchange: TopicExchange): Binding =
        BindingBuilder.bind(queue).to(domainEventExchange).with(EventTypes.EXAM_GRADED)
}

@Service
class EnrollmentValidationClient(
    builder: RestClient.Builder,
    @Value("\${enrollment-service.url:http://localhost:8084}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client: RestClient = builder.baseUrl(baseUrl).build()

    fun get(enrollmentId: UUID): EnrollmentValidation = client.get()
        .uri("/internal/v1/enrollments/{id}", enrollmentId)
        .header("X-Service-Token", serviceToken)
        .retrieve()
        .body(EnrollmentValidation::class.java)
        ?: throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ENROLLMENT_SERVICE_UNAVAILABLE", "Không nhận được dữ liệu ghi danh")

}

@Service
class CourseLearningClient(
    builder: RestClient.Builder,
    @Value("\${course-service.url:http://localhost:8083}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client: RestClient = builder.baseUrl(baseUrl).build()

    fun get(courseId: UUID, version: Int): CourseLearningMetadata = client.get()
        .uri { builder -> builder.path("/internal/v1/courses/{id}/learning-metadata").queryParam("version", version).build(courseId) }
        .header("X-Service-Token", serviceToken)
        .retrieve()
        .body(CourseLearningMetadata::class.java)
        ?: throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "COURSE_SERVICE_UNAVAILABLE", "Không nhận được cấu trúc khóa học")
}

@Service
class FileAccessClient(
    builder: RestClient.Builder,
    @Value("\${file-storage-service.url:http://localhost:8089}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client: RestClient = builder.baseUrl(baseUrl).build()

    fun grant(userId: UUID, fileIds: Set<UUID>, source: String) {
        if (fileIds.isEmpty()) return
        client.post()
            .uri("/internal/v1/files/access-grants")
            .header("X-Service-Token", serviceToken)
            .body(mapOf("userId" to userId, "fileIds" to fileIds, "source" to source, "ttlSeconds" to 14_400))
            .retrieve()
            .toBodilessEntity()
    }
}

@Service
class LearningProgressService(
    private val courses: CourseProgressRepository,
    private val lessons: LessonProgressRepository,
    private val idempotency: IdempotencyRecordRepository,
    private val enrollmentClient: EnrollmentValidationClient,
    private val courseClient: CourseLearningClient,
    private val fileAccess: FileAccessClient,
    private val events: DomainEventPublisher,
    private val mapper: ObjectMapper,
) {
    @RabbitListener(queues = ["learning.enrolled"])
    @Transactional
    fun onEnrolled(event: DomainEventEnvelope) {
        val payload = mapper.treeToValue(event.payload, EnrolledPayload::class.java)
        if (courses.findByEnrollmentId(payload.enrollmentId) == null) {
            val enrollment = enrollmentClient.get(payload.enrollmentId)
            courses.save(
                CourseProgressEntity(
                    enrollmentId = payload.enrollmentId,
                    courseId = payload.courseId,
                    courseVersion = enrollment.courseVersion,
                    userId = payload.userId,
                    status = LearningStatus.NOT_STARTED,
                )
            )
        }
    }

    @RabbitListener(queues = ["learning.exam-graded"])
    @Transactional
    fun onExamGraded(event: DomainEventEnvelope) {
        val payload = mapper.treeToValue(event.payload, ExamGradedPayload::class.java)
        val enrollmentId = payload.enrollmentId ?: return
        val courseId = payload.courseId ?: return
        val lessonId = payload.lessonId ?: return
        recordVerifiedOutcome(
            enrollmentId = enrollmentId,
            courseId = courseId,
            lessonId = lessonId,
            userId = payload.userId,
            completed = payload.status == "COMPLETED" && (payload.effectivePassed ?: payload.passed),
            expectedLessonType = "EXAM",
            position = "exam-session:${payload.sessionId}",
        )
    }

    @Transactional(readOnly = true)
    fun mine(): List<CourseProgressResponse> =
        courses.findAllByUserIdOrderByLastAccessedAtDesc(CurrentUser.id()).map { it.response() }

    @Transactional(readOnly = true)
    fun summaries(userId: UUID): List<InternalCourseProgressSummary> =
        courses.findAllByUserIdOrderByLastAccessedAtDesc(userId).map {
            InternalCourseProgressSummary(it.enrollmentId, it.courseId, it.courseVersion, it.progressPercent, it.status, it.completedAt)
        }

    @Transactional(readOnly = true)
    fun detail(enrollmentId: UUID): CourseProgressResponse {
        val progress = courses.findByEnrollmentId(enrollmentId)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "PROGRESS_NOT_FOUND", "Chưa có dữ liệu tiến độ")
        requireOwnerOrScope(progress.userId, progress.enrollmentId)
        val metadata = courseClient.get(progress.courseId, progress.courseVersion)
        fileAccess.grant(CurrentUser.id(), metadata.lessonFileIds, "COURSE:${progress.courseId}:V${progress.courseVersion}")
        return progress.response(lessons.findAllByEnrollmentIdOrderByUpdatedAtAsc(enrollmentId).map { it.response() })
    }

    @Transactional
    fun update(input: ProgressUpdateRequest, key: String): CourseProgressResponse {
        if (key.isBlank() || key.length > 160) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key không hợp lệ")
        }
        val userId = CurrentUser.id()
        val scopedKey = idempotencyKey(userId, input.enrollmentId, input.lessonId, key)
        if (idempotency.existsById(scopedKey)) return detail(input.enrollmentId)

        val enrollment = enrollmentClient.get(input.enrollmentId)
        if (enrollment.userId != userId) {
            throw ApiException(HttpStatus.FORBIDDEN, "ENROLLMENT_OWNER_MISMATCH", "Ghi danh không thuộc người dùng hiện tại")
        }
        if (enrollment.courseId != input.courseId) {
            throw ApiException(HttpStatus.BAD_REQUEST, "COURSE_ENROLLMENT_MISMATCH", "Khóa học không khớp với ghi danh")
        }
        if (enrollment.status == "CANCELLED") {
            throw ApiException(HttpStatus.CONFLICT, "ENROLLMENT_INACTIVE", "Ghi danh đã bị hủy")
        }

        val metadata = courseClient.get(input.courseId, enrollment.courseVersion)
        if (metadata.status !in setOf("PUBLISHED", "HIDDEN")) {
            throw ApiException(HttpStatus.CONFLICT, "COURSE_NOT_LEARNABLE", "Khóa học chưa sẵn sàng để học")
        }
        if (input.lessonId !in metadata.lessonIds) {
            throw ApiException(HttpStatus.BAD_REQUEST, "LESSON_NOT_IN_COURSE", "Bài học không thuộc khóa học")
        }
        val lessonType = metadata.lessonTypes[input.lessonId]
        val verifiedOutcome = lessonType in setOf("ASSIGNMENT", "EXAM")
        if (verifiedOutcome && input.completed) {
            throw ApiException(HttpStatus.CONFLICT, "VERIFIED_OUTCOME_REQUIRED", "Bài thực hành và bài thi chỉ được hoàn thành từ kết quả do máy chủ xác minh")
        }

        courses.lockEnrollment(input.enrollmentId.toString())
        val now = Instant.now()
        val course = courses.findByEnrollmentId(input.enrollmentId)
            ?: courses.save(
                CourseProgressEntity(
                    enrollmentId = input.enrollmentId,
                    courseId = input.courseId,
                    courseVersion = enrollment.courseVersion,
                    userId = userId,
                    status = LearningStatus.IN_PROGRESS,
                    startedAt = now,
                )
            )
        course.courseVersion = enrollment.courseVersion
        if (course.userId != userId) {
            throw ApiException(HttpStatus.FORBIDDEN, "PROGRESS_OWNER_MISMATCH", "Không thể cập nhật tiến độ của người khác")
        }

        val lesson = lessons.findByEnrollmentIdAndLessonId(input.enrollmentId, input.lessonId)
            ?: LessonProgressEntity(
                enrollmentId = input.enrollmentId,
                courseId = input.courseId,
                lessonId = input.lessonId,
                userId = userId,
                openedAt = now,
            )
        val wasCompleted = lesson.completed
        val acceptedCompleted = if (verifiedOutcome) lesson.completed else input.completed
        lesson.completed = acceptedCompleted
        lesson.learningSeconds += input.learningSecondsDelta
        lesson.position = input.position
        lesson.updatedAt = now
        if (acceptedCompleted && lesson.completedAt == null) lesson.completedAt = now
        if (!acceptedCompleted) lesson.completedAt = null
        lessons.save(lesson)

        val requiredLessons = metadata.requiredLessonIds
        val completedRequired = lessons.findAllByEnrollmentIdOrderByUpdatedAtAsc(input.enrollmentId)
            .count { it.completed && it.lessonId in requiredLessons }
        course.progressPercent = if (requiredLessons.isEmpty()) 100 else
            ((completedRequired * 100) / requiredLessons.size).coerceIn(0, 100)
        course.status = if (course.progressPercent >= 100) LearningStatus.COMPLETED else LearningStatus.IN_PROGRESS
        course.lastLessonId = input.lessonId
        course.lastPosition = input.position
        course.totalLearningSeconds += input.learningSecondsDelta
        course.lastAccessedAt = now
        course.updatedAt = now
        if (course.status == LearningStatus.COMPLETED && course.completedAt == null) course.completedAt = now
        if (course.status != LearningStatus.COMPLETED) course.completedAt = null
        idempotency.save(IdempotencyRecordEntity(scopedKey))

        if (!wasCompleted && acceptedCompleted) {
            events.publish(
                EventTypes.LESSON_COMPLETED,
                "learning-service",
                lesson.id.toString(),
                LessonCompletedPayload(input.enrollmentId, input.courseId, input.lessonId, userId, course.progressPercent),
            )
        }
        if (course.status == LearningStatus.COMPLETED && !course.completionEventPublished) {
            course.completionEventPublished = true
            events.publish(
                EventTypes.COURSE_COMPLETED,
                "learning-service",
                course.id.toString(),
                CourseCompletedPayload(input.enrollmentId, input.courseId, userId, now),
            )
        }
        return detail(input.enrollmentId)
    }

    @Transactional
    fun recordVerifiedOutcome(
        enrollmentId: UUID,
        courseId: UUID,
        lessonId: UUID,
        userId: UUID,
        completed: Boolean,
        expectedLessonType: String,
        position: String,
    ): CourseProgressResponse {
        val enrollment = enrollmentClient.get(enrollmentId)
        if (enrollment.userId != userId || enrollment.courseId != courseId) {
            throw ApiException(HttpStatus.BAD_REQUEST, "VERIFIED_OUTCOME_CONTEXT_MISMATCH", "Kết quả xác minh không khớp với ghi danh")
        }
        if (enrollment.status == "CANCELLED") {
            throw ApiException(HttpStatus.CONFLICT, "ENROLLMENT_INACTIVE", "Ghi danh đã bị hủy")
        }
        val metadata = courseClient.get(courseId, enrollment.courseVersion)
        if (metadata.lessonTypes[lessonId] != expectedLessonType) {
            throw ApiException(HttpStatus.BAD_REQUEST, "VERIFIED_OUTCOME_LESSON_MISMATCH", "Kết quả xác minh không khớp loại bài học")
        }

        courses.lockEnrollment(enrollmentId.toString())
        val now = Instant.now()
        val course = courses.findByEnrollmentId(enrollmentId)
            ?: courses.save(
                CourseProgressEntity(
                    enrollmentId = enrollmentId,
                    courseId = courseId,
                    courseVersion = enrollment.courseVersion,
                    userId = userId,
                    status = LearningStatus.IN_PROGRESS,
                    startedAt = now,
                )
            )
        if (course.userId != userId || course.courseId != courseId) {
            throw ApiException(HttpStatus.BAD_REQUEST, "PROGRESS_CONTEXT_MISMATCH", "Tiến độ không khớp với kết quả xác minh")
        }
        course.courseVersion = enrollment.courseVersion

        val lesson = lessons.findByEnrollmentIdAndLessonId(enrollmentId, lessonId)
            ?: LessonProgressEntity(
                enrollmentId = enrollmentId,
                courseId = courseId,
                lessonId = lessonId,
                userId = userId,
                openedAt = now,
            )
        val wasCompleted = lesson.completed
        lesson.completed = completed
        lesson.position = position.take(500)
        lesson.updatedAt = now
        lesson.completedAt = if (completed) lesson.completedAt ?: now else null
        lessons.save(lesson)

        val requiredLessons = metadata.requiredLessonIds
        val completedRequired = lessons.findAllByEnrollmentIdOrderByUpdatedAtAsc(enrollmentId)
            .count { it.completed && it.lessonId in requiredLessons }
        course.progressPercent = if (requiredLessons.isEmpty()) 100 else
            ((completedRequired * 100) / requiredLessons.size).coerceIn(0, 100)
        course.status = if (course.progressPercent >= 100) LearningStatus.COMPLETED else LearningStatus.IN_PROGRESS
        course.lastLessonId = lessonId
        course.lastPosition = lesson.position
        course.lastAccessedAt = now
        course.updatedAt = now
        course.completedAt = if (course.status == LearningStatus.COMPLETED) course.completedAt ?: now else null

        if (!wasCompleted && completed) {
            events.publish(
                EventTypes.LESSON_COMPLETED,
                "learning-service",
                lesson.id.toString(),
                LessonCompletedPayload(enrollmentId, courseId, lessonId, userId, course.progressPercent),
            )
        }
        if (course.status == LearningStatus.COMPLETED && !course.completionEventPublished) {
            course.completionEventPublished = true
            events.publish(
                EventTypes.COURSE_COMPLETED,
                "learning-service",
                course.id.toString(),
                CourseCompletedPayload(enrollmentId, courseId, userId, now),
            )
        }
        return course.response(lessons.findAllByEnrollmentIdOrderByUpdatedAtAsc(enrollmentId).map { it.response() })
    }

    private fun idempotencyKey(userId: UUID, enrollmentId: UUID, lessonId: UUID, key: String): String {
        val value = "$userId|$enrollmentId|$lessonId|$key"
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun requireOwnerOrScope(owner: UUID, enrollmentId: UUID) {
        val currentUserId = CurrentUser.id()
        if (owner == currentUserId) return

        val auth = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication
        if (auth.authorities.none { it.authority == Permissions.LEARNING_READ_SCOPE }) {
            throw ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Không có quyền xem tiến độ này")
        }
        val enrollment = enrollmentClient.get(enrollmentId)
        val metadata = courseClient.get(enrollment.courseId, enrollment.courseVersion)
        if (metadata.ownerId != currentUserId) {
            throw ApiException(HttpStatus.FORBIDDEN, "LEARNING_OUT_OF_SCOPE", "Tiến độ ngoài khóa học được phân công")
        }
    }
}

private fun CourseProgressEntity.response(lessons: List<LessonProgressResponse> = emptyList()) =
    CourseProgressResponse(
        enrollmentId,
        courseId,
        courseVersion,
        userId,
        progressPercent,
        status,
        lastLessonId,
        lastPosition,
        totalLearningSeconds,
        lastAccessedAt,
        completedAt,
        lessons,
    )

private fun LessonProgressEntity.response() =
    LessonProgressResponse(lessonId, completed, learningSeconds, position, completedAt)

@RestController
@RequestMapping("/api/v1/learning")
class LearningController(private val service: LearningProgressService) {
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_READ_SELF}')")
    fun mine() = service.mine()

    @GetMapping("/{enrollmentId}")
    @PreAuthorize("hasAnyAuthority('${Permissions.LEARNING_READ_SELF}','${Permissions.LEARNING_READ_SCOPE}')")
    fun detail(@PathVariable enrollmentId: UUID) = service.detail(enrollmentId)

    @PutMapping("/progress")
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_WRITE_SELF}')")
    fun update(
        @Valid @RequestBody input: ProgressUpdateRequest,
        @RequestHeader("Idempotency-Key") key: String,
    ) = service.update(input, key)
}


@RestController
@RequestMapping("/internal/v1/learning")
class InternalLearningController(
    private val service: LearningProgressService,
    private val internal: InternalTokenAuthorizer,
) {
    @GetMapping("/users/{userId}/courses")
    fun summaries(
        @PathVariable userId: UUID,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ): List<InternalCourseProgressSummary> {
        internal.require(token)
        return service.summaries(userId)
    }
}
