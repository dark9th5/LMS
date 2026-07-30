package com.lmspilot.notification.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class NotificationChannel { IN_APP, EMAIL }
enum class DeliveryStatus { CREATED, SENT, FAILED, SKIPPED }

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
)
interface NotificationRepository : org.springframework.data.jpa.repository.JpaRepository<NotificationEntity, UUID> {
    fun findAllByUserIdAndChannelOrderByCreatedAtDesc(userId: UUID, channel: NotificationChannel): List<NotificationEntity>
    fun existsBySourceEventIdAndUserIdAndChannel(eventId: UUID, userId: UUID, channel: NotificationChannel): Boolean
    fun countByUserIdAndChannelAndReadFalse(userId: UUID, channel: NotificationChannel): Long
}
