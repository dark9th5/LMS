package com.lmspilot.course.domain.model;

import java.time.Instant;
import java.util.UUID;

public class CourseVersion {
    public UUID id = UUID.randomUUID();
    public UUID courseId;
    public int versionNumber = 1;
    public String snapshotJson = "{}";
    public UUID createdBy;
    public Instant createdAt = Instant.now();
}
