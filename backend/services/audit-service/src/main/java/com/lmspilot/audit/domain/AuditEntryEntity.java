package com.lmspilot.audit.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_entries", uniqueConstraints = @UniqueConstraint(name = "uq_audit_event", columnNames = "event_id"))
public class AuditEntryEntity {
    @Id private UUID id = UUID.randomUUID();
    @Column(name = "event_id", nullable = false) private UUID eventId = UUID.randomUUID();
    private String actorId;
    private String actorUsername;
    @Column(nullable = false, length = 120) private String action = "";
    @Column(nullable = false, length = 120) private String resourceType = "";
    private String resourceId;
    @Column(nullable = false, length = 40) private String outcome = "SUCCESS";
    @Column(columnDefinition = "text") private String beforeJson;
    @Column(columnDefinition = "text") private String afterJson;
    private String ipAddress;
    private String correlationId;
    @Column(nullable = false) private Instant occurredAt = Instant.now();

    protected AuditEntryEntity() {}
    public AuditEntryEntity(UUID eventId, String actorId, String actorUsername, String action, String resourceType,
                            String resourceId, String outcome, String beforeJson, String afterJson,
                            String ipAddress, String correlationId, Instant occurredAt) {
        this.eventId=eventId; this.actorId=actorId; this.actorUsername=actorUsername; this.action=action;
        this.resourceType=resourceType; this.resourceId=resourceId; this.outcome=outcome; this.beforeJson=beforeJson;
        this.afterJson=afterJson; this.ipAddress=ipAddress; this.correlationId=correlationId; this.occurredAt=occurredAt;
    }
    public UUID getId(){return id;} public UUID getEventId(){return eventId;} public String getActorId(){return actorId;}
    public String getActorUsername(){return actorUsername;} public String getAction(){return action;}
    public String getResourceType(){return resourceType;} public String getResourceId(){return resourceId;}
    public String getOutcome(){return outcome;} public String getBeforeJson(){return beforeJson;}
    public String getAfterJson(){return afterJson;} public String getIpAddress(){return ipAddress;}
    public String getCorrelationId(){return correlationId;} public Instant getOccurredAt(){return occurredAt;}
}
