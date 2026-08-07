package com.lmspilot.course.application.dto.response;

import com.lmspilot.course.domain.enums.CourseStatus;
import java.util.UUID;

public record PublicationStatus(UUID courseId, CourseStatus status, int contentVersion, int publishedVersion,
                                boolean published) {
}
