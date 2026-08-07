package com.lmspilot.course.application.dto.command;

import com.lmspilot.course.domain.enums.CourseStatus;
import java.util.UUID;

public final class CourseCommand {
    private CourseCommand() {
    }

    public record Upsert(String code, String name, String description, String objectives, String targetAudience,
            Integer durationMinutes, Double passingScore, String completionPolicyJson, UUID categoryId) {
    }

    public record TransitionStatus(CourseStatus status) {
    }
}
