package com.lmspilot.competency.api; import jakarta.validation.constraints.*; import java.util.UUID; public record CourseMapRequest(UUID courseId,UUID competencyId,@Min(1) @Max(10) int targetLevel){}
