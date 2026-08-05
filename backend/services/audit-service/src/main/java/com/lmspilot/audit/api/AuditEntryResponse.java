package com.lmspilot.audit.api;

import java.time.Instant;
import java.util.UUID;

public record AuditEntryResponse(UUID id, String actorId, String actorUsername, String action, String resourceType,
                                 String resourceId, String outcome, String beforeJson, String afterJson,
                                 String ipAddress, String correlationId, Instant occurredAt) {}
