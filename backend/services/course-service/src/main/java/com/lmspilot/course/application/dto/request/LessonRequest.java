package com.lmspilot.course.application.dto.request;

import com.lmspilot.course.domain.enums.LessonType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record LessonRequest(@NotBlank @Size(max = 220) String title,
                            @NotNull LessonType type,
                            String textContent,
                            UUID fileId,
                            Boolean required,
                            @Min(0) Integer sortOrder,
                            @Min(0) Integer estimatedMinutes) {
}
