package com.lmspilot.course.application.dto.response;

import com.lmspilot.course.domain.enums.DiscussionThreadStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DiscussionThreadResponse(UUID id, UUID courseId, UUID lessonId, String title, UUID authorId,
                                       DiscussionThreadStatus status, boolean pinned, int postCount,
                                       Instant createdAt, Instant updatedAt, List<DiscussionPostResponse> posts) {
}
