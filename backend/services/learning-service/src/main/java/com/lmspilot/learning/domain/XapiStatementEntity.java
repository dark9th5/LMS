package com.lmspilot.learning.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "xapi_statements")
public class XapiStatementEntity {
    @Id public UUID id = UUID.randomUUID();
    @Column(name = "actor_user_id", nullable = false) public UUID actorUserId;
    @Column(nullable = false, length = 180) public String verb = "";
    @Column(name = "object_id", nullable = false, length = 500) public String objectId = "";
    @Enumerated(EnumType.STRING) @Column(name = "object_type", nullable = false, length = 30) public XapiObjectType objectType = XapiObjectType.OTHER;
    @Column(name = "course_id") public UUID courseId;
    @Column(name = "lesson_id") public UUID lessonId;
    @Column(name = "enrollment_id") public UUID enrollmentId;
    @Column(name = "result_score") public Double resultScore;
    @Column(name = "result_success") public Boolean resultSuccess;
    @Column(name = "result_completion") public Boolean resultCompletion;
    @Column(name = "duration_seconds") public Long durationSeconds;
    @Column(name = "context_json", nullable = false, columnDefinition = "text") public String contextJson = "{}";
    @Column(name = "occurred_at", nullable = false) public Instant occurredAt = Instant.now();
    @Column(name = "stored_at", nullable = false) public Instant storedAt = Instant.now();
    @Column(nullable = false, length = 80) public String source = "WEB";
    public XapiStatementEntity() {}
}
