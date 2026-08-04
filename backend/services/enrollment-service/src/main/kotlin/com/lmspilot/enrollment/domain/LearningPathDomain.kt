package com.lmspilot.enrollment.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class LearningPathStatus { DRAFT, PUBLISHED, ARCHIVED }
enum class LearningPathUnlockMode { IMMEDIATE, AFTER_PREVIOUS }
enum class LearningPathAssignmentStatus { ACTIVE, CANCELLED }
enum class UserLearningPathStatus { ASSIGNED, IN_PROGRESS, COMPLETED, OVERDUE, CANCELLED }

@Entity
@Table(name = "learning_paths", indexes = [Index(name = "idx_learning_path_status", columnList = "status,updated_at")])
class LearningPathEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 80) var code: String = "",
    @Column(nullable = false, length = 220) var name: String = "",
    @Column(columnDefinition = "text") var description: String? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: LearningPathStatus = LearningPathStatus.DRAFT,
    @Column(nullable = false) var ownerId: UUID = UUID.randomUUID(),
    var publishedAt: Instant? = null,
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

@Entity
@Table(
    name = "learning_path_items",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_learning_path_item_order", columnNames = ["path_id", "sort_order"]),
        UniqueConstraint(name = "uq_learning_path_item_class", columnNames = ["path_id", "class_id"]),
    ],
    indexes = [Index(name = "idx_learning_path_item_path", columnList = "path_id,sort_order")],
)
class LearningPathItemEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "path_id", nullable = false) var path: LearningPathEntity = LearningPathEntity(),
    @Column(nullable = false) var classId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var courseVersion: Int = 1,
    @Column(nullable = false) var sortOrder: Int = 0,
    @Column(nullable = false) var required: Boolean = true,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var unlockMode: LearningPathUnlockMode = LearningPathUnlockMode.AFTER_PREVIOUS,
    @Column(nullable = false) var dueOffsetDays: Int = 0,
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
)

@Entity
@Table(
    name = "learning_path_assignments",
    indexes = [
        Index(name = "idx_learning_path_assignment_path", columnList = "path_id,assigned_at"),
        Index(name = "idx_learning_path_assignment_target", columnList = "assignee_type,assignee_id,status"),
    ],
)
class LearningPathAssignmentEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "path_id", nullable = false) var path: LearningPathEntity = LearningPathEntity(),
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var assigneeType: AssignmentTargetType = AssignmentTargetType.USER,
    @Column(nullable = false) var assigneeId: UUID = UUID.randomUUID(),
    var dueAt: Instant? = null,
    @Column(nullable = false) var assignedBy: UUID = UUID.randomUUID(),
    @Column(nullable = false) var assignedAt: Instant = Instant.now(),
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: LearningPathAssignmentStatus = LearningPathAssignmentStatus.ACTIVE,
)

@Entity
@Table(
    name = "user_learning_paths",
    uniqueConstraints = [UniqueConstraint(name = "uq_user_learning_path", columnNames = ["path_id", "user_id"])],
    indexes = [Index(name = "idx_user_learning_path_user", columnList = "user_id,status,updated_at")],
)
class UserLearningPathEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "path_id", nullable = false) var path: LearningPathEntity = LearningPathEntity(),
    @Column(nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var sourceAssignmentId: UUID = UUID.randomUUID(),
    var dueAt: Instant? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: UserLearningPathStatus = UserLearningPathStatus.ASSIGNED,
    @Column(nullable = false) var assignedAt: Instant = Instant.now(),
    var startedAt: Instant? = null,
    var completedAt: Instant? = null,
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

interface LearningPathRepository : org.springframework.data.jpa.repository.JpaRepository<LearningPathEntity, UUID> {
    fun existsByCodeIgnoreCase(code: String): Boolean
    fun findAllByStatusNotOrderByUpdatedAtDesc(status: LearningPathStatus): List<LearningPathEntity>
}

interface LearningPathItemRepository : org.springframework.data.jpa.repository.JpaRepository<LearningPathItemEntity, UUID> {
    fun findAllByPathIdOrderBySortOrderAsc(pathId: UUID): List<LearningPathItemEntity>
    fun deleteAllByPathId(pathId: UUID): Long
}

interface LearningPathAssignmentRepository : org.springframework.data.jpa.repository.JpaRepository<LearningPathAssignmentEntity, UUID> {
    fun findAllByPathIdOrderByAssignedAtDesc(pathId: UUID): List<LearningPathAssignmentEntity>
    fun findAllByAssigneeTypeAndAssigneeIdAndStatusOrderByAssignedAtDesc(type: AssignmentTargetType, assigneeId: UUID, status: LearningPathAssignmentStatus): List<LearningPathAssignmentEntity>
}

interface UserLearningPathRepository : org.springframework.data.jpa.repository.JpaRepository<UserLearningPathEntity, UUID> {
    fun findByPathIdAndUserId(pathId: UUID, userId: UUID): UserLearningPathEntity?
    fun findAllByUserIdAndStatusNotOrderByUpdatedAtDesc(userId: UUID, status: UserLearningPathStatus): List<UserLearningPathEntity>
    fun findAllByPathIdOrderByAssignedAtDesc(pathId: UUID): List<UserLearningPathEntity>
}
