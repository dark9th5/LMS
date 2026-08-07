package com.lmspilot.course.application.mapper;

import com.lmspilot.course.application.dto.command.LessonCommand;
import com.lmspilot.course.application.dto.request.LessonRequest;
import org.springframework.stereotype.Component;

@Component
public class LessonDtoMapper {
    public LessonCommand.Upsert toUpsertCommand(LessonRequest request) {
        return new LessonCommand.Upsert(request.title(), request.type(), request.textContent(), request.fileId(),
            request.required(), request.sortOrder(), request.estimatedMinutes());
    }
}
