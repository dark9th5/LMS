package com.lmspilot.course.application.dto.response;

import com.lmspilot.course.domain.enums.CourseStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CourseResponse(UUID id, String code, String name, String description, String objectives,
                             String targetAudience, Integer durationMinutes, double passingScore,
                             String completionPolicyJson, UUID categoryId, CourseStatus status,
                             int contentVersion, int publishedVersion, boolean hasUnpublishedChanges,
                             Instant publishedAt, UUID ownerId, List<LessonResponse> lessons) {
}
