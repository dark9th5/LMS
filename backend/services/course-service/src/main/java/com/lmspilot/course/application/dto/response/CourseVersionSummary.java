package com.lmspilot.course.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CourseVersionSummary(UUID id, UUID courseId, int versionNumber, UUID createdBy, Instant createdAt) {
}
