package com.lmspilot.course.domain.model;

import com.lmspilot.course.domain.enums.CourseStatus;
import java.time.Instant;
import java.util.UUID;

public class Course {
    public UUID id = UUID.randomUUID();
    public String code = "";
    public String name = "";
    public String description;
    public String objectives;
    public String targetAudience;
    public Integer durationMinutes;
    public double passingScore = 70.0;
    public String completionPolicyJson = "{\"requiredLessonPercent\":100}";
    public UUID categoryId;
    public CourseStatus status = CourseStatus.DRAFT;
    public int contentVersion = 1;
    public int publishedVersion = 0;
    public Instant publishedAt;
    public UUID publishedBy;
    public UUID ownerId = UUID.randomUUID();
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
    public long rowVersion = 0;
}
