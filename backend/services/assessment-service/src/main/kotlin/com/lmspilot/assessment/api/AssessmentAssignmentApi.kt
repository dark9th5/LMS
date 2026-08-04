package com.lmspilot.assessment.api

import com.lmspilot.assessment.cls.AssessmentContextType
import com.lmspilot.assessment.domain.*
import com.lmspilot.contracts.Permissions
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.ScopedAuthorizationClient
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.UUID

data class AssessmentAssignmentRequest(
    val assessmentId: UUID,
    val assigneeType: AssessmentAssigneeType,
    val assigneeId: UUID,
    val availableFrom: Instant? = null,
    val dueAt: Instant? = null,
    val required: Boolean = true,
) {
    @AssertTrue(message = "Hạn làm bài phải sau thời gian mở")
    fun validWindow(): Boolean = dueAt == null || availableFrom == null || dueAt.isAfter(availableFrom)
}

data class AssessmentAssignmentResponse(
    val id: UUID,
    val assessmentId: UUID,
    val assigneeType: AssessmentAssigneeType,
    val assigneeId: UUID,
    val availableFrom: Instant?,
    val dueAt: Instant?,
    val required: Boolean,
    val status: AssessmentAssignmentStatus,
    val assignedBy: UUID,
    val assignedAt: Instant,
)

@Service
class AssessmentOrganizationScopeClient(
    builder: RestClient.Builder,
    @Value("\${organization-service.url:http://localhost:8082}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val token: String,
) {
    private val client = builder.baseUrl(baseUrl).build()

    fun users(unitId: UUID): Set<UUID> = client.get()
        .uri("/internal/v1/organization/units/{id}/users", unitId)
        .header("X-Service-Token", token)
        .retrieve()
        .body(Array<String>::class.java)
        ?.map(UUID::fromString)
        ?.toSet()
        .orEmpty()

    fun userUnits(userId: UUID): Set<UUID> = client.get()
        .uri("/internal/v1/organization/users/{id}/unit-ids", userId)
        .header("X-Service-Token", token)
        .retrieve()
        .body(Array<String>::class.java)
        ?.map(UUID::fromString)
        ?.toSet()
        .orEmpty()
}

@Service
class AssessmentAudienceService(
    private val assignments: AssessmentAssignmentRepository,
    private val exams: ExamRepository,
    private val contexts: AssessmentContextRepository,
    private val organization: AssessmentOrganizationScopeClient,
    private val scopedAuthorization: ScopedAuthorizationClient,
) {
    @Transactional
    fun assign(input: AssessmentAssignmentRequest): AssessmentAssignmentResponse {
        val exam = requireAssignableExam(input.assessmentId)
        requireManage(exam, Permissions.EXAMS_ASSIGN)
        if (input.assigneeType != AssessmentAssigneeType.USER) {
            val targetUsers = runCatching { organization.users(input.assigneeId) }.getOrElse {
                throw ApiException(HttpStatus.BAD_GATEWAY, "ORGANIZATION_UNAVAILABLE", "Không thể xác minh phạm vi tổ chức")
            }
            if (targetUsers.isEmpty()) {
                throw ApiException(HttpStatus.BAD_REQUEST, "ASSIGNMENT_TARGET_EMPTY", "Đơn vị được chọn chưa có người dùng hoạt động")
            }
        }
        val entity = assignments.findByAssessmentIdAndAssigneeTypeAndAssigneeId(
            input.assessmentId,
            input.assigneeType,
            input.assigneeId,
        ) ?: AssessmentAssignmentEntity(
            assessmentId = input.assessmentId,
            assigneeType = input.assigneeType,
            assigneeId = input.assigneeId,
            assignedBy = CurrentUser.id(),
        )
        entity.availableFrom = input.availableFrom
        entity.dueAt = input.dueAt
        entity.required = input.required
        entity.status = AssessmentAssignmentStatus.ACTIVE
        entity.updatedAt = Instant.now()
        return assignments.save(entity).response()
    }

    @Transactional(readOnly = true)
    fun list(assessmentId: UUID): List<AssessmentAssignmentResponse> {
        val exam = requireAssignableExam(assessmentId)
        requireManage(exam, Permissions.EXAMS_ASSIGN)
        return assignments.findAllByAssessmentIdOrderByAssignedAtDesc(assessmentId).map { it.response() }
    }

    @Transactional
    fun revoke(id: UUID): AssessmentAssignmentResponse {
        val entity = assignments.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "ASSESSMENT_ASSIGNMENT_NOT_FOUND", "Không tìm thấy đối tượng được giao")
        }
        val exam = exams.findById(entity.assessmentId).orElseThrow { examNotFound() }
        requireManage(exam, Permissions.EXAMS_ASSIGN)
        entity.status = AssessmentAssignmentStatus.REVOKED
        entity.updatedAt = Instant.now()
        return entity.response()
    }

    /**
     * Backward-compatible audience policy: an active standalone assessment with
     * no assignment rows is open to every user holding the participation right.
     * Once at least one active row exists, the user must match a direct or
     * organization-unit assignment and its availability window.
     */
    @Transactional(readOnly = true)
    fun isEligible(assessmentId: UUID, userId: UUID, at: Instant = Instant.now()): Boolean {
        if (!assignments.existsByAssessmentIdAndStatus(assessmentId, AssessmentAssignmentStatus.ACTIVE)) return true
        return matchingAssignments(userId).any { row ->
            row.assessmentId == assessmentId && inWindow(row, at)
        }
    }

    @Transactional(readOnly = true)
    fun assignedAssessmentIds(userId: UUID, at: Instant = Instant.now()): Set<UUID> =
        matchingAssignments(userId).filter { inWindow(it, at) }.map { it.assessmentId }.toSet()

    private fun matchingAssignments(userId: UUID): List<AssessmentAssignmentEntity> {
        val direct = assignments.findAllByAssigneeTypeAndAssigneeIdAndStatusOrderByAssignedAtDesc(
            AssessmentAssigneeType.USER,
            userId,
            AssessmentAssignmentStatus.ACTIVE,
        )
        val unitIds = runCatching { organization.userUnits(userId) }.getOrDefault(emptySet())
        val organizationRows = unitIds.flatMap { unitId ->
            listOf(AssessmentAssigneeType.GROUP, AssessmentAssigneeType.DEPARTMENT, AssessmentAssigneeType.BRANCH)
                .flatMap { type ->
                    assignments.findAllByAssigneeTypeAndAssigneeIdAndStatusOrderByAssignedAtDesc(
                        type,
                        unitId,
                        AssessmentAssignmentStatus.ACTIVE,
                    )
                }
        }
        return (direct + organizationRows).distinctBy { it.id }
    }

    private fun inWindow(entity: AssessmentAssignmentEntity, at: Instant): Boolean =
        (entity.availableFrom == null || !entity.availableFrom!!.isAfter(at)) &&
            (entity.dueAt == null || entity.dueAt!!.isAfter(at))

    private fun requireAssignableExam(id: UUID): ExamEntity {
        val exam = exams.findById(id).orElseThrow { examNotFound() }
        val context = contexts.findById(id).orElseThrow {
            ApiException(HttpStatus.CONFLICT, "ASSESSMENT_CONTEXT_MISSING", "Bài thi chưa có ngữ cảnh")
        }
        if (context.contextType !in setOf(AssessmentContextType.STANDALONE_EXAM, AssessmentContextType.COMPETITION)) {
            throw ApiException(HttpStatus.CONFLICT, "COURSE_ASSESSMENT_AUDIENCE", "Bài kiểm tra trong khóa học được giao qua lớp hoặc khóa học")
        }
        return exam
    }

    private fun requireManage(exam: ExamEntity, permission: String) {
        val allowed = CurrentUser.isSystemAdmin() || "ADMIN" in CurrentUser.roles() || exam.ownerId == CurrentUser.id() ||
            scopedAuthorization.allowed(permission, "EXAM", exam.id) ||
            scopedAuthorization.allowed(Permissions.EXAMS_MANAGE, "EXAM", exam.id) ||
            scopedAuthorization.allowed(Permissions.COMPETITIONS_MANAGE, "EXAM", exam.id)
        if (!allowed) {
            throw ApiException(HttpStatus.FORBIDDEN, "ASSESSMENT_ASSIGNMENT_OUT_OF_SCOPE", "Bài thi ngoài phạm vi được giao")
        }
    }

    private fun examNotFound() = ApiException(HttpStatus.NOT_FOUND, "EXAM_NOT_FOUND", "Không tìm thấy bài thi")
}

private fun AssessmentAssignmentEntity.response() = AssessmentAssignmentResponse(
    id,
    assessmentId,
    assigneeType,
    assigneeId,
    availableFrom,
    dueAt,
    required,
    status,
    assignedBy,
    assignedAt,
)

@RestController
@RequestMapping("/api/v1/assessment-assignments")
class AssessmentAssignmentController(private val service: AssessmentAudienceService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.EXAMS_ASSIGN}')")
    fun assign(@Valid @RequestBody input: AssessmentAssignmentRequest) = service.assign(input)

    @GetMapping
    @PreAuthorize("hasAnyAuthority('${Permissions.EXAMS_ASSIGN}','${Permissions.EXAMS_MANAGE}','${Permissions.COMPETITIONS_MANAGE}')")
    fun list(@RequestParam assessmentId: UUID) = service.list(assessmentId)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('${Permissions.EXAMS_ASSIGN}')")
    fun revoke(@PathVariable id: UUID) = service.revoke(id)
}
