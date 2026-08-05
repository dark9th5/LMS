package com.lmspilot.course.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.course.domain.CourseRepository
import com.lmspilot.course.domain.CourseStatus
import com.lmspilot.course.domain.DiscussionPostEntity
import com.lmspilot.course.domain.DiscussionPostRepository
import com.lmspilot.course.domain.DiscussionPostStatus
import com.lmspilot.course.domain.DiscussionThreadEntity
import com.lmspilot.course.domain.DiscussionThreadRepository
import com.lmspilot.course.domain.DiscussionThreadStatus
import com.lmspilot.course.domain.LessonRepository
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class CreateDiscussionThreadRequest(
    val lessonId: UUID? = null,
    @field:NotBlank @field:Size(min = 3, max = 240) val title: String,
    @field:NotBlank @field:Size(min = 1, max = 20000) val content: String,
)
data class CreateDiscussionPostRequest(
    val parentPostId: UUID? = null,
    @field:NotBlank @field:Size(min = 1, max = 20000) val content: String,
)
data class ModerateDiscussionThreadRequest(val status: DiscussionThreadStatus, val pinned: Boolean? = null)
data class DiscussionPostResponse(val id: UUID, val authorId: UUID, val parentPostId: UUID?, val content: String, val status: DiscussionPostStatus, val createdAt: Instant, val updatedAt: Instant)
data class DiscussionThreadResponse(val id: UUID, val courseId: UUID, val lessonId: UUID?, val title: String, val authorId: UUID, val status: DiscussionThreadStatus, val pinned: Boolean, val postCount: Int, val createdAt: Instant, val updatedAt: Instant, val posts: List<DiscussionPostResponse> = emptyList())

@Service
class DiscussionService(
    private val courses: CourseRepository,
    private val lessons: LessonRepository,
    private val threads: DiscussionThreadRepository,
    private val posts: DiscussionPostRepository,
    private val enrollmentScope: EnrollmentCourseScopeClient,
) {
    @Transactional(readOnly = true)
    fun threads(courseId: UUID): List<DiscussionThreadResponse> {
        requireRead(courseId)
        return threads.findAllByCourseIdAndStatusNotOrderByPinnedDescUpdatedAtDesc(courseId, DiscussionThreadStatus.HIDDEN).map { it.response() }
    }

    @Transactional(readOnly = true)
    fun thread(id: UUID): DiscussionThreadResponse {
        val thread = findThread(id)
        requireRead(thread.courseId)
        val visiblePosts = posts.findAllByThreadIdAndStatusNotOrderByCreatedAtAsc(id, DiscussionPostStatus.DELETED)
            .filter { it.status == DiscussionPostStatus.VISIBLE || canModerate(thread.courseId) || it.authorId == CurrentUser.id() }
            .map { it.response() }
        return thread.response(visiblePosts)
    }

    @Transactional
    fun createThread(courseId: UUID, input: CreateDiscussionThreadRequest): DiscussionThreadResponse {
        requireWrite(courseId)
        input.lessonId?.let { lessonId ->
            val lesson = lessons.findById(lessonId).orElseThrow { ApiException(HttpStatus.BAD_REQUEST, "LESSON_NOT_FOUND", "Không tìm thấy bài học") }
            if (lesson.courseId != courseId) throw ApiException(HttpStatus.BAD_REQUEST, "LESSON_COURSE_MISMATCH", "Bài học không thuộc khóa học")
        }
        val now = Instant.now()
        val thread = threads.save(DiscussionThreadEntity(courseId = courseId, lessonId = input.lessonId, title = input.title.trim(), authorId = CurrentUser.id(), postCount = 1, createdAt = now, updatedAt = now))
        posts.save(DiscussionPostEntity(threadId = thread.id, authorId = CurrentUser.id(), content = input.content.trim(), createdAt = now, updatedAt = now))
        return thread.response()
    }

    @Transactional
    fun reply(threadId: UUID, input: CreateDiscussionPostRequest): DiscussionPostResponse {
        val thread = findThread(threadId)
        requireWrite(thread.courseId)
        if (thread.status != DiscussionThreadStatus.OPEN) throw ApiException(HttpStatus.CONFLICT, "THREAD_LOCKED", "Chủ đề đã khóa")
        input.parentPostId?.let { parentId ->
            val parent = posts.findById(parentId).orElseThrow { ApiException(HttpStatus.BAD_REQUEST, "PARENT_POST_NOT_FOUND", "Không tìm thấy bài viết cha") }
            if (parent.threadId != threadId || parent.status == DiscussionPostStatus.DELETED) throw ApiException(HttpStatus.BAD_REQUEST, "PARENT_POST_INVALID", "Bài viết cha không hợp lệ")
        }
        val post = posts.save(DiscussionPostEntity(threadId = threadId, authorId = CurrentUser.id(), parentPostId = input.parentPostId, content = input.content.trim()))
        thread.postCount += 1
        thread.updatedAt = Instant.now()
        return post.response()
    }

    @Transactional
    fun moderateThread(id: UUID, input: ModerateDiscussionThreadRequest): DiscussionThreadResponse {
        val thread = findThread(id)
        requireModerate(thread.courseId)
        thread.status = input.status
        input.pinned?.let { thread.pinned = it }
        thread.updatedAt = Instant.now()
        return thread.response()
    }

    @Transactional
    fun deletePost(id: UUID) {
        val post = posts.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Không tìm thấy bài viết") }
        val thread = findThread(post.threadId)
        if (post.authorId != CurrentUser.id() && !canModerate(thread.courseId)) {
            throw ApiException(HttpStatus.FORBIDDEN, "DISCUSSION_OUT_OF_SCOPE", "Không thể xóa bài viết này")
        }
        post.status = DiscussionPostStatus.DELETED
        post.content = "[Đã xóa]"
        post.updatedAt = Instant.now()
        thread.updatedAt = Instant.now()
    }

    private fun requireRead(courseId: UUID) {
        val course = courses.findById(courseId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "Không tìm thấy khóa học") }
        val userId = CurrentUser.id()
        val allowed = course.ownerId == userId || Permissions.DISCUSSIONS_MODERATE in CurrentUser.authorities() ||
            (course.status in setOf(CourseStatus.PUBLISHED, CourseStatus.HIDDEN) && courseId in enrollmentScope.activeCourseIds(userId)) ||
            courseId in enrollmentScope.assignedCourseIds(userId)
        if (!allowed) throw ApiException(HttpStatus.FORBIDDEN, "DISCUSSION_OUT_OF_SCOPE", "Thảo luận ngoài phạm vi truy cập")
    }

    private fun requireWrite(courseId: UUID) {
        if (Permissions.DISCUSSIONS_WRITE !in CurrentUser.authorities()) throw ApiException(HttpStatus.FORBIDDEN, "DISCUSSION_WRITE_DENIED", "Không có quyền viết thảo luận")
        requireRead(courseId)
    }

    private fun canModerate(courseId: UUID): Boolean {
        val course = courses.findById(courseId).orElse(null) ?: return false
        return course.ownerId == CurrentUser.id() || Permissions.DISCUSSIONS_MODERATE in CurrentUser.authorities()
    }

    private fun requireModerate(courseId: UUID) {
        if (!canModerate(courseId)) throw ApiException(HttpStatus.FORBIDDEN, "DISCUSSION_MODERATE_DENIED", "Không có quyền kiểm duyệt thảo luận")
    }

    private fun findThread(id: UUID) = threads.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "THREAD_NOT_FOUND", "Không tìm thấy chủ đề") }
}

private fun DiscussionThreadEntity.response(posts: List<DiscussionPostResponse> = emptyList()) = DiscussionThreadResponse(id, courseId, lessonId, title, authorId, status, pinned, postCount, createdAt, updatedAt, posts)
private fun DiscussionPostEntity.response() = DiscussionPostResponse(id, authorId, parentPostId, content, status, createdAt, updatedAt)

@RestController
@RequestMapping("/api/v1/discussions")
class DiscussionController(private val service: DiscussionService) {
    @GetMapping("/courses/{courseId}/threads")
    @PreAuthorize("hasAuthority('${Permissions.DISCUSSIONS_READ}')")
    fun threads(@PathVariable courseId: UUID) = service.threads(courseId)

    @PostMapping("/courses/{courseId}/threads")
    @PreAuthorize("hasAuthority('${Permissions.DISCUSSIONS_WRITE}')")
    fun createThread(@PathVariable courseId: UUID, @Valid @RequestBody input: CreateDiscussionThreadRequest) = service.createThread(courseId, input)

    @GetMapping("/threads/{id}")
    @PreAuthorize("hasAuthority('${Permissions.DISCUSSIONS_READ}')")
    fun thread(@PathVariable id: UUID) = service.thread(id)

    @PostMapping("/threads/{id}/posts")
    @PreAuthorize("hasAuthority('${Permissions.DISCUSSIONS_WRITE}')")
    fun reply(@PathVariable id: UUID, @Valid @RequestBody input: CreateDiscussionPostRequest) = service.reply(id, input)

    @PatchMapping("/threads/{id}")
    @PreAuthorize("hasAuthority('${Permissions.DISCUSSIONS_MODERATE}')")
    fun moderate(@PathVariable id: UUID, @RequestBody input: ModerateDiscussionThreadRequest) = service.moderateThread(id, input)

    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasAuthority('${Permissions.DISCUSSIONS_WRITE}')")
    fun deletePost(@PathVariable id: UUID) = service.deletePost(id)
}
