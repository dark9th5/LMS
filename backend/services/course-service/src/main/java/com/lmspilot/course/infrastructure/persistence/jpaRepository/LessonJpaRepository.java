package com.lmspilot.course.infrastructure.persistence.jpaRepository;

import com.lmspilot.course.infrastructure.persistence.entity.LessonEntity;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonJpaRepository extends JpaRepository<LessonEntity, UUID> {
    List<LessonEntity> findAllByCourseIdOrderBySortOrderAsc(UUID courseId);

    long countByCourseId(UUID courseId);

    void deleteAllByCourseId(UUID courseId);
}
