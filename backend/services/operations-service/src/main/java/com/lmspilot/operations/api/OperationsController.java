package com.lmspilot.operations.api;

import com.lmspilot.contracts.Permissions;

import com.lmspilot.operations.domain.OperationType;

import jakarta.validation.Valid;

import org.springframework.http.*;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("/api/v1/operations")
@PreAuthorize("hasAuthority('"+Permissions.OPERATIONS_MANAGE+"')")
public class OperationsController{
    private final OperationsService s;
    public OperationsController(OperationsService s){
        this.s=s;
    }
    @GetMapping("/health")
    public List<ServiceHealth>health(){
        return s.health();
    }
    @GetMapping("/jobs")
    public List<OperationJobResponse>jobs(){
        return s.jobs();
    }
    @PostMapping("/jobs/{type}")
    public OperationJobResponse request(@PathVariable OperationType type,@Valid
    @RequestBody OperationRequest i){
        return s.request(type,i);
    }
    @GetMapping("/schedules")
    public List<OperationScheduleResponse>schedules(){
        return s.schedules();
    }
    @PostMapping("/schedules")
    public OperationScheduleResponse create(@Valid
    @RequestBody OperationScheduleRequest i){
        return s.createSchedule(i);
    }
    @PutMapping("/schedules/{id}")
    public OperationScheduleResponse update(@PathVariable UUID id,@Valid
    @RequestBody OperationScheduleRequest i){
        return s.updateSchedule(id,i);
    }
    @DeleteMapping("/schedules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id){
        s.deleteSchedule(id);
    }

}
