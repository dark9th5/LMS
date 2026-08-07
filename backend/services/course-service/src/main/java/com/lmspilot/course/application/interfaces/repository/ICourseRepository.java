package com.lmspilot.course.application.interfaces.repository;

import com.lmspilot.course.domain.enums.CourseStatus;
import com.lmspilot.course.domain.model.Course;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICourseRepository {
    long count();
    Course save(Course course);
    Optional<Course> findById(UUID id);
    boolean existsByCodeIgnoreCase(String code);
    Page<Course> search(String query, CourseStatus status, UUID categoryId, Pageable pageable);
}
