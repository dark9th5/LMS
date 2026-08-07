package com.lmspilot.competency.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;
public interface CourseCompetencyMapRepository extends JpaRepository<CourseCompetencyMapEntity,UUID>{
    List<CourseCompetencyMapEntity> findAllByCompetencyId(UUID competencyId);
    CourseCompetencyMapEntity findByCourseIdAndCompetencyId(UUID courseId,UUID competencyId);
}
