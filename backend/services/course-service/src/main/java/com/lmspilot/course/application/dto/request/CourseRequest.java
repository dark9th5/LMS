package com.lmspilot.course.application.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CourseRequest(@NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 240) String name,
        String description,
        String objectives,
        String targetAudience,
        @Min(0) Integer durationMinutes,
        @DecimalMin("0") @DecimalMax("100") Double passingScore,
        String completionPolicyJson,
        UUID categoryId) {
}
