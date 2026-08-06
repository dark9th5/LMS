package com.lmspilot.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_reminder_dispatches")
public class NotificationReminderDispatchEntity {
    @Id public UUID id = UUID.randomUUID();
    @Column(name = "rule_id", nullable = false) public UUID ruleId;
    @Column(name = "enrollment_id", nullable = false) public UUID enrollmentId;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "due_at", nullable = false) public Instant dueAt;
    @Column(name = "source_event_id", nullable = false) public UUID sourceEventId;
    @Column(name = "dispatched_at", nullable = false) public Instant dispatchedAt = Instant.now();
    public NotificationReminderDispatchEntity() {}
}
