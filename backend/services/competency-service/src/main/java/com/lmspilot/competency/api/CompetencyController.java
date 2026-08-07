package com.lmspilot.competency.api;

import com.lmspilot.contracts.Permissions;

import com.lmspilot.support.security.CurrentUser;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("/api/v1/competencies")
public class CompetencyController {
    private final CompetencyService service;
    public CompetencyController(CompetencyService s){
        service=s;
    }
    @GetMapping
    @PreAuthorize("hasAnyAuthority('"+Permissions.COMPETENCIES_READ_SELF+"','"+Permissions.COMPETENCIES_READ_SCOPE+"','"+Permissions.COMPETENCIES_MANAGE+"')")
    public List<CompetencyView> list(@RequestParam(defaultValue="false") boolean includeInactive){
        return service.listCompetencies(includeInactive);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('"+Permissions.COMPETENCIES_MANAGE+"')")
    public CompetencyView create(@Valid
    @RequestBody CompetencyRequest i){
        return service.createCompetency(i);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('"+Permissions.COMPETENCIES_MANAGE+"')")
    public CompetencyView update(@PathVariable UUID id,@Valid
    @RequestBody CompetencyRequest i){
        return service.updateCompetency(id,i);
    }
    @GetMapping("/profiles")
    @PreAuthorize("hasAnyAuthority('"+Permissions.COMPETENCIES_READ_SCOPE+"','"+Permissions.COMPETENCIES_MANAGE+"')")
    public List<ProfileView> profiles(){
        return service.listProfiles();
    }
    @PostMapping("/profiles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('"+Permissions.COMPETENCIES_MANAGE+"')")
    public ProfileView createProfile(@Valid
    @RequestBody ProfileRequest i){
        return service.createProfile(i);
    }
    @PutMapping("/profiles/{id}")
    @PreAuthorize("hasAuthority('"+Permissions.COMPETENCIES_MANAGE+"')")
    public ProfileView updateProfile(@PathVariable UUID id,@Valid
    @RequestBody ProfileRequest i){
        return service.updateProfile(id,i);
    }
    @PostMapping("/profile-assignments")
    @PreAuthorize("hasAuthority('"+Permissions.COMPETENCIES_MANAGE+"')")
    public Map<String,Integer> assign(@Valid
    @RequestBody AssignProfileRequest i){
        return Map.of("created",service.assignProfile(i));
    }
    @DeleteMapping("/profile-assignments")
    @PreAuthorize("hasAuthority('"+Permissions.COMPETENCIES_MANAGE+"')")
    public Map<String,Long> unassign(@RequestParam UUID userId,@RequestParam UUID profileId){
        return Map.of("deleted",service.unassignProfile(userId,profileId));
    }
    @PostMapping("/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('"+Permissions.COMPETENCIES_READ_SELF+"','"+Permissions.COMPETENCIES_ASSESS+"')")
    public AssessmentView assess(@Valid
    @RequestBody AssessmentRequest i){
        return service.assess(i);
    }
    @GetMapping("/me/gaps")
    @PreAuthorize("hasAuthority('"+Permissions.COMPETENCIES_READ_SELF+"')")
    public CompetencyGapResponse myGaps(){
        return service.gap(CurrentUser.id());
    }
    @GetMapping("/users/{userId}/gaps")
    @PreAuthorize("hasAuthority('"+Permissions.COMPETENCIES_READ_SCOPE+"')")
    public CompetencyGapResponse gaps(@PathVariable UUID userId){
        return service.gap(userId);
    }
    @GetMapping("/me/assessments")
    @PreAuthorize("hasAuthority('"+Permissions.COMPETENCIES_READ_SELF+"')")
    public List<AssessmentView> myAssessments(){
        return service.assessments(CurrentUser.id());
    }
    @GetMapping("/users/{userId}/assessments")
    @PreAuthorize("hasAuthority('"+Permissions.COMPETENCIES_READ_SCOPE+"')")
    public List<AssessmentView> assessments(@PathVariable UUID userId){
        return service.assessments(userId);
    }
    @PostMapping("/course-maps")
    @PreAuthorize("hasAuthority('"+Permissions.COMPETENCIES_MANAGE+"')")
    public void mapCourse(@Valid
    @RequestBody CourseMapRequest i){
        service.mapCourse(i);
    }

}
