package com.lmspilot.reporting.api;

import com.lmspilot.contracts.Permissions;

import com.lmspilot.reporting.domain.ReportScope;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("/api/v1/reports/kpis")
public class KpiReportingController{
    private final KpiReportingService s;
    public KpiReportingController(KpiReportingService s){
        this.s=s;
    }
    @GetMapping
    @PreAuthorize("hasAnyAuthority('"+Permissions.REPORTS_KPI_READ+"','"+Permissions.REPORTS_READ_SELF+"','"+Permissions.REPORTS_READ_SCOPE+"','"+Permissions.REPORTS_READ+"')")
    public LearningKpiResponse summary(@RequestParam(defaultValue="SELF")ReportScope scope,@RequestParam(required=false)UUID courseId){
        return s.summary(scope,courseId);
    }
    @GetMapping("/courses")
    @PreAuthorize("hasAnyAuthority('"+Permissions.REPORTS_KPI_READ+"','"+Permissions.REPORTS_READ_SCOPE+"','"+Permissions.REPORTS_READ+"')")
    public List<CourseKpiRow>courses(@RequestParam(defaultValue="ASSIGNED")ReportScope scope){
        return s.byCourse(scope);
    }

}
