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
}
interface LessonProgressRepository : org.springframework.data.jpa.repository.JpaRepository<LessonProgressEntity, UUID> {
    fun findByEnrollmentIdAndLessonId(enrollmentId: UUID, lessonId: UUID): LessonProgressEntity?
    fun countByEnrollmentIdAndCompletedTrue(enrollmentId: UUID): Long
    fun findAllByEnrollmentIdOrderByUpdatedAtAsc(enrollmentId: UUID): List<LessonProgressEntity>
}
interface IdempotencyRecordRepository : org.springframework.data.jpa.repository.JpaRepository<IdempotencyRecordEntity, String>
