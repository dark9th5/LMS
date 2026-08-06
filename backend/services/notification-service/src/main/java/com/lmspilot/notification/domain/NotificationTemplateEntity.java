package com.lmspilot.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_templates")
public class NotificationTemplateEntity {
    @Id public UUID id = UUID.randomUUID();
    @Column(nullable = false, unique = true, length = 80) public String code = "";
    @Column(nullable = false, length = 180) public String name = "";
    @Column(name = "event_type", length = 120) public String eventType;
    @Column(name = "title_template", nullable = false, length = 240) public String titleTemplate = "";
    @Column(name = "body_template", nullable = false, columnDefinition = "text") public String bodyTemplate = "";
    @Column(name = "in_app_enabled", nullable = false) public boolean inAppEnabled = true;
    @Column(name = "email_enabled", nullable = false) public boolean emailEnabled;
    @Column(nullable = false) public boolean active = true;
    @Column(name = "created_by", nullable = false) public UUID createdBy;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) public Instant updatedAt = Instant.now();
    @Version public long version;
    public NotificationTemplateEntity() {}
}
