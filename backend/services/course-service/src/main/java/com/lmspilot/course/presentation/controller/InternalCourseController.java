package com.lmspilot.course.presentation.controller;

import com.lmspilot.course.application.dto.response.CourseDocumentScope;
import com.lmspilot.course.application.dto.response.CourseLearningMetadata;
import com.lmspilot.course.application.dto.response.PublicationStatus;
import com.lmspilot.course.application.interfaces.service.ICourseInternalQueryService;
import com.lmspilot.support.security.InternalTokenAuthorizer;

import java.util.*;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/courses")
public class InternalCourseController {
    private final ICourseInternalQueryService service;
    private final InternalTokenAuthorizer internal;
    public InternalCourseController(ICourseInternalQueryService s,InternalTokenAuthorizer i){
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
