package com.lmspilot.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_reminder_rules")
public class NotificationReminderRuleEntity {
    @Id public UUID id = UUID.randomUUID();
    @Column(nullable = false, length = 180) public String name = "";
    @Enumerated(EnumType.STRING) @Column(name = "rule_type", nullable = false, length = 30) public ReminderRuleType ruleType = ReminderRuleType.COURSE_DUE;
    @Column(name = "template_id", nullable = false) public UUID templateId;
    @Column(name = "relative_days", nullable = false) public int relativeDays = 1;
    @Column(name = "hour_utc", nullable = false) public int hourUtc;
    @Column(nullable = false) public boolean enabled = true;
    @Column(name = "next_run_at", nullable = false) public Instant nextRunAt = Instant.now();
    @Column(name = "created_by", nullable = false) public UUID createdBy;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) public Instant updatedAt = Instant.now();
    @Version public long version;
    public NotificationReminderRuleEntity() {}
}
