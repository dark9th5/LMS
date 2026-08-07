package com.lmspilot.course.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateDiscussionThreadRequest(@NotBlank @Size(max = 240) String title,
                                            UUID lessonId,
                                            @NotBlank String content) {
}
