package com.lmspilot.course.infrastructure.persistence.jpaRepository;

import com.lmspilot.course.infrastructure.persistence.entity.CourseVersionEntity;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface CourseVersionJpaRepository extends JpaRepository<CourseVersionEntity,UUID> {
    Optional<CourseVersionEntity> findByCourseIdAndVersionNumber(UUID courseId,int versionNumber);
    List<CourseVersionEntity> findAllByCourseIdOrderByVersionNumberDesc(UUID courseId);
}
