package com.lmspilot.course.infrastructure.persistence.jpaRepository;

import com.lmspilot.course.domain.enums.DiscussionThreadStatus;
import com.lmspilot.course.infrastructure.persistence.entity.DiscussionThreadEntity;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface DiscussionThreadJpaRepository extends JpaRepository<DiscussionThreadEntity,UUID> {
    List<DiscussionThreadEntity> findAllByCourseIdAndStatusNotOrderByPinnedDescUpdatedAtDesc(UUID courseId,DiscussionThreadStatus status);
}
