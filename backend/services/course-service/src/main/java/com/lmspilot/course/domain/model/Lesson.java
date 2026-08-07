package com.lmspilot.course.domain.model;

import com.lmspilot.course.domain.enums.LessonType;
import java.time.Instant;
import java.util.UUID;

public class Lesson {
    public UUID id = UUID.randomUUID();
    public UUID courseId;
    public String title = "";
    public LessonType type = LessonType.TEXT;
    public String textContent;
    public UUID fileId;
    public boolean required = true;
    public int sortOrder = 0;
    public int estimatedMinutes = 0;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
    public long version = 0;
}
