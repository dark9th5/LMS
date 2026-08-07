package com.lmspilot.course.infrastructure.persistence.jpaRepository;

import com.lmspilot.course.domain.enums.DiscussionPostStatus;
import com.lmspilot.course.infrastructure.persistence.entity.DiscussionPostEntity;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface DiscussionPostJpaRepository extends JpaRepository<DiscussionPostEntity,UUID> {
    List<DiscussionPostEntity> findAllByThreadIdAndStatusNotOrderByCreatedAtAsc(UUID threadId,DiscussionPostStatus status);
}
