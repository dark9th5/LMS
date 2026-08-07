package com.lmspilot.course.api;

import com.lmspilot.support.security.InternalTokenAuthorizer;

import java.util.*;

import org.springframework.web.bind.annotation.*;

import static com.lmspilot.course.api.CourseModels.*;
@RestController
@RequestMapping("/internal/v1/courses")
public class InternalCourseController {
    private final CourseManagementService service;
    private final InternalTokenAuthorizer internal;
    public InternalCourseController(CourseManagementService s,InternalTokenAuthorizer i){
        service=s;
        internal=i;
    }
    @GetMapping("/{id}/publication")
    public PublicationStatus publication(@PathVariable UUID id,@RequestHeader(value="X-Service-Token",required=false) String token){
        internal.require(token);
        return service.publication(id);
    }
    @GetMapping("/{id}/document-scope")
    public CourseDocumentScope documentScope(@PathVariable UUID id,@RequestHeader(value="X-Service-Token",required=false) String token){
        internal.require(token);
        return service.documentScope(id);
    }
    @GetMapping("/{id}/learning-metadata")
    public CourseLearningMetadata metadata(@PathVariable UUID id,@RequestParam(required=false) Integer version,@RequestHeader(value="X-Service-Token",required=false) String token){
        internal.require(token);
        return service.learningMetadata(id,version);
    }

}
