package com.lmspilot.course.application.dto.response;

import com.lmspilot.course.domain.enums.DiscussionPostStatus;
import java.time.Instant;
import java.util.UUID;

public record DiscussionPostResponse(UUID id, UUID authorId, UUID parentPostId, String content,
                                     DiscussionPostStatus status, Instant createdAt) {
}
