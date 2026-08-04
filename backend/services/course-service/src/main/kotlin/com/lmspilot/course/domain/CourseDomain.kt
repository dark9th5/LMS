package com.lmspilot.course.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class RecordStatus { ACTIVE, INACTIVE }
enum class CourseStatus { DRAFT, PUBLISHED, HIDDEN, ARCHIVED }
enum class LessonType { TEXT, PDF, DOCX, VIDEO, AUDIO, FILE, ASSIGNMENT, EXAM }

@Entity
@Table(name = "course_categories")
class CourseCategoryEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 80) var code: String = "",
    @Column(nullable = false, length = 180) var name: String = "",
    var parentId: UUID? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: RecordStatus = RecordStatus.ACTIVE,
    @Column(nullable = false) var sortOrder: Int = 0,
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

@Entity
@Table(name = "courses")
class CourseEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 80) var code: String = "",
    @Column(nullable = false, length = 240) var name: String = "",
    @Column(columnDefinition = "text") var description: String? = null,
    @Column(columnDefinition = "text") var objectives: String? = null,
    @Column(length = 500) var targetAudience: String? = null,
    var durationMinutes: Int? = null,
    @Column(nullable = false) var passingScore: Double = 70.0,
    @Column(nullable = false, columnDefinition = "text") var completionPolicyJson: String = "{\"requiredLessonPercent\":100}",
    var categoryId: UUID? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: CourseStatus = CourseStatus.DRAFT,
    @Column(nullable = false) var contentVersion: Int = 1,
    @Column(nullable = false) var publishedVersion: Int = 0,
    var publishedAt: Instant? = null,
    var publishedBy: UUID? = null,
    @Column(nullable = false) var ownerId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var rowVersion: Long = 0,
)

@Entity
@Table(name = "lessons", uniqueConstraints = [UniqueConstraint(name = "uq_lesson_course_order", columnNames = ["course_id", "sort_order"])])
class LessonEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "course_id", nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 220) var title: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var type: LessonType = LessonType.TEXT,
    @Column(columnDefinition = "text") var textContent: String? = null,
    var fileId: UUID? = null,
    @Column(nullable = false) var required: Boolean = true,
    @Column(nullable = false) var sortOrder: Int = 0,
    @Column(nullable = false) var estimatedMinutes: Int = 0,
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

interface CourseCategoryRepository : org.springframework.data.jpa.repository.JpaRepository<CourseCategoryEntity, UUID> {
    fun existsByCodeIgnoreCase(code: String): Boolean
}

interface CourseRepository : org.springframework.data.jpa.repository.JpaRepository<CourseEntity, UUID> {
    fun existsByCodeIgnoreCase(code: String): Boolean

    @org.springframework.data.jpa.repository.Query("""
        select c from CourseEntity c
        where (:query is null or lower(c.code) like lower(concat('%', cast(:query as string), '%')) or lower(c.name) like lower(concat('%', cast(:query as string), '%')))
          and (:status is null or c.status = :status)
          and (:categoryId is null or c.categoryId = :categoryId)
          and (:ownerId is null or c.ownerId = :ownerId)
    """)
    fun search(query: String?, status: CourseStatus?, categoryId: UUID?, ownerId: UUID?, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<CourseEntity>

    @org.springframework.data.jpa.repository.Query("""
        select c from CourseEntity c
        where (:query is null or lower(c.code) like lower(concat('%', cast(:query as string), '%')) or lower(c.name) like lower(concat('%', cast(:query as string), '%')))
          and (:status is null or c.status = :status)
          and (:categoryId is null or c.categoryId = :categoryId)
          and (:ownerId is null or c.ownerId = :ownerId or c.id in :assignedCourseIds)
          and (:includeArchived = true or c.status <> :archivedStatus)
    """)
    fun searchVisible(query: String?, status: CourseStatus?, categoryId: UUID?, ownerId: UUID?, assignedCourseIds: Set<UUID>, includeArchived: Boolean, archivedStatus: CourseStatus, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<CourseEntity>
}

interface LessonRepository : org.springframework.data.jpa.repository.JpaRepository<LessonEntity, UUID> {
    fun findAllByCourseIdOrderBySortOrderAsc(courseId: UUID): List<LessonEntity>
    fun countByCourseId(courseId: UUID): Long
}

enum class DiscussionThreadStatus { OPEN, LOCKED, HIDDEN }

enum class DiscussionPostStatus { VISIBLE, HIDDEN, DELETED }

@Entity
@Table(name = "discussion_threads", indexes = [Index(name = "idx_discussion_thread_course", columnList = "course_id,updated_at")])
class DiscussionThreadEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "course_id", nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(name = "lesson_id") var lessonId: UUID? = null,
    @Column(nullable = false, length = 240) var title: String = "",
    @Column(name = "author_id", nullable = false) var authorId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: DiscussionThreadStatus = DiscussionThreadStatus.OPEN,
    @Column(nullable = false) var pinned: Boolean = false,
    @Column(name = "post_count", nullable = false) var postCount: Int = 0,
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

@Entity
@Table(name = "discussion_posts", indexes = [Index(name = "idx_discussion_post_thread", columnList = "thread_id,created_at")])
class DiscussionPostEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "thread_id", nullable = false) var threadId: UUID = UUID.randomUUID(),
    @Column(name = "author_id", nullable = false) var authorId: UUID = UUID.randomUUID(),
    @Column(name = "parent_post_id") var parentPostId: UUID? = null,
    @Column(nullable = false, columnDefinition = "text") var content: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: DiscussionPostStatus = DiscussionPostStatus.VISIBLE,
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

interface DiscussionThreadRepository : org.springframework.data.jpa.repository.JpaRepository<DiscussionThreadEntity, UUID> {
    fun findAllByCourseIdAndStatusNotOrderByPinnedDescUpdatedAtDesc(courseId: UUID, status: DiscussionThreadStatus): List<DiscussionThreadEntity>
}

interface DiscussionPostRepository : org.springframework.data.jpa.repository.JpaRepository<DiscussionPostEntity, UUID> {
    fun findAllByThreadIdAndStatusNotOrderByCreatedAtAsc(threadId: UUID, status: DiscussionPostStatus): List<DiscussionPostEntity>
}
