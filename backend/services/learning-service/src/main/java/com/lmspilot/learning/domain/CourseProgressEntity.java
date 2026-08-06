package com.lmspilot.learning.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_progress")
public class CourseProgressEntity {
    @Id public UUID id = UUID.randomUUID();
    @Column(name = "enrollment_id", nullable = false, unique = true) public UUID enrollmentId;
    @Column(name = "course_id", nullable = false) public UUID courseId;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "course_version", nullable = false) public int courseVersion = 1;
    @Column(name = "progress_percent", nullable = false) public int progressPercent;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) public LearningStatus status = LearningStatus.NOT_STARTED;
    @Column(name = "last_lesson_id") public UUID lastLessonId;
    @Column(name = "last_position", length = 500) public String lastPosition;
    @Column(name = "total_learning_seconds", nullable = false) public long totalLearningSeconds;
    @Column(name = "started_at") public Instant startedAt;
    @Column(name = "last_accessed_at") public Instant lastAccessedAt;
    @Column(name = "completed_at") public Instant completedAt;
    @Column(name = "completion_event_published", nullable = false) public boolean completionEventPublished;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt = Instant.now();
    @Version public long version;
    public CourseProgressEntity() {}
}
