package com.lmspilot.contracts;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

import java.util.*;
public record AuditPayload(String actorId, String actorUsername, String action, String resourceType, String resourceId, String outcome, JsonNode beforeJson, JsonNode afterJson, String ipAddress) {
}
