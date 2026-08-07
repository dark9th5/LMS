package com.lmspilot.course.presentation.controller;

import com.lmspilot.course.application.dto.request.CourseRequest;
import com.lmspilot.course.application.dto.query.CourseVersionQuery;
import com.lmspilot.course.application.dto.response.CourseResponse;
import com.lmspilot.course.application.dto.response.CourseVersionSummary;
import com.lmspilot.course.application.dto.request.LessonRequest;
import com.lmspilot.course.application.dto.response.LessonResponse;
import com.lmspilot.course.application.dto.response.PageResponse;
import com.lmspilot.course.application.interfaces.service.ICourseService;
import com.lmspilot.course.application.interfaces.service.ILessonService;
import com.lmspilot.course.application.mapper.CourseDtoMapper;
import com.lmspilot.course.application.mapper.LessonDtoMapper;
import com.lmspilot.course.domain.enums.CourseStatus;

import jakarta.validation.Valid;

import java.util.*;

import org.springframework.http.*;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {
    private final ICourseService courses;
    private final ILessonService lessons;
    private final CourseDtoMapper courseMapper;
    private final LessonDtoMapper lessonMapper;

    public CourseController(ICourseService courses, ILessonService lessons, CourseDtoMapper courseMapper, LessonDtoMapper lessonMapper){
        this.courses=courses;
        this.lessons=lessons;
        this.courseMapper=courseMapper;
        this.lessonMapper=lessonMapper;
    }
    @GetMapping
    public PageResponse<CourseResponse> search(@RequestParam(required=false) String query,@RequestParam(required=false) CourseStatus status,@RequestParam(required=false) UUID categoryId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
        return courses.search(courseMapper.toSearchQuery(query,status,categoryId,page,size));
    }
    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable UUID id){
        return courses.get(id);
    }
    @GetMapping("/{id}/versions")
    public List<CourseVersionSummary> versions(@PathVariable UUID id){
        return courses.versions(id);
    }
    @GetMapping("/{id}/versions/{version}")
    public CourseResponse version(@PathVariable UUID id,@PathVariable int version){
        return courses.version(new CourseVersionQuery(id,version));
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid
    @RequestBody CourseRequest i){
        return courses.create(courseMapper.toUpsertCommand(i));
    }
    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable UUID id,@Valid
    @RequestBody CourseRequest i){
        return courses.update(id,courseMapper.toUpsertCommand(i));
    }
    @PostMapping("/{id}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResponse addLesson(@PathVariable UUID id,@Valid
    @RequestBody LessonRequest i){
        return lessons.addLesson(id,lessonMapper.toUpsertCommand(i));
    }
    @PutMapping("/{courseId}/lessons/{lessonId}")
    public LessonResponse updateLesson(@PathVariable UUID courseId,@PathVariable UUID lessonId,@Valid
    @RequestBody LessonRequest i){
        return lessons.updateLesson(courseId,lessonId,lessonMapper.toUpsertCommand(i));
    }
    @DeleteMapping("/{courseId}/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLesson(@PathVariable UUID courseId,@PathVariable UUID lessonId){
        lessons.deleteLesson(courseId,lessonId);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id){
        courses.archive(id);
    }
    @PostMapping("/{id}/status/{status}")
    public CourseResponse transition(@PathVariable UUID id,@PathVariable CourseStatus status){
        return courses.transition(id,courseMapper.toTransitionCommand(status));
    }

}
