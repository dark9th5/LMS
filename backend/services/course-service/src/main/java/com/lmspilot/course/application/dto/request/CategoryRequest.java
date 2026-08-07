package com.lmspilot.course.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CategoryRequest(@NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 180) String name,
        UUID parentId,
        Integer sortOrder) {
}
