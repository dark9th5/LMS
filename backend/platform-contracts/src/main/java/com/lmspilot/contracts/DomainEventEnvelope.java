package com.lmspilot.contracts;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record DomainEventEnvelope(
    UUID eventId, String eventType, int eventVersion, Instant occurredAt,
    String correlationId, String producer, String aggregateId, JsonNode payload
) {
    public DomainEventEnvelope(String eventType, String correlationId, String producer, String aggregateId, JsonNode payload) {
        this(UUID.randomUUID(), eventType, 1, Instant.now(), correlationId, producer, aggregateId, payload);
    }
}
