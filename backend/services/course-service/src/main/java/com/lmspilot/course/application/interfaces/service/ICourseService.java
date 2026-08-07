package com.lmspilot.course.application.interfaces.service;

import com.lmspilot.course.application.dto.command.CourseCommand;
import com.lmspilot.course.application.dto.query.CourseSearchQuery;
import com.lmspilot.course.application.dto.query.CourseVersionQuery;
import com.lmspilot.course.application.dto.response.CourseResponse;
import com.lmspilot.course.application.dto.response.CourseVersionSummary;
import com.lmspilot.course.application.dto.response.PageResponse;
import java.util.List;
import java.util.UUID;

public interface ICourseService {
    PageResponse<CourseResponse> search(CourseSearchQuery query);
    CourseResponse get(UUID id);
    CourseResponse create(CourseCommand.Upsert command);
    CourseResponse update(UUID id, CourseCommand.Upsert command);
    void archive(UUID id);
    CourseResponse transition(UUID id, CourseCommand.TransitionStatus command);
    List<CourseVersionSummary> versions(UUID id);
    CourseResponse version(CourseVersionQuery query);
}
