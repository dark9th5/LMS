package com.lmspilot.course.application.dto.response;

import com.lmspilot.course.domain.enums.LessonType;
import java.util.UUID;

public record LessonResponse(UUID id, String title, LessonType type, String textContent, UUID fileId,
                             boolean required, int sortOrder, int estimatedMinutes) {
}
