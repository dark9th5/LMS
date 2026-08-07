package com.lmspilot.course.application.interfaces.repository;

import com.lmspilot.course.domain.enums.DiscussionPostStatus;
import com.lmspilot.course.domain.model.DiscussionPost;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IDiscussionPostRepository {
    DiscussionPost save(DiscussionPost post);
    Optional<DiscussionPost> findById(UUID id);
    List<DiscussionPost> findAllByThreadIdAndStatusNotOrderByCreatedAtAsc(UUID threadId, DiscussionPostStatus status);
}
