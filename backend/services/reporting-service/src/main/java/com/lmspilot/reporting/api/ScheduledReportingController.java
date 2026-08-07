package com.lmspilot.reporting.api;

import com.lmspilot.contracts.Permissions;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("/api/v1/reports")
public class ScheduledReportingController{
    private final ScheduledReportingService s;
    public ScheduledReportingController(ScheduledReportingService s){
        this.s=s;
    }
    @PostMapping("/exports")
    @PreAuthorize("hasAuthority('"+Permissions.REPORTS_EXPORT+"')")
    public ReportExportJobResponse create(@RequestBody CreateReportExportRequest i){
        return s.createExport(i);
    }
    @GetMapping("/exports")
    @PreAuthorize("hasAuthority('"+Permissions.REPORTS_EXPORT+"')")
    public List<ReportExportJobResponse>exports(){
        return s.myExports();
    }
    @GetMapping("/exports/{id}/download")
    @PreAuthorize("hasAuthority('"+Permissions.REPORTS_EXPORT+"')")
    public ResponseEntity<byte[]>download(@PathVariable UUID id){
        return s.download(id);
    }
    @PostMapping("/schedules")
    @PreAuthorize("hasAuthority('"+Permissions.REPORTS_SCHEDULE+"')")
    public ReportScheduleResponse createSchedule(@Valid
    @RequestBody ReportScheduleRequest i){
        return s.createSchedule(i);
    }
    @GetMapping("/schedules")
    @PreAuthorize("hasAuthority('"+Permissions.REPORTS_SCHEDULE+"')")
    public List<ReportScheduleResponse>schedules(){
        return s.mySchedules();
    }
    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('"+Permissions.REPORTS_SCHEDULE+"')")
    public ReportScheduleResponse update(@PathVariable UUID id,@Valid
    @RequestBody ReportScheduleRequest i){
        return s.updateSchedule(id,i);
    }
    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('"+Permissions.REPORTS_SCHEDULE+"')")
    public void delete(@PathVariable UUID id){
        s.deleteSchedule(id);
    }

}
