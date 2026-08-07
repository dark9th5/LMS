package com.lmspilot.reporting.api;

import java.time.Instant;

import java.util.UUID;
public record CourseKpiRow(UUID courseId,int totalEnrollments,int completed,int overdue,double completionRate,double passRate,double averageProgress,Double averageScore,Instant lastActivityAt){
}
