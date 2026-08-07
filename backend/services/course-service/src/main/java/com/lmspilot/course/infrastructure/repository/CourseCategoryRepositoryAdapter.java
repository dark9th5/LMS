package com.lmspilot.course.infrastructure.repository;

import com.lmspilot.course.application.interfaces.repository.ICourseCategoryRepository;
import com.lmspilot.course.domain.model.CourseCategory;
import com.lmspilot.course.infrastructure.persistence.jpaRepository.CourseCategoryJpaRepository;
import com.lmspilot.course.infrastructure.persistence.mapper.CoursePersistenceMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CourseCategoryRepositoryAdapter implements ICourseCategoryRepository {
    private final CourseCategoryJpaRepository jpa;
    private final CoursePersistenceMapper mapper;

    public CourseCategoryRepositoryAdapter(CourseCategoryJpaRepository jpa, CoursePersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    public long count() { return jpa.count(); }
    public CourseCategory save(CourseCategory category) { return mapper.toDomain(jpa.save(mapper.toEntity(category))); }
    public Optional<CourseCategory> findById(UUID id) { return jpa.findById(id).map(mapper::toDomain); }
    public boolean existsByCodeIgnoreCase(String code) { return jpa.existsByCodeIgnoreCase(code); }
    public List<CourseCategory> findAllByOrderBySortOrderAscNameAsc() {
        return jpa.findAllByOrderBySortOrderAscNameAsc().stream().map(mapper::toDomain).toList();
    }
}
