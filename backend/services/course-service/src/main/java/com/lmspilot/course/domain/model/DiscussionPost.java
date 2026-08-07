package com.lmspilot.course.domain.model;

import com.lmspilot.course.domain.enums.DiscussionPostStatus;
import java.time.Instant;
import java.util.UUID;

public class DiscussionPost {
    public UUID id = UUID.randomUUID();
    public UUID threadId;
    public UUID authorId;
    public UUID parentPostId;
    public String content = "";
    public DiscussionPostStatus status = DiscussionPostStatus.VISIBLE;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
    public long version = 0;
}
