package com.lmspilot.course.application.dto.command;

import com.lmspilot.course.domain.enums.LessonType;
import java.util.UUID;

public final class LessonCommand {
    private LessonCommand() {
    }

    public record Upsert(String title, LessonType type, String textContent, UUID fileId, Boolean required,
            Integer sortOrder, Integer estimatedMinutes) {
    }
}
