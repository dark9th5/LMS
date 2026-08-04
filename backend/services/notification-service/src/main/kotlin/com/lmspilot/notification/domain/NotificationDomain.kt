package com.lmspilot.notification.domain

import jakarta.persistence.*
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

enum class NotificationChannel { IN_APP, EMAIL }
enum class DeliveryStatus { CREATED, PROCESSING, SENT, FAILED, DEAD, SKIPPED }

@Entity
@Table(name = "notifications", uniqueConstraints = [UniqueConstraint(name = "uq_notification_event_user", columnNames = ["source_event_id", "user_id", "channel"])])
class NotificationEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "source_event_id", nullable = false) var sourceEventId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 120) var type: String = "GENERAL",
    @Column(nullable = false, length = 240) var title: String = "",
    @Column(nullable = false, columnDefinition = "text") var body: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var channel: NotificationChannel = NotificationChannel.IN_APP,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var deliveryStatus: DeliveryStatus = DeliveryStatus.CREATED,
    @Column(nullable = false) var read: Boolean = false,
    var readAt: Instant? = null,
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    var deliveredAt: Instant? = null,
    var lastError: String? = null,
    @Column(length = 320) var recipientEmail: String? = null,
    @Column(nullable = false) var attemptCount: Int = 0,
    var nextAttemptAt: Instant? = null,
)

interface NotificationRepository : org.springframework.data.jpa.repository.JpaRepository<NotificationEntity, UUID> {
    @Modifying
    @Query(
        value = """
            INSERT INTO notifications
                (id, source_event_id, user_id, type, title, body, channel, delivery_status, read, created_at, attempt_count, next_attempt_at)
            VALUES
                (:id, :sourceEventId, :userId, :type, :title, :body, :channel, 'CREATED', false, :createdAt, 0, :nextAttemptAt)
            ON CONFLICT (source_event_id, user_id, channel) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("id") id: UUID,
        @Param("sourceEventId") sourceEventId: UUID,
        @Param("userId") userId: UUID,
        @Param("type") type: String,
        @Param("title") title: String,
        @Param("body") body: String,
        @Param("channel") channel: String,
        @Param("createdAt") createdAt: Instant,
        @Param("nextAttemptAt") nextAttemptAt: Instant?,
    ): Int

    fun findAllByUserIdAndChannelOrderByCreatedAtDesc(userId: UUID, channel: NotificationChannel): List<NotificationEntity>
    fun existsBySourceEventIdAndUserIdAndChannel(eventId: UUID, userId: UUID, channel: NotificationChannel): Boolean
    fun countByUserIdAndChannelAndReadFalse(userId: UUID, channel: NotificationChannel): Long

    @Query(
        value = """
            SELECT * FROM notifications
            WHERE channel = 'EMAIL'
              AND delivery_status IN ('CREATED', 'FAILED', 'PROCESSING')
              AND next_attempt_at IS NOT NULL
              AND next_attempt_at <= now()
            ORDER BY next_attempt_at, created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true,
    )
    fun lockDueEmails(@Param("limit") limit: Int): List<NotificationEntity>
}
