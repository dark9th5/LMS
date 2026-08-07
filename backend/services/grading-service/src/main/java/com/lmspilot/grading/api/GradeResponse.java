package com.lmspilot.grading.api;

import com.lmspilot.grading.domain.GradeStatus;

import java.time.Instant;

import java.util.*;
public record GradeResponse(UUID id,UUID sessionId,UUID examId,UUID enrollmentId,UUID courseId,UUID lessonId,UUID userId,double score,double maxScore,double percentage,boolean passed,GradeStatus status,List<GradeDetail>details,String feedback,Instant updatedAt){
}
