package com.lmspilot.course.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateDiscussionPostRequest(UUID parentPostId, @NotBlank String content) {
}
