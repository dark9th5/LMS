package com.lmspilot.course.application.dto.response;

import java.util.Set;
import java.util.UUID;

public record CourseDocumentScope(UUID courseId, Set<UUID> fileIds) {
}
