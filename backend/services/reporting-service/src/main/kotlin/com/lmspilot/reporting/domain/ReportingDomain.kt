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
    fun findAllByClassIdIn(classIds: Collection<UUID>): List<LearnerCourseReadModel>
    fun countByCompleted(completed: Boolean): Long
    fun countByDueAtBeforeAndCompletedFalse(now: Instant): Long
    fun findAllByCompletedFalseAndDueAtGreaterThanEqualAndDueAtLessThanOrderByDueAtAsc(from: Instant, to: Instant): List<LearnerCourseReadModel>
}

enum class ReportScope { SELF, ASSIGNED, SYSTEM }
enum class ReportExportStatus { PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED }
enum class ReportFrequency { DAILY, WEEKLY }

@Entity
@Table(
    name = "report_export_jobs",
    uniqueConstraints = [UniqueConstraint(name = "uq_report_schedule_period", columnNames = ["schedule_id", "period_key"])],
    indexes = [Index(name = "idx_report_export_owner", columnList = "owner_id,created_at"), Index(name = "idx_report_export_status", columnList = "status,created_at")],
)
class ReportExportJobEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "owner_id", nullable = false) var ownerId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var scope: ReportScope = ReportScope.SELF,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: ReportExportStatus = ReportExportStatus.PENDING,
    @Column(name = "schedule_id") var scheduleId: UUID? = null,
    @Column(name = "period_key", length = 80) var periodKey: String? = null,
    @Column(name = "output_csv", columnDefinition = "text") var outputCsv: String? = null,
    @Column(name = "row_count") var rowCount: Int? = null,
    @Column(name = "error_message", length = 1000) var errorMessage: String? = null,
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "started_at") var startedAt: Instant? = null,
    @Column(name = "completed_at") var completedAt: Instant? = null,
    @Column(name = "expires_at", nullable = false) var expiresAt: Instant = Instant.now().plusSeconds(7 * 86400),
    @Version var version: Long = 0,
)

@Entity
@Table(name = "report_schedules", indexes = [Index(name = "idx_report_schedule_due", columnList = "enabled,next_run_at")])
class ReportScheduleEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "owner_id", nullable = false) var ownerId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 180) var name: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var scope: ReportScope = ReportScope.SELF,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var frequency: ReportFrequency = ReportFrequency.DAILY,
    @Column(name = "day_of_week") var dayOfWeek: Int? = null,
    @Column(name = "hour_utc", nullable = false) var hourUtc: Int = 0,
    @Column(nullable = false) var enabled: Boolean = true,
    @Column(name = "next_run_at", nullable = false) var nextRunAt: Instant = Instant.now(),
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

interface ReportExportJobRepository : org.springframework.data.jpa.repository.JpaRepository<ReportExportJobEntity, UUID> {
    fun findAllByOwnerIdOrderByCreatedAtDesc(ownerId: UUID): List<ReportExportJobEntity>
    fun findTop20ByStatusOrderByCreatedAtAsc(status: ReportExportStatus): List<ReportExportJobEntity>
    fun findByScheduleIdAndPeriodKey(scheduleId: UUID, periodKey: String): ReportExportJobEntity?
}

interface ReportScheduleRepository : org.springframework.data.jpa.repository.JpaRepository<ReportScheduleEntity, UUID> {
    fun findAllByOwnerIdOrderByCreatedAtDesc(ownerId: UUID): List<ReportScheduleEntity>
    fun findTop50ByEnabledTrueAndNextRunAtBeforeOrderByNextRunAtAsc(now: Instant): List<ReportScheduleEntity>
}
