package com.lmspilot.course.application.dto.query;

import java.util.UUID;

public record CourseVersionQuery(UUID courseId, int version) {
}
