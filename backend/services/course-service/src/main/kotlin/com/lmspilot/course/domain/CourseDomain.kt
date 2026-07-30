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
        where (:query is null or lower(c.code) like lower(concat('%', :query, '%')) or lower(c.name) like lower(concat('%', :query, '%')))
          and (:status is null or c.status = :status)
          and (:categoryId is null or c.categoryId = :categoryId)
          and (:ownerId is null or c.ownerId = :ownerId)
    """)
    fun search(query: String?, status: CourseStatus?, categoryId: UUID?, ownerId: UUID?, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<CourseEntity>

    @org.springframework.data.jpa.repository.Query("""
        select c from CourseEntity c
        where (:query is null or lower(c.code) like lower(concat('%', :query, '%')) or lower(c.name) like lower(concat('%', :query, '%')))
          and (:status is null or c.status = :status)
          and (:categoryId is null or c.categoryId = :categoryId)
          and (:ownerId is null or c.ownerId = :ownerId or c.id in :assignedCourseIds)
    """)
    fun searchVisible(query: String?, status: CourseStatus?, categoryId: UUID?, ownerId: UUID?, assignedCourseIds: Set<UUID>, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<CourseEntity>
}

interface LessonRepository : org.springframework.data.jpa.repository.JpaRepository<LessonEntity, UUID> {
    fun findAllByCourseIdOrderBySortOrderAsc(courseId: UUID): List<LessonEntity>
    fun countByCourseId(courseId: UUID): Long
}
