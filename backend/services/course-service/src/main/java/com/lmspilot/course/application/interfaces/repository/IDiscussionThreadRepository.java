package com.lmspilot.course.application.interfaces.repository;

import com.lmspilot.course.domain.enums.DiscussionThreadStatus;
import com.lmspilot.course.domain.model.DiscussionThread;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IDiscussionThreadRepository {
    DiscussionThread save(DiscussionThread thread);
    Optional<DiscussionThread> findById(UUID id);
    List<DiscussionThread> findAllByCourseIdAndStatusNotOrderByPinnedDescUpdatedAtDesc(UUID courseId, DiscussionThreadStatus status);
}
