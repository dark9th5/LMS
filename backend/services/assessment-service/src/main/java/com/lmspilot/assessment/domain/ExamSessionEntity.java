package com.lmspilot.assessment.domain;

import com.lmspilot.assessment.platform.AssessmentContextType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "exam_sessions",
    uniqueConstraints = @UniqueConstraint(name = "uq_exam_submit_key", columnNames = "submit_idempotency_key")
)
public class ExamSessionEntity {
    @Id public UUID id = UUID.randomUUID();
    @Column(name = "exam_id", nullable = false) public UUID examId;
    @Column(name = "exam_version", nullable = false) public int examVersion = 1;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "enrollment_id") public UUID enrollmentId;
    @Column(name = "course_id") public UUID courseId;
    @Column(name = "lesson_id") public UUID lessonId;
    @Column(name = "attempt_no", nullable = false) public int attemptNo = 1;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) public ExamSessionStatus status = ExamSessionStatus.IN_PROGRESS;
    @Column(name = "started_at", nullable = false) public Instant startedAt = Instant.now();
    @Column(name = "expires_at", nullable = false) public Instant expiresAt = Instant.now();
    @Column(name = "grace_until", nullable = false) public Instant graceUntil = Instant.now();
    @Column(name = "last_heartbeat_at", nullable = false) public Instant lastHeartbeatAt = Instant.now();
    @Column(name = "suspicious_event_count", nullable = false) public int suspiciousEventCount;
    @Column(name = "submitted_at") public Instant submittedAt;
    @Column(name = "answers_json", nullable = false, columnDefinition = "text") public String answersJson = "{}";
    @Column(name = "questions_snapshot_json", nullable = false, columnDefinition = "text") public String questionsSnapshotJson = "[]";
    @Column(name = "grading_snapshot_json", nullable = false, columnDefinition = "text") public String gradingSnapshotJson = "[]";
    @Column(name = "passing_score_snapshot") public Double passingScoreSnapshot;
    @Column(name = "auto_grade_snapshot") public Boolean autoGradeSnapshot;
    @Enumerated(EnumType.STRING) @Column(name = "context_type_snapshot", length = 40) public AssessmentContextType contextTypeSnapshot;
    @Enumerated(EnumType.STRING) @Column(name = "score_strategy_snapshot", length = 20) public ScoreStrategy scoreStrategySnapshot;
    @Column(name = "submit_idempotency_key", length = 160) public String submitIdempotencyKey;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt = Instant.now();
    @Version @Column(name = "row_version") public long rowVersion;

    public ExamSessionEntity() {}
}
