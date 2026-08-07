package com.lmspilot.enrollment.domain;

import jakarta.persistence.*;

import java.time.*;

import java.util.*;
@Entity
@Table(name="course_assignments_v2")
public class CourseAssignmentEntity {
    @Id
    public UUID id=UUID.randomUUID();
    @Column(nullable=false)
    public UUID courseId;
    @Column(nullable=false)
    public UUID classId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    public AssignmentTargetType assigneeType=AssignmentTargetType.USER;
    @Column(nullable=false)
    public UUID assigneeId;
    @Column(nullable=false)
    public int assignedVersion=1;
    @Column(nullable=false)
    public Instant assignedAt=Instant.now();
    public Instant availableFrom;
    public Instant dueAt;
    @Column(nullable=false)
    public int gracePeriodMinutes;
    @Column(nullable=false)
    public boolean required=true;
    @Column(nullable=false)
    public UUID assignedBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    public CourseAssignmentStatus status=CourseAssignmentStatus.ACTIVE;
    public CourseAssignmentEntity(){
    }

}
