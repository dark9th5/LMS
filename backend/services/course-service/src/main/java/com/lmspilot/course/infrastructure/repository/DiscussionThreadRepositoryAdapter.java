package com.lmspilot.course.infrastructure.repository;

import com.lmspilot.course.application.interfaces.repository.IDiscussionThreadRepository;
import com.lmspilot.course.domain.enums.DiscussionThreadStatus;
import com.lmspilot.course.domain.model.DiscussionThread;
import com.lmspilot.course.infrastructure.persistence.jpaRepository.DiscussionThreadJpaRepository;
import com.lmspilot.course.infrastructure.persistence.mapper.CoursePersistenceMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class DiscussionThreadRepositoryAdapter implements IDiscussionThreadRepository {
    private final DiscussionThreadJpaRepository jpa;
    private final CoursePersistenceMapper mapper;

    public DiscussionThreadRepositoryAdapter(DiscussionThreadJpaRepository jpa, CoursePersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    public DiscussionThread save(DiscussionThread thread) { return mapper.toDomain(jpa.save(mapper.toEntity(thread))); }
    public Optional<DiscussionThread> findById(UUID id) { return jpa.findById(id).map(mapper::toDomain); }
    public List<DiscussionThread> findAllByCourseIdAndStatusNotOrderByPinnedDescUpdatedAtDesc(UUID courseId, DiscussionThreadStatus status) {
        return jpa.findAllByCourseIdAndStatusNotOrderByPinnedDescUpdatedAtDesc(courseId, status).stream().map(mapper::toDomain).toList();
    }
}
