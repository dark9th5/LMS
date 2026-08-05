package com.lmspilot.course.domain; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface DiscussionThreadRepository extends JpaRepository<DiscussionThreadEntity,UUID> { List<DiscussionThreadEntity> findAllByCourseIdAndStatusNotOrderByPinnedDescUpdatedAtDesc(UUID courseId,DiscussionThreadStatus status); }
