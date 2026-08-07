package com.lmspilot.course.infrastructure.repository;

import com.lmspilot.course.application.interfaces.repository.ILessonRepository;
import com.lmspilot.course.domain.model.Lesson;
import com.lmspilot.course.infrastructure.persistence.jpaRepository.LessonJpaRepository;
import com.lmspilot.course.infrastructure.persistence.mapper.CoursePersistenceMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class LessonRepositoryAdapter implements ILessonRepository {
    private final LessonJpaRepository jpa;
    private final CoursePersistenceMapper mapper;

    public LessonRepositoryAdapter(LessonJpaRepository jpa, CoursePersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    public Lesson save(Lesson lesson) { return mapper.toDomain(jpa.save(mapper.toEntity(lesson))); }
    public Optional<Lesson> findById(UUID id) { return jpa.findById(id).map(mapper::toDomain); }
    public void delete(Lesson lesson) { jpa.delete(mapper.toEntity(lesson)); }
    public List<Lesson> findAllByCourseIdOrderBySortOrderAsc(UUID courseId) {
        return jpa.findAllByCourseIdOrderBySortOrderAsc(courseId).stream().map(mapper::toDomain).toList();
    }
    public long countByCourseId(UUID courseId) { return jpa.countByCourseId(courseId); }
}
