package com.lmspilot.course.application.dto.response;

import java.util.List;
import java.util.UUID;

public record CourseSnapshot(UUID id, String code, String name, String description, String objectives,
                             String targetAudience, Integer durationMinutes, double passingScore,
                             String completionPolicyJson, UUID categoryId, int version, UUID ownerId,
                             List<LessonResponse> lessons) {
}
