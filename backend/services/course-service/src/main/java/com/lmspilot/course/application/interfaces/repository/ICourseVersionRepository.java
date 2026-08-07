package com.lmspilot.course.application.interfaces.repository;

import com.lmspilot.course.domain.model.CourseVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICourseVersionRepository {
    CourseVersion save(CourseVersion version);
    Optional<CourseVersion> findByCourseIdAndVersionNumber(UUID courseId, int versionNumber);
    List<CourseVersion> findAllByCourseIdOrderByVersionNumberDesc(UUID courseId);
}
