package com.lmspilot.learning.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignment_submissions")
public class AssignmentSubmissionEntity {
    @Id public UUID id = UUID.randomUUID();
    @Column(name = "enrollment_id", nullable = false) public UUID enrollmentId;
    @Column(name = "class_id", nullable = false) public UUID classId;
    @Column(name = "course_id", nullable = false) public UUID courseId;
    @Column(name = "course_version", nullable = false) public int courseVersion = 1;
    @Column(name = "lesson_id", nullable = false) public UUID lessonId;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "attempt_number", nullable = false) public int attemptNumber = 1;
    @Column(name = "file_id", nullable = false) public UUID fileId;
    @Column(columnDefinition = "text") public String comment;
    @Column(name = "submitted_at", nullable = false) public Instant submittedAt = Instant.now();
    @Column(nullable = false) public boolean late;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) public AssignmentSubmissionStatus status = AssignmentSubmissionStatus.SUBMITTED;
    public Double score;
    @Column(name = "max_score") public Double maxScore;
    @Column(columnDefinition = "text") public String feedback;
    @Column(name = "graded_by") public UUID gradedBy;
    @Column(name = "graded_at") public Instant gradedAt;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 160) public String idempotencyKey;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt = Instant.now();
    @Version public long version;
    public AssignmentSubmissionEntity() {}
}
