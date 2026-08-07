package com.lmspilot.reporting.api;

import java.time.Instant;

import java.util.UUID;
public record DueLearningReminder(UUID enrollmentId,UUID classId,UUID courseId,UUID userId,Instant dueAt,int progressPercent){
}
