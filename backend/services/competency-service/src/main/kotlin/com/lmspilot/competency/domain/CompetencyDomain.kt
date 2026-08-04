package com.lmspilot.competency.domain

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

enum class CompetencyStatus { ACTIVE, INACTIVE }
enum class AssessmentSource { SELF, MANAGER, EXAM, IMPORT }

@Entity
@Table(name = "competencies", uniqueConstraints = [UniqueConstraint(name = "uq_competency_code", columnNames = ["code"])])
class CompetencyEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 80) var code: String = "",
    @Column(nullable = false, length = 220) var name: String = "",
    @Column(columnDefinition = "text") var description: String? = null,
    @Column(length = 120) var category: String? = null,
    @Column(nullable = false) var maxLevel: Int = 5,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: CompetencyStatus = CompetencyStatus.ACTIVE,
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

@Entity
@Table(name = "competency_profiles", uniqueConstraints = [UniqueConstraint(name = "uq_competency_profile_code", columnNames = ["code"])])
class CompetencyProfileEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 80) var code: String = "",
    @Column(nullable = false, length = 220) var name: String = "",
    @Column(columnDefinition = "text") var description: String? = null,
    var organizationUnitId: UUID? = null,
    @Column(length = 80) var roleCode: String? = null,
    @Column(nullable = false) var active: Boolean = true,
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "competency_profile_requirements", uniqueConstraints = [UniqueConstraint(name = "uq_profile_competency", columnNames = ["profile_id", "competency_id"])])
class CompetencyProfileRequirementEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "profile_id", nullable = false) var profile: CompetencyProfileEntity = CompetencyProfileEntity(),
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "competency_id", nullable = false) var competency: CompetencyEntity = CompetencyEntity(),
    @Column(nullable = false) var requiredLevel: Int = 1,
    @Column(nullable = false) var weight: Double = 1.0,
)

@Entity
@Table(name = "user_competency_profiles", uniqueConstraints = [UniqueConstraint(name = "uq_user_competency_profile", columnNames = ["user_id", "profile_id"])])
class UserCompetencyProfileEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false) var userId: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "profile_id", nullable = false) var profile: CompetencyProfileEntity = CompetencyProfileEntity(),
    @Column(nullable = false) var assignedBy: UUID = UUID.randomUUID(),
    @Column(nullable = false) var assignedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "user_competency_assessments", indexes = [Index(name = "idx_user_competency_assessment", columnList = "user_id,competency_id,assessed_at")])
class UserCompetencyAssessmentEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false) var userId: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "competency_id", nullable = false) var competency: CompetencyEntity = CompetencyEntity(),
    @Column(nullable = false) var level: Int = 0,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var source: AssessmentSource = AssessmentSource.SELF,
    @Column(nullable = false) var assessedBy: UUID = UUID.randomUUID(),
    @Column(columnDefinition = "text") var evidenceJson: String = "{}",
    @Column(nullable = false) var assessedAt: Instant = Instant.now(),
    var validUntil: Instant? = null,
)

@Entity
@Table(name = "course_competency_maps", uniqueConstraints = [UniqueConstraint(name = "uq_course_competency", columnNames = ["course_id", "competency_id"])])
class CourseCompetencyMapEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false) var courseId: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "competency_id", nullable = false) var competency: CompetencyEntity = CompetencyEntity(),
    @Column(nullable = false) var targetLevel: Int = 1,
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
)

interface CompetencyRepository : JpaRepository<CompetencyEntity, UUID> {
    fun existsByCodeIgnoreCase(code: String): Boolean
    fun findAllByStatusOrderByCategoryAscNameAsc(status: CompetencyStatus): List<CompetencyEntity>
}
interface CompetencyProfileRepository : JpaRepository<CompetencyProfileEntity, UUID> {
    fun existsByCodeIgnoreCase(code: String): Boolean
    fun findAllByActiveTrueOrderByNameAsc(): List<CompetencyProfileEntity>
}
interface CompetencyProfileRequirementRepository : JpaRepository<CompetencyProfileRequirementEntity, UUID> {
    fun findAllByProfileId(profileId: UUID): List<CompetencyProfileRequirementEntity>
    fun deleteAllByProfileId(profileId: UUID)
}
interface UserCompetencyProfileRepository : JpaRepository<UserCompetencyProfileEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<UserCompetencyProfileEntity>
    fun existsByUserIdAndProfileId(userId: UUID, profileId: UUID): Boolean
    fun deleteByUserIdAndProfileId(userId: UUID, profileId: UUID): Long
}
interface UserCompetencyAssessmentRepository : JpaRepository<UserCompetencyAssessmentEntity, UUID> {
    fun findAllByUserIdOrderByAssessedAtDesc(userId: UUID): List<UserCompetencyAssessmentEntity>
}
interface CourseCompetencyMapRepository : JpaRepository<CourseCompetencyMapEntity, UUID> {
    fun findAllByCompetencyId(competencyId: UUID): List<CourseCompetencyMapEntity>
    fun findByCourseIdAndCompetencyId(courseId: UUID, competencyId: UUID): CourseCompetencyMapEntity?
}
