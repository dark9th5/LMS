package com.lmspilot.course.domain.model;

import com.lmspilot.course.domain.enums.DiscussionThreadStatus;
import java.time.Instant;
import java.util.UUID;

public class DiscussionThread {
    public UUID id = UUID.randomUUID();
    public UUID courseId;
    public UUID lessonId;
    public String title = "";
    public UUID authorId;
    public DiscussionThreadStatus status = DiscussionThreadStatus.OPEN;
    public boolean pinned = false;
    public int postCount = 0;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
    public long version = 0;
}
