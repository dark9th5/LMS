package com.lmspilot.learning.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "lesson_progress",
    uniqueConstraints = @UniqueConstraint(name = "uq_lesson_progress", columnNames = {"enrollment_id", "lesson_id"})
)
public class LessonProgressEntity {
    @Id public UUID id = UUID.randomUUID();
    @Column(name = "enrollment_id", nullable = false) public UUID enrollmentId;
    @Column(name = "course_id", nullable = false) public UUID courseId;
    @Column(name = "lesson_id", nullable = false) public UUID lessonId;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(nullable = false) public boolean completed;
    @Column(name = "learning_seconds", nullable = false) public long learningSeconds;
    @Column(length = 500) public String position;
    @Column(name = "opened_at") public Instant openedAt;
    @Column(name = "completed_at") public Instant completedAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt = Instant.now();
    @Version public long version;
    public LessonProgressEntity() {}
}
