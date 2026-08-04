package com.lmspilot.enrollment.api

import com.lmspilot.contracts.EnrolledPayload
import com.lmspilot.contracts.EventTypes
import com.lmspilot.contracts.Permissions
import com.lmspilot.enrollment.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.ScopedAuthorizationClient
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.UUID

data class CreateCourseAssignmentRequest(
    val classId: UUID,
    val assigneeType: AssignmentTargetType,
    val assigneeId: UUID,
    val availableFrom: Instant? = null,
    val dueAt: Instant? = null,
    @field:Min(0) @field:Max(10080) val gracePeriodMinutes: Int = 0,
    val required: Boolean = true,
)

data class CourseAssignmentResponse(
    val id: UUID, val classId: UUID?, val courseId: UUID, val assigneeType: AssignmentTargetType,
    val assigneeId: UUID, val assignedVersion: Int, val assignedAt: Instant, val availableFrom: Instant?,
    val dueAt: Instant?, val gracePeriodMinutes: Int, val required: Boolean, val status: CourseAssignmentStatus,
    val enrolledUsers: Int = 0,
)

data class CreateLiveSessionRequest(
    val classId: UUID,
    @field:NotBlank @field:Size(max = 220) val title: String,
    @field:Size(max = 120) val provider: String = "EXTERNAL",
    @field:NotBlank @field:Size(max = 2000) val joinUrl: String,
    @field:Size(max = 2000) val hostUrl: String? = null,
    val startsAt: Instant,
    val endsAt: Instant,
)

data class LiveSessionResponse(
    val id: UUID, val classId: UUID, val courseId: UUID, val title: String, val provider: String,
    val joinUrl: String, val hostUrl: String?, val startsAt: Instant, val endsAt: Instant,
    val status: LiveSessionStatus,
)

@Service
class OrganizationUserScopeClient(
    builder: RestClient.Builder,
    @Value("\${organization-service.url:http://localhost:8082}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val token: String,
) {
    private val client = builder.baseUrl(baseUrl).build()
    fun users(unitId: UUID): Set<UUID> = client.get().uri("/internal/v1/organization/units/{id}/users", unitId)
        .header("X-Service-Token", token).retrieve().body(Array<String>::class.java)?.map(UUID::fromString)?.toSet().orEmpty()
    fun userUnits(userId: UUID): Set<UUID> = client.get().uri("/internal/v1/organization/users/{id}/unit-ids", userId)
        .header("X-Service-Token", token).retrieve().body(Array<String>::class.java)?.map(UUID::fromString)?.toSet().orEmpty()
}

@Service
class AssignmentAndLiveService(
    private val assignments: CourseAssignmentRepository,
    private val liveSessions: LiveSessionRepository,
    private val classes: TrainingClassRepository,
    private val enrollments: EnrollmentRepository,
    private val organization: OrganizationUserScopeClient,
    private val events: DomainEventPublisher,
    private val scopedAuthorization: ScopedAuthorizationClient,
) {
    @Transactional
    fun assign(input: CreateCourseAssignmentRequest): CourseAssignmentResponse {
        val trainingClass = classes.findById(input.classId).orElseThrow { classNotFound() }
        requireClassManager(trainingClass)
        if (input.dueAt != null && input.availableFrom != null && !input.dueAt.isAfter(input.availableFrom)) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_ASSIGNMENT_WINDOW", "Hạn hoàn thành phải sau thời gian mở")
        }
        val targetUsers = when (input.assigneeType) {
            AssignmentTargetType.USER -> setOf(input.assigneeId)
            AssignmentTargetType.GROUP, AssignmentTargetType.DEPARTMENT, AssignmentTargetType.BRANCH -> organization.users(input.assigneeId)
        }
        if (targetUsers.isEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "ASSIGNMENT_TARGET_EMPTY", "Phạm vi được chọn không có người dùng")
        val assignment = assignments.save(
            CourseAssignmentEntity(
                courseId = trainingClass.courseId,
                classId = trainingClass.id,
                assigneeType = input.assigneeType,
                assigneeId = input.assigneeId,
                assignedVersion = trainingClass.courseVersion,
                availableFrom = input.availableFrom,
                dueAt = input.dueAt ?: trainingClass.dueAt,
                gracePeriodMinutes = input.gracePeriodMinutes,
                required = input.required,
                assignedBy = CurrentUser.id(),
            )
        )
        var created = 0
        targetUsers.forEach { userId ->
            if (enrollments.findByClassIdAndUserId(trainingClass.id, userId) == null) {
                val enrollment = enrollments.save(
                    EnrollmentEntity(
                        classId = trainingClass.id,
                        courseId = trainingClass.courseId,
                        userId = userId,
                        dueAt = input.dueAt ?: trainingClass.dueAt,
                        idempotencyKey = "assignment:${assignment.id}:$userId",
                    )
                )
                events.publish(EventTypes.ENROLLED, "enrollment-service", enrollment.id.toString(), EnrolledPayload(enrollment.id, trainingClass.id, trainingClass.courseId, userId, enrollment.dueAt))
                created++
            }
        }
        return assignment.response(created)
    }

    @Transactional(readOnly = true)
    fun courseAssignments(courseId: UUID): List<CourseAssignmentResponse> {
        requireCourseManager(courseId, Permissions.COURSES_ASSIGN)
        return assignments.findAllByCourseIdOrderByAssignedAtDesc(courseId).map { it.response() }
    }

    @Transactional(readOnly = true)
    fun myAssignments(): List<CourseAssignmentResponse> {
        val userId = CurrentUser.id()
        val direct = assignments.findAllByAssigneeTypeAndAssigneeIdAndStatusOrderByAssignedAtDesc(AssignmentTargetType.USER, userId, CourseAssignmentStatus.ACTIVE)
        val units = runCatching { organization.userUnits(userId) }.getOrDefault(emptySet())
        val scoped = units.flatMap { unitId -> AssignmentTargetType.entries.filter { it != AssignmentTargetType.USER }.flatMap { type -> assignments.findAllByAssigneeTypeAndAssigneeIdAndStatusOrderByAssignedAtDesc(type, unitId, CourseAssignmentStatus.ACTIVE) } }
        return (direct + scoped).distinctBy { it.id }.sortedByDescending { it.assignedAt }.map { it.response() }
    }

    @Transactional
    fun createLive(input: CreateLiveSessionRequest): LiveSessionResponse {
        val trainingClass = classes.findById(input.classId).orElseThrow { classNotFound() }
        requireClassManager(trainingClass)
        if (!input.endsAt.isAfter(input.startsAt)) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_LIVE_WINDOW", "Thời gian kết thúc phải sau thời gian bắt đầu")
        return liveSessions.save(
            LiveSessionEntity(
                classId = trainingClass.id,
                courseId = trainingClass.courseId,
                title = input.title.trim(),
                provider = input.provider.trim().uppercase(),
                joinUrl = input.joinUrl.trim(),
                hostUrl = input.hostUrl?.trim(),
                startsAt = input.startsAt,
                endsAt = input.endsAt,
                createdBy = CurrentUser.id(),
            )
        ).response(true)
    }

    @Transactional(readOnly = true)
    fun classLive(classId: UUID): List<LiveSessionResponse> {
        val trainingClass = classes.findById(classId).orElseThrow { classNotFound() }
        val learner = enrollments.findByClassIdAndUserId(classId, CurrentUser.id()) != null
        if (!learner) requireClassManager(trainingClass)
        return liveSessions.findAllByClassIdOrderByStartsAtAsc(classId).map { it.response(!learner) }
    }

    @Transactional(readOnly = true)
    fun myLiveSessions(): List<LiveSessionResponse> {
        val activeStatuses = setOf(EnrollmentStatus.ENROLLED, EnrollmentStatus.IN_PROGRESS, EnrollmentStatus.OVERDUE)
        val classIds = enrollments.findAllByUserIdOrderByEnrolledAtDesc(CurrentUser.id())
            .filter { it.status in activeStatuses }
            .map { it.classId }
            .toSet()
        return classIds.flatMap(liveSessions::findAllByClassIdOrderByStartsAtAsc)
            .distinctBy { it.id }
            .sortedBy { it.startsAt }
            .map { it.response(false) }
    }

    private fun requireClassManager(trainingClass: TrainingClassEntity) {
        if (CurrentUser.isSystemAdmin() || "ADMIN" in CurrentUser.roles() || CurrentUser.id() == trainingClass.createdBy || CurrentUser.id() in trainingClass.instructorIds) return
        val permissions = setOf(Permissions.CLASSES_MANAGE, Permissions.COURSES_ASSIGN, Permissions.LIVE_SESSIONS_MANAGE)
        val scoped = CurrentUser.authorities().any { it in permissions } && permissions.any {
            scopedAuthorization.allowed(it, "COURSE", trainingClass.courseId)
        }
        if (!scoped) throw ApiException(HttpStatus.FORBIDDEN, "CLASS_OUT_OF_SCOPE", "Lớp ngoài phạm vi phụ trách")
    }

    private fun requireCourseManager(courseId: UUID, permission: String) {
        if (CurrentUser.isSystemAdmin() || "ADMIN" in CurrentUser.roles()) return
        val assignedInstructor = classes.findAll().any { it.courseId == courseId && CurrentUser.id() in it.instructorIds }
        val scoped = scopedAuthorization.allowed(permission, "COURSE", courseId) ||
            scopedAuthorization.allowed(Permissions.CLASSES_MANAGE, "COURSE", courseId)
        if (!assignedInstructor && !scoped) throw ApiException(HttpStatus.FORBIDDEN, "COURSE_OUT_OF_SCOPE", "Khóa học ngoài phạm vi phụ trách")
    }
    private fun classNotFound() = ApiException(HttpStatus.NOT_FOUND, "CLASS_NOT_FOUND", "Không tìm thấy lớp")
}

private fun CourseAssignmentEntity.response(enrolledUsers: Int = 0) = CourseAssignmentResponse(id, classId, courseId, assigneeType, assigneeId, assignedVersion, assignedAt, availableFrom, dueAt, gracePeriodMinutes, required, status, enrolledUsers)
private fun LiveSessionEntity.response(showHostUrl: Boolean = false) = LiveSessionResponse(id, classId, courseId, title, provider, joinUrl, hostUrl.takeIf { showHostUrl }, startsAt, endsAt, status)

@RestController
@RequestMapping("/api/v1/course-assignments")
class CourseAssignmentController(private val service: AssignmentAndLiveService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('${Permissions.COURSES_ASSIGN}','${Permissions.ENROLLMENTS_WRITE}')")
    fun assign(@Valid @RequestBody input: CreateCourseAssignmentRequest) = service.assign(input)

    @GetMapping
    @PreAuthorize("hasAuthority('${Permissions.COURSES_ASSIGN}')")
    fun list(@RequestParam courseId: UUID) = service.courseAssignments(courseId)

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_READ_SELF}')")
    fun mine() = service.myAssignments()
}

@RestController
@RequestMapping("/api/v1/live-sessions")
class LiveSessionController(private val service: AssignmentAndLiveService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.LIVE_SESSIONS_MANAGE}')")
    fun create(@Valid @RequestBody input: CreateLiveSessionRequest) = service.createLive(input)

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('${Permissions.LIVE_SESSIONS_JOIN}')")
    fun mine() = service.myLiveSessions()

    @GetMapping
    @PreAuthorize("hasAnyAuthority('${Permissions.LIVE_SESSIONS_JOIN}','${Permissions.LIVE_SESSIONS_MANAGE}')")
    fun list(@RequestParam classId: UUID) = service.classLive(classId)
}
