package com.lmspilot.notification.domain

import jakarta.persistence.*
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

enum class ReminderRuleType { COURSE_DUE }

@Entity
@Table(
    name = "notification_templates",
    uniqueConstraints = [UniqueConstraint(name = "uq_notification_template_code", columnNames = ["code"])],
    indexes = [Index(name = "idx_notification_template_event", columnList = "event_type,active")],
)
class NotificationTemplateEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 80) var code: String = "",
    @Column(nullable = false, length = 180) var name: String = "",
    @Column(name = "event_type", length = 120) var eventType: String? = null,
    @Column(name = "title_template", nullable = false, length = 240) var titleTemplate: String = "",
    @Column(name = "body_template", nullable = false, columnDefinition = "text") var bodyTemplate: String = "",
    @Column(name = "in_app_enabled", nullable = false) var inAppEnabled: Boolean = true,
    @Column(name = "email_enabled", nullable = false) var emailEnabled: Boolean = false,
    @Column(nullable = false) var active: Boolean = true,
    @Column(name = "created_by", nullable = false) var createdBy: UUID = UUID.randomUUID(),
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

@Entity
@Table(
    name = "notification_reminder_rules",
    indexes = [Index(name = "idx_notification_reminder_due", columnList = "enabled,next_run_at")],
)
class NotificationReminderRuleEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 180) var name: String = "",
    @Enumerated(EnumType.STRING) @Column(name = "rule_type", nullable = false, length = 30) var ruleType: ReminderRuleType = ReminderRuleType.COURSE_DUE,
    @Column(name = "template_id", nullable = false) var templateId: UUID = UUID.randomUUID(),
    @Column(name = "relative_days", nullable = false) var relativeDays: Int = 7,
    @Column(name = "hour_utc", nullable = false) var hourUtc: Int = 0,
    @Column(nullable = false) var enabled: Boolean = true,
    @Column(name = "next_run_at", nullable = false) var nextRunAt: Instant = Instant.now(),
    @Column(name = "created_by", nullable = false) var createdBy: UUID = UUID.randomUUID(),
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

@Entity
@Table(
    name = "notification_reminder_dispatches",
    uniqueConstraints = [UniqueConstraint(name = "uq_notification_reminder_dispatch", columnNames = ["rule_id", "enrollment_id", "due_at"])],
)
class NotificationReminderDispatchEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "rule_id", nullable = false) var ruleId: UUID = UUID.randomUUID(),
    @Column(name = "enrollment_id", nullable = false) var enrollmentId: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(name = "due_at", nullable = false) var dueAt: Instant = Instant.now(),
    @Column(name = "source_event_id", nullable = false) var sourceEventId: UUID = UUID.randomUUID(),
    @Column(name = "dispatched_at", nullable = false) var dispatchedAt: Instant = Instant.now(),
)

interface NotificationTemplateRepository : org.springframework.data.jpa.repository.JpaRepository<NotificationTemplateEntity, UUID> {
    fun findByCodeIgnoreCase(code: String): NotificationTemplateEntity?
    fun findFirstByEventTypeAndActiveTrueOrderByUpdatedAtDesc(eventType: String): NotificationTemplateEntity?
    fun findAllByOrderByUpdatedAtDesc(): List<NotificationTemplateEntity>
}

interface NotificationReminderRuleRepository : org.springframework.data.jpa.repository.JpaRepository<NotificationReminderRuleEntity, UUID> {
    fun findAllByOrderByUpdatedAtDesc(): List<NotificationReminderRuleEntity>
    fun countByTemplateId(templateId: UUID): Long
    fun findTop50ByEnabledTrueAndNextRunAtBeforeOrderByNextRunAtAsc(now: Instant): List<NotificationReminderRuleEntity>
}

interface NotificationReminderDispatchRepository : org.springframework.data.jpa.repository.JpaRepository<NotificationReminderDispatchEntity, UUID> {
    @Modifying
    @Query(
        value = """
            INSERT INTO notification_reminder_dispatches
                (id, rule_id, enrollment_id, user_id, due_at, source_event_id, dispatched_at)
            VALUES
                (:id, :ruleId, :enrollmentId, :userId, :dueAt, :sourceEventId, :dispatchedAt)
            ON CONFLICT (rule_id, enrollment_id, due_at) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun claim(
        @Param("id") id: UUID,
        @Param("ruleId") ruleId: UUID,
        @Param("enrollmentId") enrollmentId: UUID,
        @Param("userId") userId: UUID,
        @Param("dueAt") dueAt: Instant,
        @Param("sourceEventId") sourceEventId: UUID,
        @Param("dispatchedAt") dispatchedAt: Instant,
    ): Int

    @Modifying
    @Query(
        "delete from NotificationReminderDispatchEntity d where d.ruleId = :ruleId and d.enrollmentId = :enrollmentId and d.dueAt = :dueAt"
    )
    fun release(@Param("ruleId") ruleId: UUID, @Param("enrollmentId") enrollmentId: UUID, @Param("dueAt") dueAt: Instant): Int

    fun countByRuleId(ruleId: UUID): Long
}
