package com.lmspilot.enrollment.api

import com.lmspilot.contracts.EnrolledPayload
import com.lmspilot.contracts.EventTypes
import com.lmspilot.contracts.Permissions
import com.lmspilot.enrollment.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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
import org.springframework.http.client.SimpleClientHttpRequestFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class LearningProgressSummary(
    val enrollmentId: UUID,
    val courseId: UUID,
    val courseVersion: Int,
    val progressPercent: Int,
    val status: String,
    val completedAt: Instant?,
)

data class LearningPathItemRequest(
    val classId: UUID,
    val required: Boolean = true,
    val unlockMode: LearningPathUnlockMode = LearningPathUnlockMode.AFTER_PREVIOUS,
    @field:Min(0) @field:Max(3650) val dueOffsetDays: Int = 0,
)

data class LearningPathRequest(
    @field:NotBlank @field:Size(max = 80) val code: String,
    @field:NotBlank @field:Size(max = 220) val name: String,
    @field:Size(max = 5000) val description: String? = null,
    @field:NotEmpty @field:Size(max = 200) @field:Valid val items: List<LearningPathItemRequest>,
)

data class AssignLearningPathRequest(
    val assigneeType: AssignmentTargetType,
    val assigneeId: UUID,
    val dueAt: Instant? = null,
)

data class LearningPathItemResponse(
    val id: UUID,
    val classId: UUID,
    val courseId: UUID,
    val courseVersion: Int,
    val sortOrder: Int,
    val required: Boolean,
    val unlockMode: LearningPathUnlockMode,
    val dueOffsetDays: Int,
)

data class LearningPathResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val status: LearningPathStatus,
    val ownerId: UUID,
    val publishedAt: Instant?,
    val updatedAt: Instant,
    val items: List<LearningPathItemResponse>,
)

data class LearningPathAssignmentResponse(
    val id: UUID,
    val pathId: UUID,
    val assigneeType: AssignmentTargetType,
    val assigneeId: UUID,
    val dueAt: Instant?,
    val assignedBy: UUID,
    val assignedAt: Instant,
    val status: LearningPathAssignmentStatus,
    val expandedUsers: Int,
)

data class UserLearningPathItemResponse(
    val itemId: UUID,
    val classId: UUID,
    val courseId: UUID,
    val courseVersion: Int,
    val enrollmentId: UUID?,
    val sortOrder: Int,
    val required: Boolean,
    val unlocked: Boolean,
    val progressPercent: Int,
    val learningStatus: String,
    val dueAt: Instant?,
)

data class UserLearningPathResponse(
    val assignmentId: UUID,
    val pathId: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val status: UserLearningPathStatus,
    val progressPercent: Int,
    val dueAt: Instant?,
    val assignedAt: Instant,
    val completedAt: Instant?,
    val items: List<UserLearningPathItemResponse>,
)

data class LearningPathParticipantResponse(
    val userId: UUID,
    val status: UserLearningPathStatus,
    val dueAt: Instant?,
    val assignedAt: Instant,
    val completedAt: Instant?,
)

@Service
class LearningProgressClient(
    builder: RestClient.Builder,
    @Value("\${learning-service.url:http://localhost:8085}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
    @Value("\${learning-service.connect-timeout-ms:3000}") connectTimeoutMs: Int,
    @Value("\${learning-service.read-timeout-ms:5000}") readTimeoutMs: Int,
) {
    private val client = builder
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(connectTimeoutMs.coerceIn(500, 60_000))
            setReadTimeout(readTimeoutMs.coerceIn(500, 120_000))
        })
        .baseUrl(baseUrl)
        .build()

    fun summaries(userId: UUID): List<LearningProgressSummary> = client.get()
        .uri("/internal/v1/learning/users/{userId}/courses", userId)
        .header("X-Service-Token", serviceToken)
        .retrieve()
        .body(Array<LearningProgressSummary>::class.java)
        ?.toList()
        .orEmpty()
}

@Service
class LearningPathService(
    private val paths: LearningPathRepository,
    private val items: LearningPathItemRepository,
    private val assignments: LearningPathAssignmentRepository,
    private val userPaths: UserLearningPathRepository,
    private val classes: TrainingClassRepository,
    private val enrollments: EnrollmentRepository,
    private val organization: OrganizationUserScopeClient,
    private val learning: LearningProgressClient,
    private val events: DomainEventPublisher,
) {
    @Transactional(readOnly = true)
    fun list(): List<LearningPathResponse> = paths.findAllByStatusNotOrderByUpdatedAtDesc(LearningPathStatus.ARCHIVED).map(::view)

    @Transactional(readOnly = true)
    fun get(id: UUID): LearningPathResponse = view(path(id))

    @Transactional
    fun create(input: LearningPathRequest): LearningPathResponse {
        requireManager()
        val code = input.code.trim().uppercase()
        if (paths.existsByCodeIgnoreCase(code)) throw ApiException(HttpStatus.CONFLICT, "LEARNING_PATH_CODE_EXISTS", "Mã lộ trình đã tồn tại")
        val entity = paths.save(LearningPathEntity(code = code, name = input.name.trim(), description = input.description?.trim(), ownerId = CurrentUser.id()))
        replaceItems(entity, input.items)
        return view(entity)
    }

    @Transactional
    fun update(id: UUID, input: LearningPathRequest): LearningPathResponse {
        val entity = path(id)
        requireManager(entity)
        if (entity.status != LearningPathStatus.DRAFT) throw ApiException(HttpStatus.CONFLICT, "LEARNING_PATH_IMMUTABLE", "Lộ trình đã xuất bản không thể sửa trực tiếp; hãy nhân bản để tạo phiên bản mới")
        val code = input.code.trim().uppercase()
        if (!entity.code.equals(code, ignoreCase = true) && paths.existsByCodeIgnoreCase(code)) throw ApiException(HttpStatus.CONFLICT, "LEARNING_PATH_CODE_EXISTS", "Mã lộ trình đã tồn tại")
        entity.code = code
        entity.name = input.name.trim()
        entity.description = input.description?.trim()
        entity.updatedAt = Instant.now()
        replaceItems(entity, input.items)
        return view(entity)
    }

    @Transactional
    fun clone(id: UUID): LearningPathResponse {
        val source = path(id)
        requireManager(source)
        val suffix = Instant.now().epochSecond.toString().takeLast(6)
        val entity = paths.save(
            LearningPathEntity(
                code = "${source.code}-COPY-$suffix".take(80),
                name = "${source.name} (bản sao)".take(220),
                description = source.description,
                ownerId = CurrentUser.id(),
            )
        )
        val sourceItems = items.findAllByPathIdOrderBySortOrderAsc(source.id)
        items.saveAll(sourceItems.map { item ->
            LearningPathItemEntity(
                path = entity,
                classId = item.classId,
                courseId = item.courseId,
                courseVersion = item.courseVersion,
                sortOrder = item.sortOrder,
                required = item.required,
                unlockMode = item.unlockMode,
                dueOffsetDays = item.dueOffsetDays,
            )
        })
        return view(entity)
    }

    @Transactional
    fun publish(id: UUID): LearningPathResponse {
        val entity = path(id)
        requireManager(entity)
        if (entity.status != LearningPathStatus.DRAFT) throw ApiException(HttpStatus.CONFLICT, "LEARNING_PATH_NOT_DRAFT", "Chỉ lộ trình nháp mới được xuất bản; hãy nhân bản lộ trình đã phát hành")
        val pathItems = items.findAllByPathIdOrderBySortOrderAsc(id)
        if (pathItems.isEmpty()) throw ApiException(HttpStatus.CONFLICT, "LEARNING_PATH_EMPTY", "Lộ trình phải có ít nhất một lớp đào tạo")
        entity.status = LearningPathStatus.PUBLISHED
        entity.publishedAt = Instant.now()
        entity.updatedAt = entity.publishedAt!!
        return view(entity)
    }

    @Transactional
    fun archive(id: UUID): LearningPathResponse {
        val entity = path(id)
        requireManager(entity)
        entity.status = LearningPathStatus.ARCHIVED
        entity.updatedAt = Instant.now()
        return view(entity)
    }

    @Transactional
    fun assign(id: UUID, input: AssignLearningPathRequest): LearningPathAssignmentResponse {
        val entity = path(id)
        requireAssigner(entity)
        if (entity.status != LearningPathStatus.PUBLISHED) throw ApiException(HttpStatus.CONFLICT, "LEARNING_PATH_NOT_PUBLISHED", "Chỉ có thể giao lộ trình đã xuất bản")
        if (input.dueAt?.isBefore(Instant.now()) == true) throw ApiException(HttpStatus.BAD_REQUEST, "LEARNING_PATH_DUE_IN_PAST", "Hạn hoàn thành phải ở tương lai")
        val targetUsers = when (input.assigneeType) {
            AssignmentTargetType.USER -> setOf(input.assigneeId)
            else -> organization.users(input.assigneeId)
        }
        if (targetUsers.isEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "LEARNING_PATH_TARGET_EMPTY", "Phạm vi được chọn không có người dùng")
        val assignment = assignments.save(
            LearningPathAssignmentEntity(
                path = entity,
                assigneeType = input.assigneeType,
                assigneeId = input.assigneeId,
                dueAt = input.dueAt,
                assignedBy = CurrentUser.id(),
            )
        )
        val pathItems = items.findAllByPathIdOrderBySortOrderAsc(entity.id)
        targetUsers.forEach { userId ->
            val existing = userPaths.findByPathIdAndUserId(entity.id, userId)
            if (existing == null) {
                userPaths.save(UserLearningPathEntity(path = entity, userId = userId, sourceAssignmentId = assignment.id, dueAt = input.dueAt))
            } else {
                existing.sourceAssignmentId = assignment.id
                existing.dueAt = earlier(existing.dueAt, input.dueAt)
                if (existing.status == UserLearningPathStatus.CANCELLED) existing.status = UserLearningPathStatus.ASSIGNED
                existing.updatedAt = Instant.now()
            }
            pathItems.forEach { item -> ensureEnrollment(item, userId, assignment) }
            events.publish(
                EventTypes.LEARNING_PATH_ASSIGNED,
                "enrollment-service",
                "${entity.id}:$userId",
                mapOf("pathId" to entity.id, "pathCode" to entity.code, "pathName" to entity.name, "userId" to userId, "assignmentId" to assignment.id, "dueAt" to input.dueAt),
            )
        }
        return assignment.response(targetUsers.size)
    }

    @Transactional(readOnly = true)
    fun assignments(id: UUID): List<LearningPathAssignmentResponse> {
        val entity = path(id)
        requireManager(entity)
        return assignments.findAllByPathIdOrderByAssignedAtDesc(id).map { assignment ->
            assignment.response(userPaths.findAllByPathIdOrderByAssignedAtDesc(id).count { it.sourceAssignmentId == assignment.id })
        }
    }

    @Transactional
    fun cancelAssignment(pathId: UUID, assignmentId: UUID): LearningPathAssignmentResponse {
        val entity = path(pathId)
        requireAssigner(entity)
        val assignment = assignments.findById(assignmentId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "LEARNING_PATH_ASSIGNMENT_NOT_FOUND", "Không tìm thấy lần giao lộ trình") }
        if (assignment.path.id != pathId) throw ApiException(HttpStatus.BAD_REQUEST, "LEARNING_PATH_ASSIGNMENT_MISMATCH", "Lần giao không thuộc lộ trình")
        assignment.status = LearningPathAssignmentStatus.CANCELLED
        val participants = userPaths.findAllByPathIdOrderByAssignedAtDesc(pathId).filter { it.sourceAssignmentId == assignment.id }
        participants.forEach { participant ->
            if (participant.status != UserLearningPathStatus.COMPLETED) {
                participant.status = UserLearningPathStatus.CANCELLED
                participant.updatedAt = Instant.now()
            }
        }
        return assignment.response(participants.size)
    }

    @Transactional
    fun mine(): List<UserLearningPathResponse> {
        val userId = CurrentUser.id()
        val progressByEnrollment = try {
            learning.summaries(userId).associateBy { it.enrollmentId }
        } catch (error: Exception) {
            throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "LEARNING_PROGRESS_UNAVAILABLE", "Chưa thể tổng hợp tiến độ lộ trình; vui lòng thử lại sau")
        }
        return userPaths.findAllByUserIdAndStatusNotOrderByUpdatedAtDesc(userId, UserLearningPathStatus.CANCELLED)
            .map { userPath -> userView(userPath, progressByEnrollment) }
    }

    @Transactional(readOnly = true)
    fun participants(id: UUID): List<LearningPathParticipantResponse> {
        val entity = path(id)
        requireManager(entity)
        return userPaths.findAllByPathIdOrderByAssignedAtDesc(id).map { LearningPathParticipantResponse(it.userId, it.status, it.dueAt, it.assignedAt, it.completedAt) }
    }

    private fun replaceItems(path: LearningPathEntity, requests: List<LearningPathItemRequest>) {
        val duplicateClass = requests.groupBy { it.classId }.entries.firstOrNull { it.value.size > 1 }?.key
        if (duplicateClass != null) throw ApiException(HttpStatus.BAD_REQUEST, "LEARNING_PATH_DUPLICATE_CLASS", "Một lớp không thể xuất hiện hai lần trong cùng lộ trình")
        val classById = classes.findAllById(requests.map { it.classId }.toSet()).associateBy { it.id }
        if (classById.size != requests.map { it.classId }.toSet().size) throw ApiException(HttpStatus.BAD_REQUEST, "LEARNING_PATH_CLASS_NOT_FOUND", "Có lớp đào tạo không tồn tại")
        items.deleteAllByPathId(path.id)
        items.saveAll(requests.mapIndexed { index, request ->
            val trainingClass = classById.getValue(request.classId)
            LearningPathItemEntity(
                path = path,
                classId = trainingClass.id,
                courseId = trainingClass.courseId,
                courseVersion = trainingClass.courseVersion,
                sortOrder = index,
                required = request.required,
                unlockMode = if (index == 0) LearningPathUnlockMode.IMMEDIATE else request.unlockMode,
                dueOffsetDays = request.dueOffsetDays,
            )
        })
    }

    private fun ensureEnrollment(item: LearningPathItemEntity, userId: UUID, assignment: LearningPathAssignmentEntity) {
        if (enrollments.findByClassIdAndUserId(item.classId, userId) != null) return
        val trainingClass = classes.findById(item.classId).orElseThrow { ApiException(HttpStatus.CONFLICT, "LEARNING_PATH_CLASS_REMOVED", "Một lớp trong lộ trình không còn tồn tại") }
        if (trainingClass.status != TrainingClassStatus.OPEN) throw ApiException(HttpStatus.CONFLICT, "LEARNING_PATH_CLASS_NOT_OPEN", "Lớp ${trainingClass.code} không còn mở ghi danh")
        val itemDue = itemDueAt(assignment.assignedAt, assignment.dueAt, item.dueOffsetDays)
        val enrollment = enrollments.save(
            EnrollmentEntity(
                classId = item.classId,
                courseId = item.courseId,
                userId = userId,
                dueAt = itemDue ?: trainingClass.dueAt,
                idempotencyKey = "learning-path:${assignment.id}:${item.classId}:$userId",
            )
        )
        events.publish(EventTypes.ENROLLED, "enrollment-service", enrollment.id.toString(), EnrolledPayload(enrollment.id, item.classId, item.courseId, userId, enrollment.dueAt))
    }

    private fun userView(userPath: UserLearningPathEntity, progressByEnrollment: Map<UUID, LearningProgressSummary>): UserLearningPathResponse {
        val now = Instant.now()
        val pathItems = items.findAllByPathIdOrderBySortOrderAsc(userPath.path.id)
        var previousCompleted = true
        val itemViews = pathItems.map { item ->
            val enrollment = enrollments.findByClassIdAndUserId(item.classId, userPath.userId)
            val progress = enrollment?.id?.let(progressByEnrollment::get)
            val completed = progress?.status == "COMPLETED" || (progress?.progressPercent ?: 0) >= 100
            val unlocked = item.unlockMode == LearningPathUnlockMode.IMMEDIATE || previousCompleted
            val view = UserLearningPathItemResponse(
                itemId = item.id,
                classId = item.classId,
                courseId = item.courseId,
                courseVersion = item.courseVersion,
                enrollmentId = enrollment?.id,
                sortOrder = item.sortOrder,
                required = item.required,
                unlocked = unlocked,
                progressPercent = progress?.progressPercent ?: 0,
                learningStatus = progress?.status ?: "NOT_STARTED",
                dueAt = itemDueAt(userPath.assignedAt, userPath.dueAt, item.dueOffsetDays),
            )
            previousCompleted = completed
            view
        }
        val requiredItems = itemViews.filter { it.required }
        val overall = if (requiredItems.isEmpty()) 100 else requiredItems.sumOf { it.progressPercent } / requiredItems.size
        val allRequiredComplete = requiredItems.all { it.progressPercent >= 100 }
        val started = itemViews.any { it.progressPercent > 0 }
        val newStatus = when {
            allRequiredComplete -> UserLearningPathStatus.COMPLETED
            userPath.dueAt?.isBefore(now) == true -> UserLearningPathStatus.OVERDUE
            started -> UserLearningPathStatus.IN_PROGRESS
            else -> UserLearningPathStatus.ASSIGNED
        }
        if (newStatus != userPath.status) {
            val completedFirstTime = newStatus == UserLearningPathStatus.COMPLETED && userPath.completedAt == null
            userPath.status = newStatus
            userPath.updatedAt = now
            if (started && userPath.startedAt == null) userPath.startedAt = now
            if (completedFirstTime) {
                userPath.completedAt = now
                events.publish(EventTypes.LEARNING_PATH_COMPLETED, "enrollment-service", userPath.id.toString(), mapOf("pathId" to userPath.path.id, "userId" to userPath.userId, "completedAt" to now))
            }
        }
        return UserLearningPathResponse(userPath.id, userPath.path.id, userPath.path.code, userPath.path.name, userPath.path.description, userPath.status, overall, userPath.dueAt, userPath.assignedAt, userPath.completedAt, itemViews)
    }

    private fun view(entity: LearningPathEntity): LearningPathResponse = LearningPathResponse(
        entity.id,
        entity.code,
        entity.name,
        entity.description,
        entity.status,
        entity.ownerId,
        entity.publishedAt,
        entity.updatedAt,
        items.findAllByPathIdOrderBySortOrderAsc(entity.id).map { item -> LearningPathItemResponse(item.id, item.classId, item.courseId, item.courseVersion, item.sortOrder, item.required, item.unlockMode, item.dueOffsetDays) },
    )

    private fun path(id: UUID) = paths.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "LEARNING_PATH_NOT_FOUND", "Không tìm thấy lộ trình đào tạo") }

    private fun requireManager(entity: LearningPathEntity? = null) {
        if (CurrentUser.isSystemAdmin() || Permissions.LEARNING_PATHS_MANAGE in CurrentUser.authorities() || entity?.ownerId == CurrentUser.id()) return
        throw ApiException(HttpStatus.FORBIDDEN, "LEARNING_PATH_SCOPE_DENIED", "Không có quyền quản lý lộ trình đào tạo")
    }

    private fun requireAssigner(entity: LearningPathEntity) {
        if (CurrentUser.isSystemAdmin() || Permissions.LEARNING_PATHS_ASSIGN in CurrentUser.authorities() || entity.ownerId == CurrentUser.id()) return
        throw ApiException(HttpStatus.FORBIDDEN, "LEARNING_PATH_ASSIGN_DENIED", "Không có quyền giao lộ trình đào tạo")
    }

    private fun earlier(first: Instant?, second: Instant?): Instant? = when {
        first == null -> second
        second == null -> first
        first.isBefore(second) -> first
        else -> second
    }

    private fun itemDueAt(assignedAt: Instant, pathDueAt: Instant?, offsetDays: Int): Instant? {
        val offset = if (offsetDays > 0) assignedAt.plus(offsetDays.toLong(), ChronoUnit.DAYS) else null
        return earlier(pathDueAt, offset)
    }
}

private fun LearningPathAssignmentEntity.response(expandedUsers: Int) = LearningPathAssignmentResponse(id, path.id, assigneeType, assigneeId, dueAt, assignedBy, assignedAt, status, expandedUsers)

@RestController
@RequestMapping("/api/v1/learning-paths")
class LearningPathController(private val service: LearningPathService) {
    @GetMapping
    @PreAuthorize("hasAnyAuthority('${Permissions.LEARNING_PATHS_MANAGE}','${Permissions.LEARNING_PATHS_ASSIGN}')")
    fun list() = service.list()

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('${Permissions.LEARNING_PATHS_MANAGE}','${Permissions.LEARNING_PATHS_ASSIGN}')")
    fun get(@PathVariable id: UUID) = service.get(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_PATHS_MANAGE}')")
    fun create(@Valid @RequestBody input: LearningPathRequest) = service.create(input)

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_PATHS_MANAGE}')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody input: LearningPathRequest) = service.update(id, input)

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_PATHS_MANAGE}')")
    fun clone(@PathVariable id: UUID) = service.clone(id)

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_PATHS_MANAGE}')")
    fun publish(@PathVariable id: UUID) = service.publish(id)

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_PATHS_MANAGE}')")
    fun archive(@PathVariable id: UUID) = service.archive(id)

    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_PATHS_ASSIGN}')")
    fun assign(@PathVariable id: UUID, @Valid @RequestBody input: AssignLearningPathRequest) = service.assign(id, input)

    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasAnyAuthority('${Permissions.LEARNING_PATHS_MANAGE}','${Permissions.LEARNING_PATHS_ASSIGN}')")
    fun assignments(@PathVariable id: UUID) = service.assignments(id)

    @DeleteMapping("/{pathId}/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_PATHS_ASSIGN}')")
    fun cancel(@PathVariable pathId: UUID, @PathVariable assignmentId: UUID) = service.cancelAssignment(pathId, assignmentId)

    @GetMapping("/{id}/participants")
    @PreAuthorize("hasAnyAuthority('${Permissions.LEARNING_PATHS_MANAGE}','${Permissions.LEARNING_PATHS_ASSIGN}')")
    fun participants(@PathVariable id: UUID) = service.participants(id)

    @GetMapping("/me/assigned")
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_PATHS_READ}')")
    fun mine() = service.mine()
}
