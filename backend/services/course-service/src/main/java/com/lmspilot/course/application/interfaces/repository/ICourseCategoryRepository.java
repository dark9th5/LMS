package com.lmspilot.course.application.interfaces.repository;

import com.lmspilot.course.domain.model.CourseCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICourseCategoryRepository {
    long count();
    CourseCategory save(CourseCategory category);
    Optional<CourseCategory> findById(UUID id);
    boolean existsByCodeIgnoreCase(String code);
    List<CourseCategory> findAllByOrderBySortOrderAscNameAsc();
}
