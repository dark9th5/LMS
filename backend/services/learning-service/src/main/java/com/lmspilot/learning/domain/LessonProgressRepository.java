package com.lmspilot.learning.domain; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface LessonProgressRepository extends JpaRepository<LessonProgressEntity,UUID>{ Optional<LessonProgressEntity> findByEnrollmentIdAndLessonId(UUID enrollmentId,UUID lessonId); List<LessonProgressEntity> findAllByEnrollmentIdOrderByUpdatedAtAsc(UUID enrollmentId); }
