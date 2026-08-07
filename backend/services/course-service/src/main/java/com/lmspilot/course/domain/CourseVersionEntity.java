package com.lmspilot.course.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="course_versions",uniqueConstraints=@UniqueConstraint(name="uq_course_version_number",columnNames={
    "course_id","version_number"
}
),indexes=@Index(name="idx_course_version_course",columnList="course_id,version_number"))
public class CourseVersionEntity {
    @Id
    public UUID id=UUID.randomUUID();
    @Column(name="course_id",nullable=false)
    public UUID courseId;
    @Column(name="version_number",nullable=false)
    public int versionNumber=1;
    @Column(name="snapshot_json",nullable=false,columnDefinition="text")
    public String snapshotJson="{}";
    @Column(name="created_by",nullable=false)
    public UUID createdBy;
    @Column(name="created_at",nullable=false)
    public Instant createdAt=Instant.now();
    public CourseVersionEntity() {
    }

}
