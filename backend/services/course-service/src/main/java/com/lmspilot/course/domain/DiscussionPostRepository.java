package com.lmspilot.course.domain; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface DiscussionPostRepository extends JpaRepository<DiscussionPostEntity,UUID> { List<DiscussionPostEntity> findAllByThreadIdAndStatusNotOrderByCreatedAtAsc(UUID threadId,DiscussionPostStatus status); }
