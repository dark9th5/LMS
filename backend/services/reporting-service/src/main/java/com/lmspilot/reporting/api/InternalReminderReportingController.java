package com.lmspilot.reporting.api;

import com.lmspilot.support.security.InternalTokenAuthorizer;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import java.util.*;
@RestController
@RequestMapping("/internal/v1/reports/reminders")
public class InternalReminderReportingController{
    private final ReminderReportingService s;
    private final InternalTokenAuthorizer i;
    public InternalReminderReportingController(ReminderReportingService s,InternalTokenAuthorizer i){
        this.s=s;
        this.i=i;
    }
    @GetMapping("/due")
    public List<DueLearningReminder>due(@RequestParam
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant from,@RequestParam
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant to,@RequestHeader(value="X-Service-Token",required=false)String token){
        i.require(token);
        return s.dueBetween(from,to);
    }

}
