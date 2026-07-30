package com.lmspilot.reporting.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "report_events", uniqueConstraints = [UniqueConstraint(name = "uq_report_event", columnNames = ["event_id"])])
class ReportEventEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "event_id", nullable = false) var eventId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 120) var eventType: String = "",
    @Column(nullable = false, length = 120) var aggregateId: String = "",
    @Column(nullable = false) var occurredAt: Instant = Instant.now(),
    @Column(nullable = false, columnDefinition = "text") var payloadJson: String = "{}",
)

@Entity
@Table(name = "learner_course_read_model", uniqueConstraints = [UniqueConstraint(name = "uq_report_enrollment", columnNames = ["enrollment_id"])])
class LearnerCourseReadModel(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "enrollment_id", nullable = false) var enrollmentId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var classId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var userId: UUID = UUID.randomUUID(),
    var dueAt: Instant? = null,
    @Column(nullable = false) var progressPercent: Int = 0,
    @Column(nullable = false) var completed: Boolean = false,
    var completedAt: Instant? = null,
    var lastActivityAt: Instant? = null,
    var lastScore: Double? = null,
    var passed: Boolean? = null,
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
)

interface ReportEventRepository : org.springframework.data.jpa.repository.JpaRepository<ReportEventEntity, UUID> {
    fun existsByEventId(eventId: UUID): Boolean
}
interface LearnerCourseReadModelRepository : org.springframework.data.jpa.repository.JpaRepository<LearnerCourseReadModel, UUID> {
    fun findByEnrollmentId(enrollmentId: UUID): LearnerCourseReadModel?
    fun findAllByUserId(userId: UUID): List<LearnerCourseReadModel>
    fun countByCompleted(completed: Boolean): Long
    fun countByDueAtBeforeAndCompletedFalse(now: Instant): Long
}
