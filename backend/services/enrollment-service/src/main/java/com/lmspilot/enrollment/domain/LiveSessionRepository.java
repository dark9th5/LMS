package com.lmspilot.enrollment.domain; import java.time.*; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface LiveSessionRepository extends JpaRepository<LiveSessionEntity,UUID>{ List<LiveSessionEntity> findAllByCourseIdAndEndsAtAfterOrderByStartsAtAsc(UUID courseId,Instant after); }
