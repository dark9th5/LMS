package com.lmspilot.course.infrastructure.repository;

import com.lmspilot.course.application.interfaces.repository.ICourseRepository;
import com.lmspilot.course.domain.enums.CourseStatus;
import com.lmspilot.course.domain.model.Course;
import com.lmspilot.course.infrastructure.persistence.jpaRepository.CourseJpaRepository;
import com.lmspilot.course.infrastructure.persistence.mapper.CoursePersistenceMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class CourseRepositoryAdapter implements ICourseRepository {
    private final CourseJpaRepository jpa;
    private final CoursePersistenceMapper mapper;

    public CourseRepositoryAdapter(CourseJpaRepository jpa, CoursePersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    public long count() { return jpa.count(); }
    public Course save(Course course) { return mapper.toDomain(jpa.save(mapper.toEntity(course))); }
    public Optional<Course> findById(UUID id) { return jpa.findById(id).map(mapper::toDomain); }
    public boolean existsByCodeIgnoreCase(String code) { return jpa.existsByCodeIgnoreCase(code); }
    public Page<Course> search(String query, CourseStatus status, UUID categoryId, Pageable pageable) {
        return jpa.search(query, status, categoryId, pageable).map(mapper::toDomain);
    }
}
