package com.lmspilot.enrollment.api

import com.lmspilot.contracts.EnrolledPayload
import com.lmspilot.contracts.EventTypes
import com.lmspilot.contracts.Permissions
import com.lmspilot.enrollment.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.InternalTokenAuthorizer
import com.lmspilot.support.security.ScopedAuthorizationClient
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
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

data class CreateClassRequest(
    @field:NotBlank @field:Size(max = 80) val code: String,
    @field:NotBlank @field:Size(max = 220) val name: String,
    val courseId: UUID,
    val startsAt: Instant? = null,
    val endsAt: Instant? = null,
    val dueAt: Instant? = null,
    @field:NotEmpty val instructorIds: Set<UUID>,
)
data class TrainingClassResponse(val id: UUID, val code: String, val name: String, val courseId: UUID, val courseVersion: Int, val startsAt: Instant?, val endsAt: Instant?, val dueAt: Instant?, val instructorIds: Set<UUID>, val status: TrainingClassStatus)
data class EnrollmentResponse(val id: UUID, val classId: UUID, val courseId: UUID, val userId: UUID, val dueAt: Instant?, val status: EnrollmentStatus, val enrolledAt: Instant)
data class EnrollUsersRequest(val userIds: Set<UUID>, val dueAt: Instant? = null)
data class EnrollResult(val created: List<EnrollmentResponse>, val existing: List<EnrollmentResponse>, val errors: Map<UUID, String>)
data class CoursePublication(val courseId: UUID, val published: Boolean, val version: Int, val status: String)
data class EnrollmentValidation(val enrollmentId: UUID, val classId: UUID, val courseId: UUID, val courseVersion: Int, val userId: UUID, val status: EnrollmentStatus, val dueAt: Instant?)

@Service
class CoursePublicationClient(
    builder: RestClient.Builder,
    @Value("\${course-service.url:http://localhost:8083}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client = builder.baseUrl(baseUrl).build()
    fun get(courseId: UUID): CoursePublication = client.get().uri("/internal/v1/courses/{id}/publication", courseId)
        .header("X-Service-Token", serviceToken).retrieve().body(CoursePublication::class.java)
        ?: throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "COURSE_SERVICE_UNAVAILABLE", "Không nhận được trạng thái khóa học")
}

@Service
class EnrollmentManagementService(
    private val classes: TrainingClassRepository,
    private val enrollments: EnrollmentRepository,
    private val courseClient: CoursePublicationClient,
    private val events: DomainEventPublisher,
    private val scopedAuthorization: ScopedAuthorizationClient,
) {
    @Transactional(readOnly = true)
    fun classes(): List<TrainingClassResponse> {
        val source = if (isAdmin()) classes.findAll() else classes.findAll().filter(::inClassScope)
        return source.sortedByDescending { it.createdAt }.map { it.response() }
    }

    @Transactional(readOnly = true)
    fun classDetail(id: UUID): TrainingClassResponse {
        val trainingClass = classes.findById(id).orElseThrow { classNotFound() }
        requireClassScope(trainingClass)
        return trainingClass.response()
    }

    @Transactional
    fun createClass(input: CreateClassRequest): TrainingClassResponse {
        if (classes.existsByCodeIgnoreCase(input.code)) throw ApiException(HttpStatus.CONFLICT, "DUPLICATE_CLASS_CODE", "Mã lớp đã tồn tại")
        if (input.startsAt != null && input.endsAt != null && !input.endsAt.isAfter(input.startsAt)) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_CLASS_TIME", "Thời gian kết thúc phải sau thời gian bắt đầu")
        val publication = courseClient.get(input.courseId)
        if (!publication.published) throw ApiException(HttpStatus.CONFLICT, "COURSE_NOT_PUBLISHED", "Chỉ có thể mở lớp từ khóa học đã xuất bản")
        return classes.save(TrainingClassEntity(code = input.code.trim().uppercase(), name = input.name.trim(), courseId = input.courseId, courseVersion = publication.version, startsAt = input.startsAt, endsAt = input.endsAt, dueAt = input.dueAt, instructorIds = input.instructorIds.toMutableSet(), createdBy = CurrentUser.id())).response()
    }

    @Transactional
    fun enroll(classId: UUID, input: EnrollUsersRequest, requestKey: String): EnrollResult {
        if (requestKey.isBlank() || requestKey.length > 80) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key phải có từ 1 đến 80 ký tự")
        }
        if (input.userIds.isEmpty()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "ENROLLMENT_USERS_REQUIRED", "Phải chọn ít nhất một học viên")
        }
        val trainingClass = classes.findById(classId).orElseThrow { classNotFound() }
        requireClassScope(trainingClass)
        if (trainingClass.status != TrainingClassStatus.OPEN) throw ApiException(HttpStatus.CONFLICT, "CLASS_NOT_OPEN", "Lớp không cho phép ghi danh")
        val created = mutableListOf<EnrollmentResponse>(); val existing = mutableListOf<EnrollmentResponse>(); val errors = mutableMapOf<UUID, String>()
        input.userIds.forEach { userId ->
            runCatching {
                enrollments.findByClassIdAndUserId(classId, userId)?.let { existing += it.response(); return@forEach }
                val itemKey = "$requestKey:$userId"
                enrollments.findByIdempotencyKey(itemKey)?.let { previous ->
                    if (previous.classId != classId || previous.userId != userId) {
                        throw ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "Idempotency-Key đã được dùng cho yêu cầu ghi danh khác")
                    }
                    existing += previous.response()
                    return@forEach
                }
                val entity = enrollments.save(EnrollmentEntity(classId = classId, courseId = trainingClass.courseId, userId = userId, dueAt = input.dueAt ?: trainingClass.dueAt, idempotencyKey = itemKey))
                created += entity.response()
                events.publish(EventTypes.ENROLLED, "enrollment-service", entity.id.toString(), EnrolledPayload(entity.id, classId, entity.courseId, userId, entity.dueAt))
            }.onFailure { errors[userId] = it.message ?: "Không thể ghi danh" }
        }
        return EnrollResult(created, existing, errors)
    }

    @Transactional(readOnly = true)
    fun myEnrollments() = enrollments.findAllByUserIdOrderByEnrolledAtDesc(CurrentUser.id()).map { it.response() }

    @Transactional(readOnly = true)
    fun classEnrollments(classId: UUID): List<EnrollmentResponse> {
        val trainingClass = classes.findById(classId).orElseThrow { classNotFound() }
        requireClassScope(trainingClass)
        return enrollments.findAllByClassIdOrderByEnrolledAtDesc(classId).map { it.response() }
    }

    @Transactional(readOnly = true)
    fun assignedClassIds(userId: UUID): Set<UUID> = classes.findAllAssignedTo(userId).map { it.id }.toSet()

    @Transactional(readOnly = true)
    fun assignedCourseIds(userId: UUID): Set<UUID> = classes.findAllAssignedTo(userId).map { it.courseId }.toSet()

    @Transactional(readOnly = true)
    fun activeCourseIds(userId: UUID): Set<UUID> = enrollments.findAllByUserIdOrderByEnrolledAtDesc(userId)
        .filter { it.status != EnrollmentStatus.CANCELLED }
        .map { it.courseId }
        .toSet()

    @Transactional(readOnly = true)
    fun accessibleCourseVersions(userId: UUID, courseId: UUID): Set<Int> {
        val activeEnrollments = enrollments.findAllByUserIdOrderByEnrolledAtDesc(userId)
            .filter { it.courseId == courseId && it.status != EnrollmentStatus.CANCELLED }
        if (activeEnrollments.isEmpty()) return emptySet()
        val classById = classes.findAllById(activeEnrollments.map { it.classId }.toSet()).associateBy { it.id }
        return activeEnrollments.mapNotNull { classById[it.classId]?.courseVersion }.toSet()
    }

    @Transactional(readOnly = true)
    fun validateEnrollment(id: UUID): EnrollmentValidation {
        val enrollment = enrollments.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "ENROLLMENT_NOT_FOUND", "Không tìm thấy ghi danh") }
        val trainingClass = classes.findById(enrollment.classId).orElseThrow { classNotFound() }
        return EnrollmentValidation(enrollment.id, enrollment.classId, enrollment.courseId, trainingClass.courseVersion, enrollment.userId, enrollment.status, enrollment.dueAt)
    }

    @Transactional(readOnly = true)
    fun activeEnrollments(userId: UUID, courseId: UUID): List<EnrollmentValidation> {
        val active = enrollments.findAllByUserIdOrderByEnrolledAtDesc(userId)
            .filter { it.courseId == courseId && it.status != EnrollmentStatus.CANCELLED }
        if (active.isEmpty()) return emptyList()
        val classById = classes.findAllById(active.map { it.classId }.toSet()).associateBy { it.id }
        return active.mapNotNull { enrollment ->
            classById[enrollment.classId]?.let { trainingClass ->
                EnrollmentValidation(
                    enrollment.id,
                    enrollment.classId,
                    enrollment.courseId,
                    trainingClass.courseVersion,
                    enrollment.userId,
                    enrollment.status,
                    enrollment.dueAt,
                )
            }
        }
    }

    private fun isAdmin() = CurrentUser.isSystemAdmin()
    private fun inClassScope(trainingClass: TrainingClassEntity): Boolean {
        if (isAdmin() || CurrentUser.id() in trainingClass.instructorIds || CurrentUser.id() == trainingClass.createdBy) return true
        val permissions = setOf(
            Permissions.CLASSES_READ, Permissions.CLASSES_WRITE, Permissions.CLASSES_MANAGE,
            Permissions.ENROLLMENTS_WRITE, Permissions.COURSES_ASSIGN, Permissions.LIVE_SESSIONS_MANAGE,
        )
        return permissions.any { permission ->
            permission in CurrentUser.authorities() && scopedAuthorization.allowed(permission, "COURSE", trainingClass.courseId)
        }
    }

    private fun requireClassScope(trainingClass: TrainingClassEntity) {
        if (!inClassScope(trainingClass)) {
            throw ApiException(HttpStatus.FORBIDDEN, "CLASS_OUT_OF_SCOPE", "Lớp ngoài phạm vi được phân công")
        }
    }
    private fun classNotFound() = ApiException(HttpStatus.NOT_FOUND, "CLASS_NOT_FOUND", "Không tìm thấy lớp đào tạo")
}

private fun TrainingClassEntity.response() = TrainingClassResponse(id, code, name, courseId, courseVersion, startsAt, endsAt, dueAt, instructorIds.toSet(), status)
private fun EnrollmentEntity.response() = EnrollmentResponse(id, classId, courseId, userId, dueAt, status, enrolledAt)

@RestController
@RequestMapping("/api/v1/classes")
class ClassController(private val service: EnrollmentManagementService) {
    @GetMapping @PreAuthorize("hasAnyAuthority('${Permissions.CLASSES_READ}','${Permissions.CLASSES_MANAGE}','${Permissions.LIVE_SESSIONS_MANAGE}','${Permissions.COURSES_ASSIGN}')") fun list() = service.classes()
    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('${Permissions.CLASSES_READ}','${Permissions.CLASSES_MANAGE}','${Permissions.LIVE_SESSIONS_MANAGE}','${Permissions.COURSES_ASSIGN}')") fun get(@PathVariable id: UUID) = service.classDetail(id)
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('${Permissions.CLASSES_WRITE}')") fun create(@Valid @RequestBody input: CreateClassRequest) = service.createClass(input)
    @PostMapping("/{id}/enrollments") @PreAuthorize("hasAuthority('${Permissions.ENROLLMENTS_WRITE}')") fun enroll(@PathVariable id: UUID, @Valid @RequestBody input: EnrollUsersRequest, @RequestHeader("Idempotency-Key") key: String) = service.enroll(id, input, key)
    @GetMapping("/{id}/enrollments") @PreAuthorize("hasAnyAuthority('${Permissions.CLASSES_READ}','${Permissions.CLASSES_MANAGE}','${Permissions.ENROLLMENTS_WRITE}')") fun enrollments(@PathVariable id: UUID) = service.classEnrollments(id)
}

@RestController
@RequestMapping("/api/v1/enrollments")
class EnrollmentController(private val service: EnrollmentManagementService) {
    @GetMapping("/me") @PreAuthorize("hasAuthority('${Permissions.LEARNING_READ_SELF}')") fun me() = service.myEnrollments()
}


@RestController
@RequestMapping("/internal/v1/enrollments")
class InternalEnrollmentController(
    private val service: EnrollmentManagementService,
    private val internal: InternalTokenAuthorizer,
) {
    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID, @RequestHeader("X-Service-Token", required = false) token: String?): EnrollmentValidation {
        internal.require(token)
        return service.validateEnrollment(id)
    }

    @GetMapping("/users/{userId}/courses/{courseId}")
    fun activeForCourse(
        @PathVariable userId: UUID,
        @PathVariable courseId: UUID,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ): List<EnrollmentValidation> {
        internal.require(token)
        return service.activeEnrollments(userId, courseId)
    }
}


@RestController
@RequestMapping("/internal/v1/classes")
class InternalClassScopeController(
    private val service: EnrollmentManagementService,
    private val internal: InternalTokenAuthorizer,
) {
    @GetMapping("/assigned/{userId}")
    fun assigned(@PathVariable userId: UUID, @RequestHeader("X-Service-Token", required = false) token: String?): Set<UUID> {
        internal.require(token)
        return service.assignedClassIds(userId)
    }

    @GetMapping("/assigned/{userId}/courses")
    fun assignedCourses(@PathVariable userId: UUID, @RequestHeader("X-Service-Token", required = false) token: String?): Set<UUID> {
        internal.require(token)
        return service.assignedCourseIds(userId)
    }

    @GetMapping("/user/{userId}/courses")
    fun activeCourses(@PathVariable userId: UUID, @RequestHeader("X-Service-Token", required = false) token: String?): Set<UUID> {
        internal.require(token)
        return service.activeCourseIds(userId)
    }

    @GetMapping("/user/{userId}/courses/{courseId}/versions")
    fun accessibleVersions(
        @PathVariable userId: UUID,
        @PathVariable courseId: UUID,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ): Set<Int> {
        internal.require(token)
        return service.accessibleCourseVersions(userId, courseId)
    }
}
