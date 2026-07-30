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
    var courseId: UUID? = null,
    @Column(nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var score: Double = 0.0,
    @Column(nullable = false) var maxScore: Double = 0.0,
    @Column(nullable = false) var percentage: Double = 0.0,
    @Column(nullable = false) var passingScore: Double = 0.0,
    @Column(nullable = false) var passed: Boolean = false,
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
}
