package com.lmspilot.enrollment.domain;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface CourseAssignmentRepository extends JpaRepository<CourseAssignmentEntity,UUID>{
    List<CourseAssignmentEntity> findAllByAssigneeTypeAndAssigneeIdAndStatusOrderByAssignedAtDesc(AssignmentTargetType type,UUID id,CourseAssignmentStatus status);
    List<CourseAssignmentEntity> findAllByCourseIdOrderByAssignedAtDesc(UUID courseId);
}
