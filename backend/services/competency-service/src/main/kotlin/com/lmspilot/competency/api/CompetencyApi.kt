package com.lmspilot.competency.api

import com.lmspilot.competency.domain.*
import com.lmspilot.contracts.EventTypes
import com.lmspilot.contracts.Permissions
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

data class CompetencyRequest(@field:NotBlank val code: String, @field:NotBlank val name: String, val description: String? = null, val category: String? = null, @field:Min(1) @field:Max(10) val maxLevel: Int = 5, val active: Boolean = true)
data class CompetencyView(val id: UUID, val code: String, val name: String, val description: String?, val category: String?, val maxLevel: Int, val status: CompetencyStatus)
data class RequirementRequest(val competencyId: UUID, @field:Min(0) @field:Max(10) val requiredLevel: Int, @field:Min(0) val weight: Double = 1.0)
data class ProfileRequest(@field:NotBlank val code: String, @field:NotBlank val name: String, val description: String? = null, val organizationUnitId: UUID? = null, val roleCode: String? = null, val active: Boolean = true, val requirements: List<RequirementRequest> = emptyList())
data class ProfileView(val id: UUID, val code: String, val name: String, val description: String?, val organizationUnitId: UUID?, val roleCode: String?, val active: Boolean, val requirements: List<RequirementView>)
data class RequirementView(val competencyId: UUID, val competencyCode: String, val competencyName: String, val requiredLevel: Int, val weight: Double)
data class AssignProfileRequest(val userIds: Set<UUID>, val profileId: UUID)
data class AssessmentRequest(val userId: UUID? = null, val competencyId: UUID, @field:Min(0) @field:Max(10) val level: Int, val source: AssessmentSource = AssessmentSource.SELF, val evidenceJson: String = "{}", val validUntil: Instant? = null)
data class AssessmentView(val id: UUID, val userId: UUID, val competencyId: UUID, val competencyCode: String, val competencyName: String, val level: Int, val source: AssessmentSource, val assessedBy: UUID, val assessedAt: Instant, val validUntil: Instant?)
data class CourseMapRequest(val courseId: UUID, val competencyId: UUID, @field:Min(1) @field:Max(10) val targetLevel: Int)
data class GapRow(val competencyId: UUID, val code: String, val name: String, val category: String?, val currentLevel: Int, val requiredLevel: Int, val gap: Int, val weight: Double, val recommendedCourseIds: List<UUID>)
data class CompetencyGapResponse(val userId: UUID, val profileIds: List<UUID>, val readinessPercent: Double, val gaps: List<GapRow>, val assessedAt: Instant)

@Service
class CompetencyService(
    private val competencies: CompetencyRepository,
    private val profiles: CompetencyProfileRepository,
    private val requirements: CompetencyProfileRequirementRepository,
    private val assignments: UserCompetencyProfileRepository,
    private val assessments: UserCompetencyAssessmentRepository,
    private val courseMaps: CourseCompetencyMapRepository,
    private val events: DomainEventPublisher,
) {
    @Transactional(readOnly = true)
    fun listCompetencies(includeInactive: Boolean): List<CompetencyView> = (if (includeInactive) competencies.findAll() else competencies.findAllByStatusOrderByCategoryAscNameAsc(CompetencyStatus.ACTIVE)).map { it.view() }

    @Transactional
    fun createCompetency(input: CompetencyRequest): CompetencyView {
        if (competencies.existsByCodeIgnoreCase(input.code.trim())) conflict("Mã năng lực đã tồn tại")
        return competencies.save(CompetencyEntity(code = input.code.trim().uppercase(), name = input.name.trim(), description = input.description?.trim(), category = input.category?.trim(), maxLevel = input.maxLevel, status = if (input.active) CompetencyStatus.ACTIVE else CompetencyStatus.INACTIVE)).view()
    }

    @Transactional
    fun updateCompetency(id: UUID, input: CompetencyRequest): CompetencyView {
        val entity = competencies.findById(id).orElseThrow { notFound("COMPETENCY_NOT_FOUND", "Không tìm thấy năng lực") }
        entity.name = input.name.trim(); entity.description = input.description?.trim(); entity.category = input.category?.trim(); entity.maxLevel = input.maxLevel; entity.status = if (input.active) CompetencyStatus.ACTIVE else CompetencyStatus.INACTIVE; entity.updatedAt = Instant.now()
        return entity.view()
    }

    @Transactional(readOnly = true)
    fun listProfiles(): List<ProfileView> = profiles.findAllByActiveTrueOrderByNameAsc().map(::profileView)

    @Transactional
    fun createProfile(input: ProfileRequest): ProfileView {
        if (profiles.existsByCodeIgnoreCase(input.code.trim())) conflict("Mã khung năng lực đã tồn tại")
        val profile = profiles.save(CompetencyProfileEntity(code = input.code.trim().uppercase(), name = input.name.trim(), description = input.description?.trim(), organizationUnitId = input.organizationUnitId, roleCode = input.roleCode?.trim()?.uppercase(), active = input.active))
        replaceRequirements(profile, input.requirements)
        return profileView(profile)
    }

    @Transactional
    fun updateProfile(id: UUID, input: ProfileRequest): ProfileView {
        val profile = profiles.findById(id).orElseThrow { notFound("PROFILE_NOT_FOUND", "Không tìm thấy khung năng lực") }
        profile.name = input.name.trim(); profile.description = input.description?.trim(); profile.organizationUnitId = input.organizationUnitId; profile.roleCode = input.roleCode?.trim()?.uppercase(); profile.active = input.active; profile.updatedAt = Instant.now()
        replaceRequirements(profile, input.requirements)
        return profileView(profile)
    }

    @Transactional
    fun assignProfile(input: AssignProfileRequest): Int {
        val profile = profiles.findById(input.profileId).orElseThrow { notFound("PROFILE_NOT_FOUND", "Không tìm thấy khung năng lực") }
        var created = 0
        input.userIds.forEach { userId ->
            if (!assignments.existsByUserIdAndProfileId(userId, profile.id)) {
                assignments.save(UserCompetencyProfileEntity(userId = userId, profile = profile, assignedBy = CurrentUser.id()))
                created++
            }
        }
        return created
    }

    @Transactional
    fun unassignProfile(userId: UUID, profileId: UUID): Long = assignments.deleteByUserIdAndProfileId(userId, profileId)

    @Transactional
    fun assess(input: AssessmentRequest): AssessmentView {
        val actor = CurrentUser.id()
        val target = input.userId ?: actor
        if (target != actor && Permissions.COMPETENCIES_ASSESS !in CurrentUser.authorities()) throw ApiException(HttpStatus.FORBIDDEN, "COMPETENCY_SCOPE_DENIED", "Không có quyền đánh giá người dùng khác")
        if (input.source == AssessmentSource.SELF && target != actor) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_ASSESSMENT_SOURCE", "Đánh giá SELF chỉ áp dụng cho chính người đang đăng nhập")
        val competency = competencies.findById(input.competencyId).orElseThrow { notFound("COMPETENCY_NOT_FOUND", "Không tìm thấy năng lực") }
        if (input.level > competency.maxLevel) throw ApiException(HttpStatus.BAD_REQUEST, "LEVEL_EXCEEDS_MAX", "Mức năng lực vượt quá giới hạn ${competency.maxLevel}")
        val entity = assessments.save(UserCompetencyAssessmentEntity(userId = target, competency = competency, level = input.level, source = input.source, assessedBy = actor, evidenceJson = input.evidenceJson, validUntil = input.validUntil))
        events.publish(EventTypes.COMPETENCY_ASSESSED, "competency-service", entity.id.toString(), mapOf("assessmentId" to entity.id, "userId" to target, "competencyId" to competency.id, "level" to input.level, "source" to input.source.name))
        return entity.view()
    }

    @Transactional
    fun mapCourse(input: CourseMapRequest) {
        val competency = competencies.findById(input.competencyId).orElseThrow { notFound("COMPETENCY_NOT_FOUND", "Không tìm thấy năng lực") }
        if (input.targetLevel > competency.maxLevel) throw ApiException(HttpStatus.BAD_REQUEST, "LEVEL_EXCEEDS_MAX", "Mức mục tiêu vượt giới hạn năng lực")
        val entity = courseMaps.findByCourseIdAndCompetencyId(input.courseId, competency.id) ?: CourseCompetencyMapEntity(courseId = input.courseId, competency = competency)
        entity.targetLevel = input.targetLevel
        courseMaps.save(entity)
    }

    @Transactional(readOnly = true)
    fun gap(userId: UUID): CompetencyGapResponse {
        val profileAssignments = assignments.findAllByUserId(userId)
        val profileIds = profileAssignments.map { it.profile.id }
        val required = profileIds.flatMap(requirements::findAllByProfileId)
            .groupBy { it.competency.id }
            .mapValues { (_, rows) -> rows.maxBy { it.requiredLevel } }
        val now = Instant.now()
        val latest = assessments.findAllByUserIdOrderByAssessedAtDesc(userId)
            .filter { it.validUntil == null || it.validUntil!!.isAfter(now) }
            .distinctBy { it.competency.id }
            .associateBy { it.competency.id }
        val gaps = required.values.map { requirement ->
            val current = latest[requirement.competency.id]?.level ?: 0
            val gap = (requirement.requiredLevel - current).coerceAtLeast(0)
            GapRow(requirement.competency.id, requirement.competency.code, requirement.competency.name, requirement.competency.category, current, requirement.requiredLevel, gap, requirement.weight, courseMaps.findAllByCompetencyId(requirement.competency.id).filter { it.targetLevel >= requirement.requiredLevel }.map { it.courseId }.distinct())
        }.sortedWith(compareByDescending<GapRow> { it.gap * it.weight }.thenBy { it.name })
        val totalWeight = gaps.sumOf { it.requiredLevel * it.weight }
        val achieved = gaps.sumOf { minOf(it.currentLevel, it.requiredLevel) * it.weight }
        val readiness = if (totalWeight <= 0.0) 100.0 else (achieved * 100.0 / totalWeight).coerceIn(0.0, 100.0)
        return CompetencyGapResponse(userId, profileIds, readiness, gaps, now)
    }

    @Transactional(readOnly = true)
    fun assessments(userId: UUID): List<AssessmentView> = assessments.findAllByUserIdOrderByAssessedAtDesc(userId).map { it.view() }

    private fun replaceRequirements(profile: CompetencyProfileEntity, rows: List<RequirementRequest>) {
        val duplicates = rows.groupBy { it.competencyId }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_REQUIREMENT", "Một năng lực chỉ được xuất hiện một lần trong khung")
        requirements.deleteAllByProfileId(profile.id)
        rows.forEach { row ->
            val competency = competencies.findById(row.competencyId).orElseThrow { notFound("COMPETENCY_NOT_FOUND", "Không tìm thấy năng lực ${row.competencyId}") }
            if (row.requiredLevel > competency.maxLevel) throw ApiException(HttpStatus.BAD_REQUEST, "LEVEL_EXCEEDS_MAX", "Mức yêu cầu vượt giới hạn của ${competency.code}")
            requirements.save(CompetencyProfileRequirementEntity(profile = profile, competency = competency, requiredLevel = row.requiredLevel, weight = row.weight))
        }
    }

    private fun profileView(profile: CompetencyProfileEntity) = ProfileView(profile.id, profile.code, profile.name, profile.description, profile.organizationUnitId, profile.roleCode, profile.active, requirements.findAllByProfileId(profile.id).map { RequirementView(it.competency.id, it.competency.code, it.competency.name, it.requiredLevel, it.weight) })
    private fun conflict(message: String): Nothing = throw ApiException(HttpStatus.CONFLICT, "COMPETENCY_CONFLICT", message)
    private fun notFound(code: String, message: String) = ApiException(HttpStatus.NOT_FOUND, code, message)
}

private fun CompetencyEntity.view() = CompetencyView(id, code, name, description, category, maxLevel, status)
private fun UserCompetencyAssessmentEntity.view() = AssessmentView(id, userId, competency.id, competency.code, competency.name, level, source, assessedBy, assessedAt, validUntil)

@RestController
@RequestMapping("/api/v1/competencies")
class CompetencyController(private val service: CompetencyService) {
    @GetMapping @PreAuthorize("hasAnyAuthority('${Permissions.COMPETENCIES_READ_SELF}','${Permissions.COMPETENCIES_READ_SCOPE}','${Permissions.COMPETENCIES_MANAGE}')") fun list(@RequestParam(defaultValue = "false") includeInactive: Boolean) = service.listCompetencies(includeInactive)
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('${Permissions.COMPETENCIES_MANAGE}')") fun create(@Valid @RequestBody input: CompetencyRequest) = service.createCompetency(input)
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('${Permissions.COMPETENCIES_MANAGE}')") fun update(@PathVariable id: UUID, @Valid @RequestBody input: CompetencyRequest) = service.updateCompetency(id, input)
    @GetMapping("/profiles") @PreAuthorize("hasAnyAuthority('${Permissions.COMPETENCIES_READ_SCOPE}','${Permissions.COMPETENCIES_MANAGE}')") fun profiles() = service.listProfiles()
    @PostMapping("/profiles") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('${Permissions.COMPETENCIES_MANAGE}')") fun createProfile(@Valid @RequestBody input: ProfileRequest) = service.createProfile(input)
    @PutMapping("/profiles/{id}") @PreAuthorize("hasAuthority('${Permissions.COMPETENCIES_MANAGE}')") fun updateProfile(@PathVariable id: UUID, @Valid @RequestBody input: ProfileRequest) = service.updateProfile(id, input)
    @PostMapping("/profile-assignments") @PreAuthorize("hasAuthority('${Permissions.COMPETENCIES_MANAGE}')") fun assign(@Valid @RequestBody input: AssignProfileRequest) = mapOf("created" to service.assignProfile(input))
    @DeleteMapping("/profile-assignments") @PreAuthorize("hasAuthority('${Permissions.COMPETENCIES_MANAGE}')") fun unassign(@RequestParam userId: UUID, @RequestParam profileId: UUID) = mapOf("deleted" to service.unassignProfile(userId, profileId))
    @PostMapping("/assessments") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('${Permissions.COMPETENCIES_READ_SELF}','${Permissions.COMPETENCIES_ASSESS}')") fun assess(@Valid @RequestBody input: AssessmentRequest) = service.assess(input)
    @GetMapping("/me/gaps") @PreAuthorize("hasAuthority('${Permissions.COMPETENCIES_READ_SELF}')") fun myGaps() = service.gap(CurrentUser.id())
    @GetMapping("/users/{userId}/gaps") @PreAuthorize("hasAuthority('${Permissions.COMPETENCIES_READ_SCOPE}')") fun gaps(@PathVariable userId: UUID) = service.gap(userId)
    @GetMapping("/me/assessments") @PreAuthorize("hasAuthority('${Permissions.COMPETENCIES_READ_SELF}')") fun myAssessments() = service.assessments(CurrentUser.id())
    @GetMapping("/users/{userId}/assessments") @PreAuthorize("hasAuthority('${Permissions.COMPETENCIES_READ_SCOPE}')") fun assessments(@PathVariable userId: UUID) = service.assessments(userId)
    @PostMapping("/course-maps") @PreAuthorize("hasAuthority('${Permissions.COMPETENCIES_MANAGE}')") fun mapCourse(@Valid @RequestBody input: CourseMapRequest) = service.mapCourse(input)
}
