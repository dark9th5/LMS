package com.lmspilot.enrollment.domain;

import jakarta.persistence.*;

import java.time.*;

import java.util.*;
@Entity
@Table(name="learning_path_assignments")
public class LearningPathAssignmentEntity {
    @Id
    public UUID id=UUID.randomUUID();
    @Column(name="path_id",nullable=false)
    public UUID pathId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    public AssignmentTargetType assigneeType=AssignmentTargetType.USER;
    @Column(nullable=false)
    public UUID assigneeId;
    public Instant dueAt;
    @Column(nullable=false)
    public UUID assignedBy;
    @Column(nullable=false)
    public Instant assignedAt=Instant.now();
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    public LearningPathAssignmentStatus status=LearningPathAssignmentStatus.ACTIVE;
    public LearningPathAssignmentEntity(){
    }

}
