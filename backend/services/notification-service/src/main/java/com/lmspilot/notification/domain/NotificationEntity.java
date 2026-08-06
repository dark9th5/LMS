package com.lmspilot.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id public UUID id = UUID.randomUUID();
    @Column(name = "source_event_id", nullable = false) public UUID sourceEventId = UUID.randomUUID();
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(nullable = false, length = 120) public String type = "GENERAL";
    @Column(nullable = false, length = 240) public String title = "";
    @Column(nullable = false, columnDefinition = "text") public String body = "";
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public NotificationChannel channel = NotificationChannel.IN_APP;
    @Enumerated(EnumType.STRING) @Column(name = "delivery_status", nullable = false, length = 20) public DeliveryStatus deliveryStatus = DeliveryStatus.CREATED;
    @Column(nullable = false) public boolean read;
    @Column(name = "read_at") public Instant readAt;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
    @Column(name = "delivered_at") public Instant deliveredAt;
    @Column(name = "last_error", length = 2000) public String lastError;
    @Column(name = "recipient_email", length = 320) public String recipientEmail;
    @Column(name = "attempt_count", nullable = false) public int attemptCount;
    @Column(name = "next_attempt_at") public Instant nextAttemptAt;

    public NotificationEntity() {}
}
