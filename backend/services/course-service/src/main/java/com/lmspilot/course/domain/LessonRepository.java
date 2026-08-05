package com.lmspilot.course.domain; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface LessonRepository extends JpaRepository<LessonEntity,UUID> { List<LessonEntity> findAllByCourseIdOrderBySortOrderAsc(UUID courseId); long countByCourseId(UUID courseId); void deleteAllByCourseId(UUID courseId); }
