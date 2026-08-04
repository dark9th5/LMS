package com.lmspilot.operations.domain

import jakarta.persistence.*
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

enum class OperationType { BACKUP, RESTORE, UPDATE, ROLLBACK, MAINTENANCE }
enum class OperationStatus { REQUESTED, RUNNING, SUCCEEDED, FAILED, CANCELLED }

@Entity
@Table(name = "operation_jobs")
class OperationJobEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var type: OperationType = OperationType.BACKUP,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: OperationStatus = OperationStatus.REQUESTED,
    @Column(nullable = false) var requestedBy: UUID = UUID.randomUUID(),
    @Column(nullable = false) var requestedAt: Instant = Instant.now(),
    var startedAt: Instant? = null,
    var finishedAt: Instant? = null,
    @Column(columnDefinition = "text") var parametersJson: String = "{}",
    @Column(columnDefinition = "text") var resultJson: String? = null,
    @Column(columnDefinition = "text") var errorMessage: String? = null,
    @Column(length = 160) var claimedBy: String? = null,
    @Column(length = 120) var claimToken: String? = null,
    var heartbeatAt: Instant? = null,
    var leaseUntil: Instant? = null,
    @Column(nullable = false) var attemptCount: Int = 0,
)

interface OperationJobRepository : org.springframework.data.jpa.repository.JpaRepository<OperationJobEntity, UUID> {
    fun findAllByOrderByRequestedAtDesc(): List<OperationJobEntity>

    @Query(
        value = """
            SELECT * FROM operation_jobs
            WHERE status = 'REQUESTED'
               OR (status = 'RUNNING' AND lease_until IS NOT NULL AND lease_until < now())
            ORDER BY requested_at
            LIMIT 1
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true,
    )
    fun lockNextClaimable(): OperationJobEntity?
}

enum class OperationScheduleFrequency { DAILY, WEEKLY }

@Entity
@Table(name = "operation_schedules", indexes = [Index(name = "idx_operation_schedule_due", columnList = "enabled,next_run_at")])
class OperationScheduleEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 180) var name: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var operationType: OperationType = OperationType.BACKUP,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var frequency: OperationScheduleFrequency = OperationScheduleFrequency.DAILY,
    @Column(name = "day_of_week") var dayOfWeek: Int? = null,
    @Column(name = "hour_utc", nullable = false) var hourUtc: Int = 0,
    @Column(nullable = false, columnDefinition = "text") var parametersJson: String = "{}",
    @Column(nullable = false) var enabled: Boolean = true,
    @Column(name = "next_run_at", nullable = false) var nextRunAt: Instant = Instant.now(),
    @Column(name = "created_by", nullable = false) var createdBy: UUID = UUID.randomUUID(),
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

interface OperationScheduleRepository : org.springframework.data.jpa.repository.JpaRepository<OperationScheduleEntity, UUID> {
    fun findAllByOrderByCreatedAtDesc(): List<OperationScheduleEntity>
    fun findTop50ByEnabledTrueAndNextRunAtBeforeOrderByNextRunAtAsc(now: Instant): List<OperationScheduleEntity>
}
