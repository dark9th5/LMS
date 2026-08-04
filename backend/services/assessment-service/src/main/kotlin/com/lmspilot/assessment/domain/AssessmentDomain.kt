package com.lmspilot.assessment.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class QuestionType { SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, SHORT_TEXT, ESSAY }
enum class QuestionStatus { DRAFT, ACTIVE, ARCHIVED }
enum class ExamStatus { DRAFT, ACTIVE, INACTIVE, ARCHIVED }
enum class ExamSessionStatus { IN_PROGRESS, SUBMITTED, EXPIRED, GRADED }

enum class ScoreStrategy { HIGHEST, LATEST, AVERAGE }

@Entity
@Table(name = "questions")
class QuestionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false) var ownerId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var type: QuestionType = QuestionType.SINGLE_CHOICE,
    @Column(nullable = false, columnDefinition = "text") var prompt: String = "",
    @Column(nullable = false, columnDefinition = "text") var optionsJson: String = "[]",
    @Column(nullable = false, columnDefinition = "text") var correctAnswersJson: String = "[]",
    @Column(columnDefinition = "text") var explanation: String? = null,
    @Column(nullable = false) var difficulty: Int = 1,
    @Column(nullable = false, length = 500) var tagsCsv: String = "",
    @Column(nullable = false) var defaultPoints: Double = 1.0,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: QuestionStatus = QuestionStatus.ACTIVE,
    @Column(nullable = false) var questionVersion: Int = 1,
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var rowVersion: Long = 0,
)

@Entity
@Table(name = "exams")
class ExamEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 220) var title: String = "",
    var courseId: UUID? = null,
    var lessonId: UUID? = null,
    @Column(nullable = false) var durationMinutes: Int = 30,
    var opensAt: Instant? = null,
    var closesAt: Instant? = null,
    @Column(nullable = false) var maxAttempts: Int = 1,
    @Column(nullable = false) var waitMinutesBetweenAttempts: Int = 0,
    @Column(nullable = false) var passingScore: Double = 70.0,
    @Column(nullable = false) var shuffleQuestions: Boolean = false,
    @Column(nullable = false) var shuffleAnswers: Boolean = false,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var scoreStrategy: ScoreStrategy = ScoreStrategy.HIGHEST,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: ExamStatus = ExamStatus.DRAFT,
    @Column(nullable = false) var examVersion: Int = 1,
    @Column(nullable = false) var ownerId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var rowVersion: Long = 0,
)

@Entity
@Table(name = "exam_questions", uniqueConstraints = [UniqueConstraint(name = "uq_exam_question", columnNames = ["exam_id", "question_id"])])
class ExamQuestionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "exam_id", nullable = false) var examId: UUID = UUID.randomUUID(),
    @Column(name = "question_id", nullable = false) var questionId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var questionVersion: Int = 1,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var type: QuestionType = QuestionType.SINGLE_CHOICE,
    @Column(nullable = false, columnDefinition = "text") var promptSnapshot: String = "",
    @Column(nullable = false, columnDefinition = "text") var optionsSnapshotJson: String = "[]",
    @Column(nullable = false, columnDefinition = "text") var correctAnswersSnapshotJson: String = "[]",
    @Column(nullable = false) var points: Double = 1.0,
    @Column(nullable = false) var sortOrder: Int = 0,
)

@Entity
@Table(
    name = "exam_sessions",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_exam_submit_key", columnNames = ["submit_idempotency_key"]),
    ],
)
class ExamSessionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false) var examId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var examVersion: Int = 1,
    @Column(nullable = false) var userId: UUID = UUID.randomUUID(),
    var enrollmentId: UUID? = null,
    var courseId: UUID? = null,
    var lessonId: UUID? = null,
    @Column(nullable = false) var attemptNo: Int = 1,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: ExamSessionStatus = ExamSessionStatus.IN_PROGRESS,
    @Column(nullable = false) var startedAt: Instant = Instant.now(),
    @Column(nullable = false) var expiresAt: Instant = Instant.now(),
    @Column(nullable = false) var graceUntil: Instant = Instant.now(),
    @Column(nullable = false) var lastHeartbeatAt: Instant = Instant.now(),
    @Column(nullable = false) var suspiciousEventCount: Int = 0,
    var submittedAt: Instant? = null,
    @Column(nullable = false, columnDefinition = "text") var answersJson: String = "{}",
    @Column(length = 160) var submitIdempotencyKey: String? = null,
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var rowVersion: Long = 0,
)

interface QuestionRepository : org.springframework.data.jpa.repository.JpaRepository<QuestionEntity, UUID> {
    fun findAllByOwnerIdOrderByUpdatedAtDesc(ownerId: UUID): List<QuestionEntity>
}
interface ExamRepository : org.springframework.data.jpa.repository.JpaRepository<ExamEntity, UUID> {
    fun findAllByOwnerIdOrderByUpdatedAtDesc(ownerId: UUID): List<ExamEntity>
    fun findAllByStatusOrderByUpdatedAtDesc(status: ExamStatus): List<ExamEntity>

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @org.springframework.data.jpa.repository.Query("select exam from ExamEntity exam where exam.id = :id")
    fun findStartSnapshotById(@org.springframework.data.repository.query.Param("id") id: UUID): ExamEntity?
}
interface ExamQuestionRepository : org.springframework.data.jpa.repository.JpaRepository<ExamQuestionEntity, UUID> {
    fun findAllByExamIdOrderBySortOrderAsc(examId: UUID): List<ExamQuestionEntity>
    fun deleteAllByExamId(examId: UUID)
}
interface ExamSessionRepository : org.springframework.data.jpa.repository.JpaRepository<ExamSessionEntity, UUID> {
    fun countByExamIdAndUserId(examId: UUID, userId: UUID): Long
    fun findBySubmitIdempotencyKey(key: String): ExamSessionEntity?
    fun findAllByExamIdAndUserIdOrderByAttemptNoAsc(examId: UUID, userId: UUID): List<ExamSessionEntity>
    fun findAllByExamIdAndEnrollmentIdOrderByAttemptNoAsc(examId: UUID, enrollmentId: UUID): List<ExamSessionEntity>
    fun findAllByExamIdAndUserIdAndEnrollmentIdIsNullOrderByAttemptNoAsc(examId: UUID, userId: UUID): List<ExamSessionEntity>
    fun existsByExamId(examId: UUID): Boolean

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select session from ExamSessionEntity session where session.id = :id")
    fun findForUpdateById(@org.springframework.data.repository.query.Param("id") id: UUID): ExamSessionEntity?

    @org.springframework.data.jpa.repository.Query(
        value = """
            select 1
            from (select pg_advisory_xact_lock(hashtextextended(cast(:lockKey as text), 0))) as attempt_lock
        """,
        nativeQuery = true,
    )
    fun lockAttemptSequence(@org.springframework.data.repository.query.Param("lockKey") lockKey: String): Int
}

@Entity
@Table(name = "question_provenance", uniqueConstraints = [UniqueConstraint(name = "uq_question_provenance_question", columnNames = ["question_id"])])
class QuestionProvenanceEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "question_id", nullable = false) var questionId: UUID = UUID.randomUUID(),
    @Column(name = "course_id", nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 240) var externalId: String = "",
    @Column(nullable = false, columnDefinition = "text") var citationsJson: String = "[]",
    @Column(nullable = false, columnDefinition = "text") var sourceDocumentVersionsJson: String = "[]",
    @Column(nullable = false, columnDefinition = "text") var generatorMetadataJson: String = "{}",
    @Column(nullable = false) var importedBy: UUID = UUID.randomUUID(),
    @Column(nullable = false) var importedAt: Instant = Instant.now(),
)

interface QuestionProvenanceRepository : org.springframework.data.jpa.repository.JpaRepository<QuestionProvenanceEntity, UUID> {
    fun findByQuestionId(questionId: UUID): QuestionProvenanceEntity?
}


enum class ExamSessionEventType { HEARTBEAT, TAB_HIDDEN, WINDOW_BLUR, FULLSCREEN_EXIT, NETWORK_DISCONNECTED, NETWORK_RECONNECTED, CLIENT_WARNING }

@Entity
@Table(name = "exam_session_events", indexes = [Index(name = "idx_exam_session_event", columnList = "session_id,occurred_at")])
class ExamSessionEventEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "session_id", nullable = false) var sessionId: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) var type: ExamSessionEventType = ExamSessionEventType.CLIENT_WARNING,
    @Column(columnDefinition = "text") var details: String? = null,
    @Column(name = "occurred_at", nullable = false) var occurredAt: Instant = Instant.now(),
    @Column(name = "stored_at", nullable = false) var storedAt: Instant = Instant.now(),
)

interface ExamSessionEventRepository : org.springframework.data.jpa.repository.JpaRepository<ExamSessionEventEntity, UUID> {
    fun findAllBySessionIdOrderByOccurredAtAsc(sessionId: UUID): List<ExamSessionEventEntity>
}
