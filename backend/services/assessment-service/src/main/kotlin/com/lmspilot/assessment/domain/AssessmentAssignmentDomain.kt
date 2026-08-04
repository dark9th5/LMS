package com.lmspilot.assessment.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class AssessmentAssigneeType { USER, GROUP, DEPARTMENT, BRANCH }
enum class AssessmentAssignmentStatus { ACTIVE, REVOKED }

@Entity
@Table(
    name = "assessment_assignments",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_assessment_assignment_target",
            columnNames = ["assessment_id", "assignee_type", "assignee_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_assessment_assignment_assessment", columnList = "assessment_id,status,assigned_at"),
        Index(name = "idx_assessment_assignment_target", columnList = "assignee_type,assignee_id,status,available_from,due_at"),
    ],
)
class AssessmentAssignmentEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "assessment_id", nullable = false) var assessmentId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(name = "assignee_type", nullable = false, length = 30)
    var assigneeType: AssessmentAssigneeType = AssessmentAssigneeType.USER,
    @Column(name = "assignee_id", nullable = false) var assigneeId: UUID = UUID.randomUUID(),
    @Column(name = "available_from") var availableFrom: Instant? = null,
    @Column(name = "due_at") var dueAt: Instant? = null,
    @Column(nullable = false) var required: Boolean = true,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AssessmentAssignmentStatus = AssessmentAssignmentStatus.ACTIVE,
    @Column(name = "assigned_by", nullable = false) var assignedBy: UUID = UUID.randomUUID(),
    @Column(name = "assigned_at", nullable = false) var assignedAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
)

interface AssessmentAssignmentRepository : org.springframework.data.jpa.repository.JpaRepository<AssessmentAssignmentEntity, UUID> {
    fun findAllByAssessmentIdOrderByAssignedAtDesc(assessmentId: UUID): List<AssessmentAssignmentEntity>
    fun findByAssessmentIdAndAssigneeTypeAndAssigneeId(
        assessmentId: UUID,
        assigneeType: AssessmentAssigneeType,
        assigneeId: UUID,
    ): AssessmentAssignmentEntity?
    fun findAllByAssigneeTypeAndAssigneeIdAndStatusOrderByAssignedAtDesc(
        assigneeType: AssessmentAssigneeType,
        assigneeId: UUID,
        status: AssessmentAssignmentStatus,
    ): List<AssessmentAssignmentEntity>
    fun existsByAssessmentIdAndStatus(assessmentId: UUID, status: AssessmentAssignmentStatus): Boolean
}
