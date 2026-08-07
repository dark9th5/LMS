package com.lmspilot.course.domain;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface CourseVersionRepository extends JpaRepository<CourseVersionEntity,UUID> {
    Optional<CourseVersionEntity> findByCourseIdAndVersionNumber(UUID courseId,int versionNumber);
    List<CourseVersionEntity> findAllByCourseIdOrderByVersionNumberDesc(UUID courseId);
}
