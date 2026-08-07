package com.lmspilot.grading.api;

import com.lmspilot.grading.domain.GradeAppealStatus;

import jakarta.validation.constraints.DecimalMin;
public record ResolveGradeAppealRequest(GradeAppealStatus status,String resolution,@DecimalMin("0.0")Double correctedScore){
}
