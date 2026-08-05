package com.lmspilot.support.api;

import java.time.Instant;
import java.util.Map;

public record ApiError(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    String correlationId,
    Map<String, String> fieldErrors
) {
    public ApiError {
        timestamp = timestamp == null ? Instant.now() : timestamp;
        fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }
}
