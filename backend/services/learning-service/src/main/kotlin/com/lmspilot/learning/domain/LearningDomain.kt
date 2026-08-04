package com.lmspilot.learning.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class LearningStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, OVERDUE }

@Entity
@Table(name = "course_progress", uniqueConstraints = [UniqueConstraint(name = "uq_progress_enrollment", columnNames = ["enrollment_id"])])
class CourseProgressEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "enrollment_id", nullable = false) var enrollmentId: UUID = UUID.randomUUID(),
    @Column(name = "course_id", nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(name = "course_version", nullable = false) var courseVersion: Int = 1,
    @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var progressPercent: Int = 0,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: LearningStatus = LearningStatus.NOT_STARTED,
    var lastLessonId: UUID? = null,
    @Column(length = 500) var lastPosition: String? = null,
    @Column(nullable = false) var totalLearningSeconds: Long = 0,
    var startedAt: Instant? = null,
    var lastAccessedAt: Instant? = null,
    var completedAt: Instant? = null,
    @Column(nullable = false) var completionEventPublished: Boolean = false,
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

@Entity
@Table(name = "lesson_progress", uniqueConstraints = [UniqueConstraint(name = "uq_lesson_progress", columnNames = ["enrollment_id", "lesson_id"])])
class LessonProgressEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "enrollment_id", nullable = false) var enrollmentId: UUID = UUID.randomUUID(),
    @Column(name = "course_id", nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(name = "lesson_id", nullable = false) var lessonId: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var completed: Boolean = false,
    @Column(nullable = false) var learningSeconds: Long = 0,
    @Column(length = 500) var position: String? = null,
    var openedAt: Instant? = null,
    var completedAt: Instant? = null,
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

@Entity
@Table(name = "idempotency_records")
class IdempotencyRecordEntity(
    @Id @Column(length = 160) var idempotencyKey: String = "",
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
)

interface CourseProgressRepository : org.springframework.data.jpa.repository.JpaRepository<CourseProgressEntity, UUID> {
    fun findByEnrollmentId(enrollmentId: UUID): CourseProgressEntity?
    fun findAllByUserIdOrderByLastAccessedAtDesc(userId: UUID): List<CourseProgressEntity>

    @org.springframework.data.jpa.repository.Query(
        value = """
            select 1
            from (select pg_advisory_xact_lock(hashtextextended(cast(:lockKey as text), 0))) as progress_lock
        """,
        nativeQuery = true,
    )
    fun lockEnrollment(@org.springframework.data.repository.query.Param("lockKey") lockKey: String): Int
}
interface LessonProgressRepository : org.springframework.data.jpa.repository.JpaRepository<LessonProgressEntity, UUID> {
    fun findByEnrollmentIdAndLessonId(enrollmentId: UUID, lessonId: UUID): LessonProgressEntity?
    fun countByEnrollmentIdAndCompletedTrue(enrollmentId: UUID): Long
    fun findAllByEnrollmentIdOrderByUpdatedAtAsc(enrollmentId: UUID): List<LessonProgressEntity>
}
interface IdempotencyRecordRepository : org.springframework.data.jpa.repository.JpaRepository<IdempotencyRecordEntity, String>


enum class AssignmentSubmissionStatus { SUBMITTED, GRADED, RETURNED }

@Entity
@Table(
    name = "assignment_submissions",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_assignment_submission_attempt", columnNames = ["enrollment_id", "lesson_id", "attempt_number"]),
        UniqueConstraint(name = "uq_assignment_submission_idempotency", columnNames = ["idempotency_key"]),
    ],
)
class AssignmentSubmissionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "enrollment_id", nullable = false) var enrollmentId: UUID = UUID.randomUUID(),
    @Column(name = "class_id", nullable = false) var classId: UUID = UUID.randomUUID(),
    @Column(name = "course_id", nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(name = "course_version", nullable = false) var courseVersion: Int = 1,
    @Column(name = "lesson_id", nullable = false) var lessonId: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(name = "attempt_number", nullable = false) var attemptNumber: Int = 1,
    @Column(name = "file_id", nullable = false) var fileId: UUID = UUID.randomUUID(),
    @Column(columnDefinition = "text") var comment: String? = null,
    @Column(name = "submitted_at", nullable = false) var submittedAt: Instant = Instant.now(),
    @Column(nullable = false) var late: Boolean = false,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: AssignmentSubmissionStatus = AssignmentSubmissionStatus.SUBMITTED,
    var score: Double? = null,
    var maxScore: Double? = null,
    @Column(columnDefinition = "text") var feedback: String? = null,
    var gradedBy: UUID? = null,
    var gradedAt: Instant? = null,
    @Column(name = "idempotency_key", nullable = false, length = 160) var idempotencyKey: String = "",
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

interface AssignmentSubmissionRepository : org.springframework.data.jpa.repository.JpaRepository<AssignmentSubmissionEntity, UUID> {
    fun findByIdempotencyKey(idempotencyKey: String): AssignmentSubmissionEntity?
    fun findAllByEnrollmentIdAndLessonIdOrderByAttemptNumberDesc(enrollmentId: UUID, lessonId: UUID): List<AssignmentSubmissionEntity>
    fun findAllByUserIdOrderBySubmittedAtDesc(userId: UUID): List<AssignmentSubmissionEntity>
    fun findAllByClassIdOrderBySubmittedAtDesc(classId: UUID): List<AssignmentSubmissionEntity>
    fun findAllByClassIdInOrderBySubmittedAtDesc(classIds: Collection<UUID>): List<AssignmentSubmissionEntity>
    fun countByEnrollmentIdAndLessonId(enrollmentId: UUID, lessonId: UUID): Long

    @org.springframework.data.jpa.repository.Query(
        value = """
            SELECT 1
            FROM (SELECT pg_advisory_xact_lock(hashtextextended(CAST(:lockKey AS text), 0))) AS assignment_lock
        """,
        nativeQuery = true,
    )
    fun lockAttemptSequence(@org.springframework.data.repository.query.Param("lockKey") lockKey: String): Int
}

enum class XapiObjectType { COURSE, LESSON, EXAM, ASSIGNMENT, LIVE_SESSION, OTHER }

@Entity
@Table(
    name = "xapi_statements",
    indexes = [
        Index(name = "idx_xapi_actor_time", columnList = "actor_user_id,occurred_at"),
        Index(name = "idx_xapi_object_time", columnList = "object_id,occurred_at"),
        Index(name = "idx_xapi_course_time", columnList = "course_id,occurred_at"),
    ],
)
class XapiStatementEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "actor_user_id", nullable = false) var actorUserId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 180) var verb: String = "experienced",
    @Column(name = "object_id", nullable = false, length = 500) var objectId: String = "",
    @Enumerated(EnumType.STRING) @Column(name = "object_type", nullable = false, length = 30) var objectType: XapiObjectType = XapiObjectType.OTHER,
    @Column(name = "course_id") var courseId: UUID? = null,
    @Column(name = "lesson_id") var lessonId: UUID? = null,
    @Column(name = "enrollment_id") var enrollmentId: UUID? = null,
    @Column(name = "result_score") var resultScore: Double? = null,
    @Column(name = "result_success") var resultSuccess: Boolean? = null,
    @Column(name = "result_completion") var resultCompletion: Boolean? = null,
    @Column(name = "duration_seconds") var durationSeconds: Long? = null,
    @Column(name = "context_json", nullable = false, columnDefinition = "text") var contextJson: String = "{}",
    @Column(name = "occurred_at", nullable = false) var occurredAt: Instant = Instant.now(),
    @Column(name = "stored_at", nullable = false) var storedAt: Instant = Instant.now(),
    @Column(name = "source", nullable = false, length = 80) var source: String = "WEB",
)

interface XapiStatementRepository : org.springframework.data.jpa.repository.JpaRepository<XapiStatementEntity, UUID> {
    fun findTop200ByActorUserIdOrderByOccurredAtDesc(actorUserId: UUID): List<XapiStatementEntity>
    fun findTop200ByCourseIdOrderByOccurredAtDesc(courseId: UUID): List<XapiStatementEntity>
}
