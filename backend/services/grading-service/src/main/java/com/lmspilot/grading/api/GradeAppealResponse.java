package com.lmspilot.grading.api;

import com.lmspilot.grading.domain.GradeAppealStatus;

import java.time.Instant;

import java.util.UUID;
public record GradeAppealResponse(UUID id,UUID gradeId,UUID userId,String reason,GradeAppealStatus status,String resolution,UUID resolvedBy,Instant createdAt,Instant updatedAt,Instant resolvedAt){
}
