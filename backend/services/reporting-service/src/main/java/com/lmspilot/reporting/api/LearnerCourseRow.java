package com.lmspilot.reporting.api;

import java.time.Instant;

import java.util.UUID;
public record LearnerCourseRow(UUID enrollmentId,UUID classId,UUID courseId,UUID userId,int progressPercent,boolean completed,Instant dueAt,Double lastScore,Boolean passed,Instant updatedAt){
}
