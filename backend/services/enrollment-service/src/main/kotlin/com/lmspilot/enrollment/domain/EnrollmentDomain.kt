package com.lmspilot.enrollment.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class TrainingClassStatus { DRAFT, OPEN, CLOSED, ARCHIVED }
enum class EnrollmentStatus { ENROLLED, IN_PROGRESS, COMPLETED, OVERDUE, CANCELLED }

@Entity
@Table(name = "training_classes")
class TrainingClassEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 80) var code: String = "",
    @Column(nullable = false, length = 220) var name: String = "",
    @Column(nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var courseVersion: Int = 1,
    var startsAt: Instant? = null,
    var endsAt: Instant? = null,
    var dueAt: Instant? = null,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "class_instructors", joinColumns = [JoinColumn(name = "class_id")])
    @Column(name = "instructor_id", nullable = false)
    var instructorIds: MutableSet<UUID> = mutableSetOf(),
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: TrainingClassStatus = TrainingClassStatus.OPEN,
    @Column(nullable = false) var createdBy: UUID = UUID.randomUUID(),
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

@Entity
@Table(name = "enrollments", uniqueConstraints = [
    UniqueConstraint(name = "uq_enrollment_class_user", columnNames = ["class_id", "user_id"]),
    UniqueConstraint(name = "uq_enrollment_idempotency", columnNames = ["idempotency_key"]),
])
class EnrollmentEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "class_id", nullable = false) var classId: UUID = UUID.randomUUID(),
    @Column(name = "course_id", nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
    var dueAt: Instant? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: EnrollmentStatus = EnrollmentStatus.ENROLLED,
    @Column(name = "idempotency_key", nullable = false, length = 120) var idempotencyKey: String = "",
    @Column(nullable = false) var enrolledAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

interface TrainingClassRepository : org.springframework.data.jpa.repository.JpaRepository<TrainingClassEntity, UUID> {
    fun existsByCodeIgnoreCase(code: String): Boolean

    @org.springframework.data.jpa.repository.Query("select distinct c from TrainingClassEntity c join c.instructorIds i where i = :instructorId order by c.createdAt desc")
    fun findAllAssignedTo(instructorId: UUID): List<TrainingClassEntity>
}
interface EnrollmentRepository : org.springframework.data.jpa.repository.JpaRepository<EnrollmentEntity, UUID> {
    fun findByClassIdAndUserId(classId: UUID, userId: UUID): EnrollmentEntity?
    fun findByIdempotencyKey(idempotencyKey: String): EnrollmentEntity?
    fun findAllByUserIdOrderByEnrolledAtDesc(userId: UUID): List<EnrollmentEntity>
    fun findAllByClassIdOrderByEnrolledAtDesc(classId: UUID): List<EnrollmentEntity>
}
