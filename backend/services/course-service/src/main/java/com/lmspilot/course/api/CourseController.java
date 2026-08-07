package com.lmspilot.course.api;

import com.lmspilot.course.domain.*;

import jakarta.validation.Valid;

import java.util.*;

import org.springframework.http.*;

import org.springframework.web.bind.annotation.*;

import static com.lmspilot.course.api.CourseModels.*;
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {
    private final CourseManagementService service;
    public CourseController(CourseManagementService s){
        service=s;
    }
    @GetMapping
    public PageResponse<CourseResponse> search(@RequestParam(required=false) String query,@RequestParam(required=false) CourseStatus status,@RequestParam(required=false) UUID categoryId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
        return service.search(query,status,categoryId,page,size);
    }
    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable UUID id){
        return service.get(id);
    }
    @GetMapping("/{id}/versions")
    public List<CourseVersionSummary> versions(@PathVariable UUID id){
        return service.versions(id);
    }
    @GetMapping("/{id}/versions/{version}")
    public CourseResponse version(@PathVariable UUID id,@PathVariable int version){
        return service.version(id,version);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid
    @RequestBody CourseRequest i){
        return service.create(i);
    }
    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable UUID id,@Valid
    @RequestBody CourseRequest i){
        return service.update(id,i);
    }
    @PostMapping("/{id}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResponse addLesson(@PathVariable UUID id,@Valid
    @RequestBody LessonRequest i){
        return service.addLesson(id,i);
    }
    @PutMapping("/{courseId}/lessons/{lessonId}")
    public LessonResponse updateLesson(@PathVariable UUID courseId,@PathVariable UUID lessonId,@Valid
    @RequestBody LessonRequest i){
        return service.updateLesson(courseId,lessonId,i);
    }
    @DeleteMapping("/{courseId}/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLesson(@PathVariable UUID courseId,@PathVariable UUID lessonId){
        service.deleteLesson(courseId,lessonId);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id){
        service.archive(id);
    }
    @PostMapping("/{id}/status/{status}")
    public CourseResponse transition(@PathVariable UUID id,@PathVariable CourseStatus status){
        return service.transition(id,status);
    }

}
