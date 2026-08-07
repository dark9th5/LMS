package com.lmspilot.course.application.mapper;

import com.lmspilot.course.application.dto.command.CourseCommand;
import com.lmspilot.course.application.dto.query.CourseSearchQuery;
import com.lmspilot.course.application.dto.request.CourseRequest;
import com.lmspilot.course.domain.enums.CourseStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CourseDtoMapper {
    public CourseCommand.Upsert toUpsertCommand(CourseRequest request) {
        return new CourseCommand.Upsert(request.code(), request.name(), request.description(), request.objectives(),
            request.targetAudience(), request.durationMinutes(), request.passingScore(),
            request.completionPolicyJson(), request.categoryId());
    }

    public CourseCommand.TransitionStatus toTransitionCommand(CourseStatus status) {
        return new CourseCommand.TransitionStatus(status);
    }

    public CourseSearchQuery toSearchQuery(String query, CourseStatus status, UUID categoryId, int page, int size) {
        return new CourseSearchQuery(query, status, categoryId, page, size);
    }
}
