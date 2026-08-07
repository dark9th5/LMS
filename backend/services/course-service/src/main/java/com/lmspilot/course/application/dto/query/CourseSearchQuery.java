package com.lmspilot.course.application.dto.query;

import com.lmspilot.course.domain.enums.CourseStatus;
import java.util.UUID;

public record CourseSearchQuery(String query, CourseStatus status, UUID categoryId, int page, int size) {
}
