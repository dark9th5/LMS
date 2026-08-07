package com.lmspilot.course.domain.model;

import com.lmspilot.course.domain.enums.RecordStatus;
import java.time.Instant;
import java.util.UUID;

public class CourseCategory {
    public UUID id = UUID.randomUUID();
    public String code = "";
    public String name = "";
    public UUID parentId;
    public RecordStatus status = RecordStatus.ACTIVE;
    public int sortOrder = 0;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
    public long version = 0;
}
