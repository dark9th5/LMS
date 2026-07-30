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
@Table(name = "exam_sessions", uniqueConstraints = [UniqueConstraint(name = "uq_exam_submit_key", columnNames = ["submit_idempotency_key"])])
class ExamSessionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false) var examId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var examVersion: Int = 1,
    @Column(nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var attemptNo: Int = 1,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: ExamSessionStatus = ExamSessionStatus.IN_PROGRESS,
    @Column(nullable = false) var startedAt: Instant = Instant.now(),
    @Column(nullable = false) var expiresAt: Instant = Instant.now(),
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
}
interface ExamQuestionRepository : org.springframework.data.jpa.repository.JpaRepository<ExamQuestionEntity, UUID> {
    fun findAllByExamIdOrderBySortOrderAsc(examId: UUID): List<ExamQuestionEntity>
    fun deleteAllByExamId(examId: UUID)
}
interface ExamSessionRepository : org.springframework.data.jpa.repository.JpaRepository<ExamSessionEntity, UUID> {
    fun countByExamIdAndUserId(examId: UUID, userId: UUID): Long
    fun findBySubmitIdempotencyKey(key: String): ExamSessionEntity?
    fun findAllByExamIdAndUserIdOrderByAttemptNoAsc(examId: UUID, userId: UUID): List<ExamSessionEntity>
    fun existsByExamId(examId: UUID): Boolean
}
