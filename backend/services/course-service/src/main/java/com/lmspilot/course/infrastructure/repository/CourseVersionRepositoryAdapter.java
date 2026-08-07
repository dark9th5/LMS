package com.lmspilot.course.infrastructure.repository;

import com.lmspilot.course.application.interfaces.repository.ICourseVersionRepository;
import com.lmspilot.course.domain.model.CourseVersion;
import com.lmspilot.course.infrastructure.persistence.jpaRepository.CourseVersionJpaRepository;
import com.lmspilot.course.infrastructure.persistence.mapper.CoursePersistenceMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CourseVersionRepositoryAdapter implements ICourseVersionRepository {
    private final CourseVersionJpaRepository jpa;
    private final CoursePersistenceMapper mapper;

    public CourseVersionRepositoryAdapter(CourseVersionJpaRepository jpa, CoursePersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    public CourseVersion save(CourseVersion version) { return mapper.toDomain(jpa.save(mapper.toEntity(version))); }
    public Optional<CourseVersion> findByCourseIdAndVersionNumber(UUID courseId, int versionNumber) {
        return jpa.findByCourseIdAndVersionNumber(courseId, versionNumber).map(mapper::toDomain);
    }
    public List<CourseVersion> findAllByCourseIdOrderByVersionNumberDesc(UUID courseId) {
        return jpa.findAllByCourseIdOrderByVersionNumberDesc(courseId).stream().map(mapper::toDomain).toList();
    }
}
