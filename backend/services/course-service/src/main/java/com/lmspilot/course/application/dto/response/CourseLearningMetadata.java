package com.lmspilot.course.application.dto.response;

import com.lmspilot.course.domain.enums.CourseStatus;
import java.util.List;
import java.util.UUID;

public record CourseLearningMetadata(UUID courseId, int version, String code, String name, CourseStatus status,
                                     List<LessonResponse> lessons) {
}
