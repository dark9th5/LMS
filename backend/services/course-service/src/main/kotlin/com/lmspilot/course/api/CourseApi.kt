package com.lmspilot.course.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.EventTypes
import com.lmspilot.contracts.Permissions
import com.lmspilot.course.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.InternalTokenAuthorizer
import com.lmspilot.support.security.ScopedAuthorizationClient
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.UUID

data class CategoryRequest(@field:NotBlank @field:Size(max = 80) val code: String, @field:NotBlank @field:Size(max = 180) val name: String, val parentId: UUID? = null, val status: RecordStatus = RecordStatus.ACTIVE, val sortOrder: Int = 0)
data class CategoryResponse(val id: UUID, val code: String, val name: String, val parentId: UUID?, val status: RecordStatus, val sortOrder: Int)

data class CourseRequest(
    @field:NotBlank @field:Size(max = 80) val code: String,
    @field:NotBlank @field:Size(max = 240) val name: String,
    val description: String? = null,
    val objectives: String? = null,
    val targetAudience: String? = null,
    @field:Min(0) val durationMinutes: Int? = null,
    @field:DecimalMin("0.0") @field:DecimalMax("100.0") val passingScore: Double = 70.0,
    val completionPolicyJson: String = "{\"requiredLessonPercent\":100}",
    val categoryId: UUID? = null,
)

data class LessonRequest(
    @field:NotBlank @field:Size(max = 220) val title: String,
    val type: LessonType,
    val textContent: String? = null,
    val fileId: UUID? = null,
    val required: Boolean = true,
    @field:Min(0) val sortOrder: Int,
    @field:Min(0) val estimatedMinutes: Int = 0,
)

data class LessonResponse(val id: UUID, val title: String, val type: LessonType, val textContent: String?, val fileId: UUID?, val required: Boolean, val sortOrder: Int, val estimatedMinutes: Int)
data class CourseResponse(
    val id: UUID, val code: String, val name: String, val description: String?, val objectives: String?,
    val targetAudience: String?, val durationMinutes: Int?, val passingScore: Double,
    val completionPolicyJson: String, val categoryId: UUID?, val status: CourseStatus,
    val contentVersion: Int, val publishedVersion: Int, val hasUnpublishedChanges: Boolean,
    val publishedAt: Instant?, val ownerId: UUID, val lessons: List<LessonResponse> = emptyList(),
)
data class PageResponse<T>(val items: List<T>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)
data class PublicationStatus(val courseId: UUID, val published: Boolean, val version: Int, val status: CourseStatus)
data class CourseLearningMetadata(
    val courseId: UUID,
    val version: Int,
    val status: CourseStatus,
    val lessonIds: Set<UUID>,
    val requiredLessonIds: Set<UUID>,
    val lessonTypes: Map<UUID, LessonType> = emptyMap(),
    val lessonFileIds: Set<UUID> = emptySet(),
)
data class CourseVersionSummary(val version: Int, val createdAt: Instant, val createdBy: UUID)
data class CourseSnapshot(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val objectives: String?,
    val targetAudience: String?,
    val durationMinutes: Int?,
    val passingScore: Double,
    val completionPolicyJson: String,
    val categoryId: UUID?,
    val version: Int,
    val ownerId: UUID,
    val lessons: List<LessonResponse>,
)


@Service
class EnrollmentCourseScopeClient(
    builder: RestClient.Builder,
    @Value("\${enrollment-service.url:http://localhost:8084}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client: RestClient = builder.baseUrl(baseUrl).build()

    fun activeCourseIds(userId: UUID): Set<UUID> = courseIds("/internal/v1/classes/user/{userId}/courses", userId)

    fun assignedCourseIds(userId: UUID): Set<UUID> = courseIds("/internal/v1/classes/assigned/{userId}/courses", userId)

    fun accessibleVersions(userId: UUID, courseId: UUID): Set<Int> {
        val values = client.get()
            .uri("/internal/v1/classes/user/{userId}/courses/{courseId}/versions", userId, courseId)
            .header("X-Service-Token", serviceToken)
            .retrieve()
            .body(Array<Int>::class.java)
            ?: emptyArray()
        return values.toSet()
    }

    private fun courseIds(path: String, userId: UUID): Set<UUID> {
        val values = client.get()
            .uri(path, userId)
            .header("X-Service-Token", serviceToken)
            .retrieve()
            .body(Array<String>::class.java)
            ?: emptyArray()
        return values.map(UUID::fromString).toSet()
    }
}

@Service
class CourseFileAccessClient(
    builder: RestClient.Builder,
    @Value("\${file-storage-service.url:http://localhost:8089}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client: RestClient = builder.baseUrl(baseUrl).build()

    data class FileMetadata(val id: UUID, val ownerId: UUID, val purpose: String, val status: String)

    fun requireAttachable(fileId: UUID, actorId: UUID, administrator: Boolean) {
        val file = client.get()
            .uri("/internal/v1/files/{id}", fileId)
            .header("X-Service-Token", serviceToken)
            .retrieve()
            .body(FileMetadata::class.java)
            ?: throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "FILE_SERVICE_UNAVAILABLE", "Không nhận được metadata tệp")
        if (!administrator && file.ownerId != actorId) {
            throw ApiException(HttpStatus.FORBIDDEN, "COURSE_FILE_OWNER_MISMATCH", "Không thể gắn tệp của người dùng khác vào khóa học")
        }
        if (file.status != "AVAILABLE" || file.purpose != "COURSE_CONTENT") {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_COURSE_FILE", "Tệp phải khả dụng và được tải lên cho nội dung khóa học")
        }
    }

    fun grant(userId: UUID, courseId: UUID, version: Int, fileIds: Set<UUID>) {
        if (fileIds.isEmpty()) return
        client.post()
            .uri("/internal/v1/files/access-grants")
            .header("X-Service-Token", serviceToken)
            .body(mapOf("userId" to userId, "fileIds" to fileIds, "source" to "COURSE:$courseId:V$version", "ttlSeconds" to 14_400))
            .retrieve()
            .toBodilessEntity()
    }
}

@Service
class CourseManagementService(
    private val categories: CourseCategoryRepository,
    private val courses: CourseRepository,
    private val lessons: LessonRepository,
    private val events: DomainEventPublisher,
    private val enrollmentScope: EnrollmentCourseScopeClient,
    private val scopedAuthorization: ScopedAuthorizationClient,
    private val fileAccess: CourseFileAccessClient,
    private val versions: CourseVersionRepository,
    private val mapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun categories() = categories.findAll(Sort.by("sortOrder", "name")).map { it.response() }

    @Transactional
    fun createCategory(input: CategoryRequest): CategoryResponse {
        requireCategoryPermission()
        if (categories.existsByCodeIgnoreCase(input.code)) conflict("Mã danh mục đã tồn tại")
        input.parentId?.let { categories.findById(it).orElseThrow { ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_PARENT_NOT_FOUND", "Danh mục cha không tồn tại") } }
        return categories.save(CourseCategoryEntity(code = input.code.trim().uppercase(), name = input.name.trim(), parentId = input.parentId, status = input.status, sortOrder = input.sortOrder)).response()
    }

    @Transactional
    fun updateCategory(id: UUID, input: CategoryRequest): CategoryResponse {
        requireCategoryPermission()
        val entity = categories.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Không tìm thấy danh mục") }
        input.parentId?.let { parentId ->
            if (parentId == id) throw ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_CYCLE", "Danh mục không thể là cha của chính nó")
            categories.findById(parentId).orElseThrow { ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_PARENT_NOT_FOUND", "Danh mục cha không tồn tại") }
            var cursor: UUID? = parentId
            repeat(100) {
                if (cursor == null) return@repeat
                if (cursor == id) throw ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_CYCLE", "Cấu trúc danh mục tạo thành vòng lặp")
                cursor = categories.findById(cursor!!).orElse(null)?.parentId
            }
        }
        entity.name = input.name.trim(); entity.parentId = input.parentId; entity.status = input.status; entity.sortOrder = input.sortOrder; entity.updatedAt = Instant.now()
        return entity.response()
    }

    @Transactional
    fun deactivateCategory(id: UUID): CategoryResponse {
        requireCategoryPermission()
        val entity = categories.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Không tìm thấy danh mục") }
        entity.status = RecordStatus.INACTIVE
        entity.updatedAt = Instant.now()
        return entity.response()
    }

    @Transactional(readOnly = true)
    fun search(query: String?, status: CourseStatus?, categoryId: UUID?, page: Int, size: Int): PageResponse<CourseResponse> {
        val canManage = canManageCourses()
        if (!canManage) return learnerCourses(query, categoryId, page, size)

        val currentUserId = CurrentUser.id()
        val ownerId = if (!isGlobalAdministrator()) currentUserId else null
        val assignedCourseIds = if (ownerId == null) {
            setOf(UUID(0L, 0L))
        } else {
            (enrollmentScope.assignedCourseIds(currentUserId) + scopedCourseIds()).ifEmpty { setOf(UUID(0L, 0L)) }
        }
        val result = courses.searchVisible(
            query?.takeIf { it.isNotBlank() },
            status,
            categoryId,
            ownerId,
            assignedCourseIds,
            status == CourseStatus.ARCHIVED,
            CourseStatus.ARCHIVED,
            PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by("updatedAt").descending()),
        )
        return PageResponse(result.content.map { it.response() }, result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): CourseResponse {
        val course = courses.findById(id).orElseThrow { notFound() }
        requireVisible(course)
        if (!canManageCourses()) {
            val pinnedVersion = enrollmentScope.accessibleVersions(CurrentUser.id(), id).maxOrNull() ?: throw notFound()
            return snapshotAt(course, pinnedVersion)
                .response(course.status, course.publishedAt, publishedVersion = pinnedVersion)
                .also(::grantFiles)
        }
        val response = course.response(lessons.findAllByCourseIdOrderBySortOrderAsc(id).map { it.response() })
        grantFiles(response)
        return response
    }

    @Transactional(readOnly = true)
    fun version(courseId: UUID, version: Int): CourseResponse {
        val course = courses.findById(courseId).orElseThrow { notFound() }
        requireVisible(course)
        if (!canManageCourses() && version !in enrollmentScope.accessibleVersions(CurrentUser.id(), courseId)) {
            // Return 404 rather than leaking which historical versions exist to another class.
            throw notFound()
        }
        val snapshot = snapshotAt(course, version)
        val response = snapshot.response(course.status, course.publishedAt, publishedVersion = version)
        grantFiles(response)
        return response
    }

    @Transactional(readOnly = true)
    fun versions(courseId: UUID): List<CourseVersionSummary> {
        val course = courses.findById(courseId).orElseThrow { notFound() }
        requireCoursePermission(course, Permissions.COURSES_READ)
        return versions.findAllByCourseIdOrderByVersionNumberDesc(courseId)
            .map { CourseVersionSummary(it.versionNumber, it.createdAt, it.createdBy) }
    }

    @Transactional
    fun create(input: CourseRequest): CourseResponse {
        requireCourseCreationPermission()
        if (courses.existsByCodeIgnoreCase(input.code)) conflict("Mã khóa học đã tồn tại")
        input.categoryId?.let { categories.findById(it).orElseThrow { ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_NOT_FOUND", "Danh mục không tồn tại") } }
        return courses.save(input.toEntity(CurrentUser.id())).response()
    }

    @Transactional
    fun update(id: UUID, input: CourseRequest): CourseResponse {
        val course = courses.findById(id).orElseThrow { notFound() }
        requireCoursePermission(course, Permissions.COURSES_UPDATE)
        if (course.status == CourseStatus.ARCHIVED) throw ApiException(HttpStatus.CONFLICT, "COURSE_ARCHIVED", "Không thể sửa khóa học đã lưu trữ")
        input.categoryId?.let { categories.findById(it).orElseThrow { ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_NOT_FOUND", "Danh mục không tồn tại") } }
        prepareDraftVersion(course)
        course.name = input.name.trim(); course.description = input.description; course.objectives = input.objectives
        course.targetAudience = input.targetAudience; course.durationMinutes = input.durationMinutes
        course.passingScore = input.passingScore; course.completionPolicyJson = input.completionPolicyJson
        course.categoryId = input.categoryId; course.updatedAt = Instant.now()
        return get(course.id)
    }

    @Transactional
    fun addLesson(courseId: UUID, input: LessonRequest): LessonResponse {
        val course = courses.findById(courseId).orElseThrow { notFound() }
        requireCoursePermission(course, Permissions.COURSES_UPDATE)
        ensureEditable(course)
        prepareDraftVersion(course)
        validateLesson(input)
        val lesson = lessons.save(LessonEntity(courseId = courseId, title = input.title.trim(), type = input.type, textContent = normalizedText(input), fileId = normalizedFileId(input), required = input.required, sortOrder = input.sortOrder, estimatedMinutes = input.estimatedMinutes))
        touchContent(course)
        return lesson.response()
    }

    @Transactional
    fun updateLesson(courseId: UUID, lessonId: UUID, input: LessonRequest): LessonResponse {
        val course = courses.findById(courseId).orElseThrow { notFound() }
        requireCoursePermission(course, Permissions.COURSES_UPDATE)
        ensureEditable(course)
        prepareDraftVersion(course)
        validateLesson(input)
        val lesson = lessons.findById(lessonId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "LESSON_NOT_FOUND", "Không tìm thấy bài học") }
        if (lesson.courseId != courseId) throw ApiException(HttpStatus.NOT_FOUND, "LESSON_NOT_FOUND", "Không tìm thấy bài học")
        lesson.title = input.title.trim(); lesson.type = input.type; lesson.textContent = normalizedText(input); lesson.fileId = normalizedFileId(input)
        lesson.required = input.required; lesson.sortOrder = input.sortOrder; lesson.estimatedMinutes = input.estimatedMinutes; lesson.updatedAt = Instant.now()
        touchContent(course)
        return lesson.response()
    }

    @Transactional
    fun deleteLesson(courseId: UUID, lessonId: UUID) {
        val course = courses.findById(courseId).orElseThrow { notFound() }
        requireCoursePermission(course, Permissions.COURSES_UPDATE)
        ensureEditable(course)
        prepareDraftVersion(course)
        val lesson = lessons.findById(lessonId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "LESSON_NOT_FOUND", "Không tìm thấy bài học") }
        if (lesson.courseId != courseId) throw ApiException(HttpStatus.NOT_FOUND, "LESSON_NOT_FOUND", "Không tìm thấy bài học")
        lessons.delete(lesson)
        touchContent(course)
    }

    @Transactional
    fun archive(id: UUID) {
        val course = courses.findById(id).orElseThrow { notFound() }
        requireCoursePermission(course, Permissions.COURSES_UPDATE)
        course.status = CourseStatus.ARCHIVED
        course.updatedAt = Instant.now()
    }

    @Transactional
    fun transition(id: UUID, target: CourseStatus): CourseResponse {
        val course = courses.findById(id).orElseThrow { notFound() }
        requireCoursePermission(course, Permissions.COURSES_PUBLISH)
        when (target) {
            CourseStatus.PUBLISHED -> {
                if (lessons.countByCourseId(id) == 0L) throw ApiException(HttpStatus.CONFLICT, "COURSE_EMPTY", "Khóa học phải có ít nhất một bài học trước khi xuất bản")
                saveSnapshot(course)
                course.status = CourseStatus.PUBLISHED
                course.publishedVersion = course.contentVersion
                course.publishedAt = Instant.now()
                course.publishedBy = CurrentUser.id()
                events.publish(EventTypes.COURSE_PUBLISHED, "course-service", id.toString(), mapOf("courseId" to id, "version" to course.publishedVersion, "name" to course.name))
            }
            CourseStatus.HIDDEN -> if (course.status !in setOf(CourseStatus.PUBLISHED, CourseStatus.HIDDEN)) invalidTransition() else course.status = CourseStatus.HIDDEN
            CourseStatus.ARCHIVED -> course.status = CourseStatus.ARCHIVED
            CourseStatus.DRAFT -> if (course.publishedAt != null) invalidTransition() else course.status = CourseStatus.DRAFT
        }
        course.updatedAt = Instant.now()
        return get(id)
    }

    @Transactional(readOnly = true)
    fun publication(id: UUID): PublicationStatus {
        val c = courses.findById(id).orElseThrow { notFound() }
        return PublicationStatus(c.id, c.status == CourseStatus.PUBLISHED && c.publishedVersion > 0, c.publishedVersion, c.status)
    }

    @Transactional(readOnly = true)
    fun learningMetadata(id: UUID, version: Int?): CourseLearningMetadata {
        val course = courses.findById(id).orElseThrow { notFound() }
        val requestedVersion = version ?: course.publishedVersion.takeIf { it > 0 } ?: course.contentVersion
        val snapshot = versions.findByCourseIdAndVersionNumber(id, requestedVersion)?.let { mapper.readValue(it.snapshotJson, CourseSnapshot::class.java) }
            ?: if (requestedVersion == course.publishedVersion && requestedVersion == course.contentVersion) currentSnapshot(course) else null
            ?: throw ApiException(HttpStatus.NOT_FOUND, "COURSE_VERSION_NOT_FOUND", "Không tìm thấy phiên bản khóa học")
        return CourseLearningMetadata(
            courseId = course.id,
            version = requestedVersion,
            status = course.status,
            lessonIds = snapshot.lessons.map { it.id }.toSet(),
            requiredLessonIds = snapshot.lessons.filter { it.required }.map { it.id }.toSet(),
            lessonTypes = snapshot.lessons.associate { it.id to it.type },
            lessonFileIds = snapshot.lessons.mapNotNull { it.fileId }.toSet(),
        )
    }

    private fun ensureEditable(course: CourseEntity) {
        if (course.status == CourseStatus.ARCHIVED) throw ApiException(HttpStatus.CONFLICT, "COURSE_ARCHIVED", "Khóa học đã lưu trữ")
    }

    private fun grantFiles(course: CourseResponse) {
        fileAccess.grant(CurrentUser.id(), course.id, course.contentVersion, course.lessons.mapNotNull { it.fileId }.toSet())
    }

    private fun validateLesson(input: LessonRequest) {
        if (input.type == LessonType.TEXT && input.textContent.isNullOrBlank()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "TEXT_REQUIRED", "Bài học văn bản cần có nội dung")
        }
        if (input.type !in setOf(LessonType.TEXT, LessonType.ASSIGNMENT, LessonType.EXAM) && input.fileId == null) {
            throw ApiException(HttpStatus.BAD_REQUEST, "FILE_REQUIRED", "Loại bài học này cần file tài nguyên")
        }
        if (input.fileId != null && input.type !in setOf(LessonType.TEXT, LessonType.ASSIGNMENT, LessonType.EXAM)) {
            fileAccess.requireAttachable(input.fileId, CurrentUser.id(), isGlobalAdministrator())
        }
    }

    private fun normalizedText(input: LessonRequest) = input.textContent?.trim()?.takeIf { input.type in setOf(LessonType.TEXT, LessonType.ASSIGNMENT, LessonType.EXAM) && it.isNotBlank() }
    private fun normalizedFileId(input: LessonRequest) = input.fileId?.takeIf { input.type !in setOf(LessonType.TEXT, LessonType.ASSIGNMENT, LessonType.EXAM) }

    private fun touchContent(course: CourseEntity) {
        prepareDraftVersion(course)
        course.updatedAt = Instant.now()
    }

    private fun prepareDraftVersion(course: CourseEntity) {
        if (course.publishedVersion > 0 && course.contentVersion <= course.publishedVersion) {
            ensurePublishedSnapshot(course)
            course.contentVersion = course.publishedVersion + 1
        }
    }

    private fun ensurePublishedSnapshot(course: CourseEntity) {
        if (course.publishedVersion <= 0 || versions.findByCourseIdAndVersionNumber(course.id, course.publishedVersion) != null) return
        val snapshot = currentSnapshot(course, version = course.publishedVersion)
        versions.save(CourseVersionEntity(courseId = course.id, versionNumber = course.publishedVersion, snapshotJson = mapper.writeValueAsString(snapshot), createdBy = course.publishedBy ?: course.ownerId, createdAt = course.publishedAt ?: course.createdAt))
    }

    private fun saveSnapshot(course: CourseEntity) {
        val snapshot = currentSnapshot(course)
        val existing = versions.findByCourseIdAndVersionNumber(course.id, course.contentVersion)
        if (existing != null) {
            if (existing.snapshotJson != mapper.writeValueAsString(snapshot)) {
                throw ApiException(HttpStatus.CONFLICT, "IMMUTABLE_COURSE_VERSION", "Phiên bản khóa học đã được xuất bản và không thể ghi đè")
            }
            return
        }
        versions.save(CourseVersionEntity(courseId = course.id, versionNumber = course.contentVersion, snapshotJson = mapper.writeValueAsString(snapshot), createdBy = CurrentUser.id()))
    }

    private fun snapshotAt(course: CourseEntity, version: Int): CourseSnapshot =
        versions.findByCourseIdAndVersionNumber(course.id, version)?.let { mapper.readValue(it.snapshotJson, CourseSnapshot::class.java) }
            ?: if (version == course.publishedVersion && course.contentVersion == version) currentSnapshot(course) else null
            ?: throw ApiException(HttpStatus.NOT_FOUND, "COURSE_VERSION_NOT_FOUND", "Không tìm thấy phiên bản khóa học")

    private fun currentSnapshot(course: CourseEntity, version: Int = course.contentVersion): CourseSnapshot = CourseSnapshot(
        id = course.id,
        code = course.code,
        name = course.name,
        description = course.description,
        objectives = course.objectives,
        targetAudience = course.targetAudience,
        durationMinutes = course.durationMinutes,
        passingScore = course.passingScore,
        completionPolicyJson = course.completionPolicyJson,
        categoryId = course.categoryId,
        version = version,
        ownerId = course.ownerId,
        lessons = lessons.findAllByCourseIdOrderBySortOrderAsc(course.id).map { it.response() },
    )

    private fun requireVisible(course: CourseEntity) {
        if (canManageCourses()) {
            val visible = isGlobalAdministrator() || course.ownerId == CurrentUser.id() ||
                course.id in enrollmentScope.assignedCourseIds(CurrentUser.id()) || course.id in scopedCourseIds()
            if (!visible) throw notFound()
        } else if (course.status != CourseStatus.PUBLISHED || course.id !in enrollmentScope.activeCourseIds(CurrentUser.id())) {
            throw notFound()
        }
    }

    private fun learnerCourses(query: String?, categoryId: UUID?, page: Int, size: Int): PageResponse<CourseResponse> {
        val normalizedQuery = query?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        val allowed = enrollmentScope.activeCourseIds(CurrentUser.id())
        val filtered: List<CourseEntity> = if (allowed.isEmpty()) emptyList() else courses.findAllById(allowed)
            .asSequence()
            .filter { it.status == CourseStatus.PUBLISHED }
            .filter { categoryId == null || it.categoryId == categoryId }
            .filter { normalizedQuery == null || it.code.lowercase().contains(normalizedQuery) || it.name.lowercase().contains(normalizedQuery) }
            .sortedByDescending { it.updatedAt }
            .toList()
        val safeSize = size.coerceIn(1, 100)
        val safePage = page.coerceAtLeast(0)
        val from = (safePage * safeSize).coerceAtMost(filtered.size)
        val to = (from + safeSize).coerceAtMost(filtered.size)
        val totalPages = if (filtered.isEmpty()) 0 else (filtered.size + safeSize - 1) / safeSize
        return PageResponse(filtered.subList(from, to).map { it.response() }, safePage, safeSize, filtered.size.toLong(), totalPages)
    }

    private val courseManagementPermissions = setOf(
        Permissions.COURSES_CREATE, Permissions.COURSES_UPDATE, Permissions.COURSES_WRITE, Permissions.COURSES_PUBLISH,
    )

    private fun canManageCourses() = CurrentUser.authorities().any { it in courseManagementPermissions }

    private fun isGlobalAdministrator() = CurrentUser.isSystemAdmin()

    private fun requireCategoryPermission() {
        val allowed = isGlobalAdministrator() || Permissions.COURSE_CATEGORIES_MANAGE in CurrentUser.authorities() || scopedAuthorization.allowed(Permissions.COURSE_CATEGORIES_MANAGE, "SYSTEM", null)
        if (!allowed) throw ApiException(HttpStatus.FORBIDDEN, "CATEGORY_MANAGE_DENIED", "Không có quyền quản lý danh mục khóa học")
    }

    private fun requireCourseCreationPermission() {
        val allowed = isGlobalAdministrator() ||
            Permissions.COURSES_CREATE in CurrentUser.authorities() ||
            Permissions.COURSES_WRITE in CurrentUser.authorities() ||
            scopedAuthorization.allowed(Permissions.COURSES_CREATE, "SYSTEM", null) ||
            scopedAuthorization.allowed(Permissions.COURSES_WRITE, "SYSTEM", null)
        if (!allowed) {
            throw ApiException(HttpStatus.FORBIDDEN, "COURSE_CREATE_OUT_OF_SCOPE", "Cần quyền toàn hệ thống để tạo khóa học mới")
        }
    }

    private fun scopedCourseIds(): Set<UUID> =
        scopedAuthorization.scopeIds(Permissions.COURSES_UPDATE, "COURSE") +
            scopedAuthorization.scopeIds(Permissions.COURSES_WRITE, "COURSE") +
            scopedAuthorization.scopeIds(Permissions.COURSES_PUBLISH, "COURSE") +
            scopedAuthorization.scopeIds(Permissions.COURSES_READ, "COURSE")

    private fun requireCoursePermission(course: CourseEntity, permission: String) {
        val legacyPermission = if (permission == Permissions.COURSES_UPDATE) Permissions.COURSES_WRITE else permission
        val allowed = isGlobalAdministrator() || course.ownerId == CurrentUser.id() ||
            scopedAuthorization.allowed(permission, "COURSE", course.id) ||
            scopedAuthorization.allowed(legacyPermission, "COURSE", course.id)
        if (!allowed) {
            throw ApiException(HttpStatus.FORBIDDEN, "COURSE_OUT_OF_SCOPE", "Khóa học ngoài phạm vi được cấp")
        }
    }
    private fun notFound() = ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "Không tìm thấy khóa học")
    private fun conflict(message: String): Nothing = throw ApiException(HttpStatus.CONFLICT, "COURSE_CONFLICT", message)
    private fun invalidTransition(): Nothing = throw ApiException(HttpStatus.CONFLICT, "INVALID_COURSE_TRANSITION", "Chuyển trạng thái khóa học không hợp lệ")
}

private fun CourseRequest.toEntity(owner: UUID) = CourseEntity(code = code.trim().uppercase(), name = name.trim(), description = description, objectives = objectives, targetAudience = targetAudience, durationMinutes = durationMinutes, passingScore = passingScore, completionPolicyJson = completionPolicyJson, categoryId = categoryId, ownerId = owner)
private fun CourseCategoryEntity.response() = CategoryResponse(id, code, name, parentId, status, sortOrder)
private fun CourseEntity.response(lessons: List<LessonResponse> = emptyList()) = CourseResponse(id, code, name, description, objectives, targetAudience, durationMinutes, passingScore, completionPolicyJson, categoryId, status, contentVersion, publishedVersion, contentVersion > publishedVersion, publishedAt, ownerId, lessons)
private fun CourseSnapshot.response(status: CourseStatus, publishedAt: Instant?, publishedVersion: Int) = CourseResponse(id, code, name, description, objectives, targetAudience, durationMinutes, passingScore, completionPolicyJson, categoryId, status, version, publishedVersion, false, publishedAt, ownerId, lessons)
private fun LessonEntity.response() = LessonResponse(id, title, type, textContent, fileId, required, sortOrder, estimatedMinutes)

@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(private val service: CourseManagementService) {
    @GetMapping @PreAuthorize("hasAuthority('${Permissions.COURSES_READ}')") fun list() = service.categories()
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('${Permissions.COURSE_CATEGORIES_MANAGE}','${Permissions.COURSES_CREATE}','${Permissions.COURSES_WRITE}')") fun create(@Valid @RequestBody input: CategoryRequest) = service.createCategory(input)
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('${Permissions.COURSE_CATEGORIES_MANAGE}')") fun update(@PathVariable id: UUID, @Valid @RequestBody input: CategoryRequest) = service.updateCategory(id, input)
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('${Permissions.COURSE_CATEGORIES_MANAGE}')") fun deactivate(@PathVariable id: UUID) = service.deactivateCategory(id)
}

@RestController
@RequestMapping("/api/v1/courses")
class CourseController(private val service: CourseManagementService) {
    @GetMapping @PreAuthorize("hasAuthority('${Permissions.COURSES_READ}')") fun search(@RequestParam(required=false) query: String?, @RequestParam(required=false) status: CourseStatus?, @RequestParam(required=false) categoryId: UUID?, @RequestParam(defaultValue="0") page: Int, @RequestParam(defaultValue="20") size: Int) = service.search(query, status, categoryId, page, size)
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('${Permissions.COURSES_READ}')") fun get(@PathVariable id: UUID) = service.get(id)
    @GetMapping("/{id}/versions") @PreAuthorize("hasAuthority('${Permissions.COURSES_READ}')") fun versions(@PathVariable id: UUID) = service.versions(id)
    @GetMapping("/{id}/versions/{version}") @PreAuthorize("hasAuthority('${Permissions.COURSES_READ}')") fun version(@PathVariable id: UUID, @PathVariable version: Int) = service.version(id, version)
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('${Permissions.COURSES_CREATE}','${Permissions.COURSES_WRITE}')") fun create(@Valid @RequestBody input: CourseRequest) = service.create(input)
    @PutMapping("/{id}") @PreAuthorize("hasAnyAuthority('${Permissions.COURSES_UPDATE}','${Permissions.COURSES_WRITE}')") fun update(@PathVariable id: UUID, @Valid @RequestBody input: CourseRequest) = service.update(id, input)
    @PostMapping("/{id}/lessons") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('${Permissions.COURSES_UPDATE}','${Permissions.COURSES_WRITE}')") fun addLesson(@PathVariable id: UUID, @Valid @RequestBody input: LessonRequest) = service.addLesson(id, input)
    @PutMapping("/{courseId}/lessons/{lessonId}") @PreAuthorize("hasAnyAuthority('${Permissions.COURSES_UPDATE}','${Permissions.COURSES_WRITE}')") fun updateLesson(@PathVariable courseId: UUID, @PathVariable lessonId: UUID, @Valid @RequestBody input: LessonRequest) = service.updateLesson(courseId, lessonId, input)
    @DeleteMapping("/{courseId}/lessons/{lessonId}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyAuthority('${Permissions.COURSES_UPDATE}','${Permissions.COURSES_WRITE}')") fun deleteLesson(@PathVariable courseId: UUID, @PathVariable lessonId: UUID) = service.deleteLesson(courseId, lessonId)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyAuthority('${Permissions.COURSES_UPDATE}','${Permissions.COURSES_WRITE}')") fun archive(@PathVariable id: UUID) = service.archive(id)
    @PostMapping("/{id}/status/{status}") @PreAuthorize("hasAuthority('${Permissions.COURSES_PUBLISH}')") fun transition(@PathVariable id: UUID, @PathVariable status: CourseStatus) = service.transition(id, status)
}

@RestController
@RequestMapping("/internal/v1/courses")
class InternalCourseController(private val service: CourseManagementService, private val internal: InternalTokenAuthorizer) {
    @GetMapping("/{id}/publication")
    fun publication(@PathVariable id: UUID, @RequestHeader("X-Service-Token", required=false) token: String?): PublicationStatus {
        internal.require(token); return service.publication(id)
    }

    @GetMapping("/{id}/learning-metadata")
    fun learningMetadata(
        @PathVariable id: UUID,
        @RequestParam(required = false) version: Int?,
        @RequestHeader("X-Service-Token", required=false) token: String?,
    ): CourseLearningMetadata {
        internal.require(token); return service.learningMetadata(id, version)
    }
}
