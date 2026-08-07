package com.lmspilot.reporting.api;

import com.lmspilot.reporting.domain.ReportScope;

import java.time.Instant;

import java.util.UUID;
public record LearningKpiResponse(ReportScope scope,UUID courseId,int totalEnrollments,int notStarted,int inProgress,int completed,int overdue,int dueSoon,int passed,int failed,int activeLast30Days,double completionRate,double passRate,double averageProgress,Double averageScore,Instant generatedAt){
}
