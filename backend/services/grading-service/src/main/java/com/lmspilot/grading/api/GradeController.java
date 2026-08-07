package com.lmspilot.grading.api;

import com.lmspilot.contracts.Permissions;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("/api/v1/grades")
public class GradeController{
    private final GradingService s;
    public GradeController(GradingService s){
        this.s=s;
    }
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('"+Permissions.GRADES_READ_SELF+"')")
    public List<GradeResponse>mine(){
        return s.myGrades();
    }
    @GetMapping("/queue")
    @PreAuthorize("hasAuthority('"+Permissions.GRADING_MANAGE+"')")
    public List<GradeResponse>queue(){
        return s.queue();
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('"+Permissions.GRADING_MANAGE+"')")
    public GradeResponse manual(@PathVariable UUID id,@Valid
    @RequestBody ManualGradeRequest i){
        return s.completeManual(id,i);
    }
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyAuthority('"+Permissions.GRADES_READ_SELF+"','"+Permissions.GRADING_MANAGE+"')")
    public List<GradeRevisionResponse>history(@PathVariable UUID id){
        return s.history(id);
    }
    @PostMapping("/{id}/appeals")
    @PreAuthorize("hasAuthority('"+Permissions.GRADE_APPEALS_CREATE+"')")
    public GradeAppealResponse appeal(@PathVariable UUID id,@Valid
    @RequestBody CreateGradeAppealRequest i){
        return s.createAppeal(id,i);
    }
    @GetMapping("/appeals/me")
    @PreAuthorize("hasAuthority('"+Permissions.GRADE_APPEALS_CREATE+"')")
    public List<GradeAppealResponse>myAppeals(){
        return s.myAppeals();
    }
    @GetMapping("/appeals")
    @PreAuthorize("hasAuthority('"+Permissions.GRADE_APPEALS_MANAGE+"')")
    public List<GradeAppealResponse>appealQueue(){
        return s.appealQueue();
    }
    @PutMapping("/appeals/{id}")
    @PreAuthorize("hasAuthority('"+Permissions.GRADE_APPEALS_MANAGE+"')")
    public GradeAppealResponse resolve(@PathVariable UUID id,@Valid
    @RequestBody ResolveGradeAppealRequest i){
        return s.resolveAppeal(id,i);
    }

}
