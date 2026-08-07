package com.lmspilot.course.application.interfaces.service;

import com.lmspilot.course.application.dto.command.LessonCommand;
import com.lmspilot.course.application.dto.response.LessonResponse;
import java.util.UUID;

public interface ILessonService {
    LessonResponse addLesson(UUID courseId, LessonCommand.Upsert command);
    LessonResponse updateLesson(UUID courseId, UUID lessonId, LessonCommand.Upsert command);
    void deleteLesson(UUID courseId, UUID lessonId);
}
