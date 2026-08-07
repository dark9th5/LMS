package com.lmspilot.course.infrastructure.repository;

import com.lmspilot.course.application.interfaces.repository.IDiscussionPostRepository;
import com.lmspilot.course.domain.enums.DiscussionPostStatus;
import com.lmspilot.course.domain.model.DiscussionPost;
import com.lmspilot.course.infrastructure.persistence.jpaRepository.DiscussionPostJpaRepository;
import com.lmspilot.course.infrastructure.persistence.mapper.CoursePersistenceMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class DiscussionPostRepositoryAdapter implements IDiscussionPostRepository {
    private final DiscussionPostJpaRepository jpa;
    private final CoursePersistenceMapper mapper;

    public DiscussionPostRepositoryAdapter(DiscussionPostJpaRepository jpa, CoursePersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    public DiscussionPost save(DiscussionPost post) { return mapper.toDomain(jpa.save(mapper.toEntity(post))); }
    public Optional<DiscussionPost> findById(UUID id) { return jpa.findById(id).map(mapper::toDomain); }
    public List<DiscussionPost> findAllByThreadIdAndStatusNotOrderByCreatedAtAsc(UUID threadId, DiscussionPostStatus status) {
        return jpa.findAllByThreadIdAndStatusNotOrderByCreatedAtAsc(threadId, status).stream().map(mapper::toDomain).toList();
    }
}
