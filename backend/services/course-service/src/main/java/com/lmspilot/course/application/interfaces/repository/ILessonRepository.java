package com.lmspilot.course.application.interfaces.repository;

import com.lmspilot.course.domain.model.Lesson;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ILessonRepository {
    Lesson save(Lesson lesson);
    Optional<Lesson> findById(UUID id);
    void delete(Lesson lesson);
    List<Lesson> findAllByCourseIdOrderBySortOrderAsc(UUID courseId);
    long countByCourseId(UUID courseId);
}
