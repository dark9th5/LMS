package com.lmspilot.course.application.interfaces.service;

import com.lmspilot.course.application.dto.response.CourseDocumentScope;
import com.lmspilot.course.application.dto.response.CourseLearningMetadata;
import com.lmspilot.course.application.dto.response.PublicationStatus;
import java.util.UUID;

public interface ICourseInternalQueryService {
    PublicationStatus publication(UUID id);
    CourseDocumentScope documentScope(UUID id);
    CourseLearningMetadata learningMetadata(UUID id, Integer version);
}
