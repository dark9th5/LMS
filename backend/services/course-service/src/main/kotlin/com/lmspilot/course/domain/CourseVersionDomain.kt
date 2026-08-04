package com.lmspilot.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

/** Immutable serialized course content used by active classes and historical reports. */
@Entity
@Table(
    name = "course_versions",
    uniqueConstraints = [UniqueConstraint(name = "uq_course_version_number", columnNames = ["course_id", "version_number"])],
    indexes = [Index(name = "idx_course_version_course", columnList = "course_id,version_number")],
)
class CourseVersionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "course_id", nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(name = "version_number", nullable = false) var versionNumber: Int = 1,
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "text") var snapshotJson: String = "{}",
    @Column(name = "created_by", nullable = false) var createdBy: UUID = UUID.randomUUID(),
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
)

interface CourseVersionRepository : org.springframework.data.jpa.repository.JpaRepository<CourseVersionEntity, UUID> {
    fun findByCourseIdAndVersionNumber(courseId: UUID, versionNumber: Int): CourseVersionEntity?
    fun findAllByCourseIdOrderByVersionNumberDesc(courseId: UUID): List<CourseVersionEntity>
}
