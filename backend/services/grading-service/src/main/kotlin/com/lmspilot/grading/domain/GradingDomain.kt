package com.lmspilot.grading.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class GradeStatus { AUTO_GRADED, PENDING_MANUAL, COMPLETED }

@Entity
@Table(name = "grade_results", uniqueConstraints = [UniqueConstraint(name = "uq_grade_session", columnNames = ["session_id"])])
class GradeResultEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "session_id", nullable = false) var sessionId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var examId: UUID = UUID.randomUUID(),
    var enrollmentId: UUID? = null,
    var courseId: UUID? = null,
    var lessonId: UUID? = null,
    @Column(nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var score: Double = 0.0,
    @Column(nullable = false) var maxScore: Double = 0.0,
    @Column(nullable = false) var percentage: Double = 0.0,
    @Column(nullable = false) var passingScore: Double = 0.0,
    @Column(nullable = false) var passed: Boolean = false,
    @Column(nullable = false, length = 20) var scoreStrategy: String = "HIGHEST",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: GradeStatus = GradeStatus.AUTO_GRADED,
    @Column(nullable = false, columnDefinition = "text") var detailsJson: String = "[]",
    @Column(columnDefinition = "text") var feedback: String? = null,
    var gradedBy: UUID? = null,
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var rowVersion: Long = 0,
)

interface GradeResultRepository : org.springframework.data.jpa.repository.JpaRepository<GradeResultEntity, UUID> {
    fun findBySessionId(sessionId: UUID): GradeResultEntity?
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<GradeResultEntity>
    fun findAllByStatusOrderByCreatedAtAsc(status: GradeStatus): List<GradeResultEntity>
    fun findAllByExamIdAndUserIdOrderByCreatedAtAsc(examId: UUID, userId: UUID): List<GradeResultEntity>
    fun findAllByExamIdAndEnrollmentIdOrderByCreatedAtAsc(examId: UUID, enrollmentId: UUID): List<GradeResultEntity>
    fun findAllByExamIdAndUserIdAndEnrollmentIdIsNullOrderByCreatedAtAsc(examId: UUID, userId: UUID): List<GradeResultEntity>

    @org.springframework.data.jpa.repository.Query(
        value = """
            select 1
            from (select pg_advisory_xact_lock(hashtextextended(cast(:lockKey as text), 0))) as grade_lock
        """,
        nativeQuery = true,
    )
    fun lockSession(@org.springframework.data.repository.query.Param("lockKey") lockKey: String): Int
}

enum class GradeRevisionType { MANUAL_GRADE, APPEAL_CORRECTION, ADMIN_CORRECTION }

enum class GradeAppealStatus { OPEN, UNDER_REVIEW, APPROVED, REJECTED, CANCELLED }

@Entity
@Table(name = "grade_revisions", indexes = [Index(name = "idx_grade_revision_grade", columnList = "grade_id,created_at")])
class GradeRevisionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "grade_id", nullable = false) var gradeId: UUID = UUID.randomUUID(),
    @Column(name = "previous_score", nullable = false) var previousScore: Double = 0.0,
    @Column(name = "new_score", nullable = false) var newScore: Double = 0.0,
    @Column(name = "previous_percentage", nullable = false) var previousPercentage: Double = 0.0,
    @Column(name = "new_percentage", nullable = false) var newPercentage: Double = 0.0,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) var type: GradeRevisionType = GradeRevisionType.MANUAL_GRADE,
    @Column(nullable = false, columnDefinition = "text") var reason: String = "",
    @Column(name = "changed_by", nullable = false) var changedBy: UUID = UUID.randomUUID(),
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
)

@Entity
@Table(
    name = "grade_appeals",
    uniqueConstraints = [UniqueConstraint(name = "uq_open_grade_appeal", columnNames = ["grade_id", "user_id", "active_key"])],
    indexes = [Index(name = "idx_grade_appeal_status", columnList = "status,created_at")],
)
class GradeAppealEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "grade_id", nullable = false) var gradeId: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "text") var reason: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: GradeAppealStatus = GradeAppealStatus.OPEN,
    @Column(name = "active_key", nullable = false, length = 20) var activeKey: String = "ACTIVE",
    @Column(columnDefinition = "text") var resolution: String? = null,
    @Column(name = "resolved_by") var resolvedBy: UUID? = null,
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Column(name = "resolved_at") var resolvedAt: Instant? = null,
    @Version var rowVersion: Long = 0,
)

interface GradeRevisionRepository : org.springframework.data.jpa.repository.JpaRepository<GradeRevisionEntity, UUID> {
    fun findAllByGradeIdOrderByCreatedAtDesc(gradeId: UUID): List<GradeRevisionEntity>
}

interface GradeAppealRepository : org.springframework.data.jpa.repository.JpaRepository<GradeAppealEntity, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<GradeAppealEntity>
    fun findAllByStatusInOrderByCreatedAtAsc(statuses: Collection<GradeAppealStatus>): List<GradeAppealEntity>
    fun findByGradeIdAndUserIdAndActiveKey(gradeId: UUID, userId: UUID, activeKey: String): GradeAppealEntity?
}
