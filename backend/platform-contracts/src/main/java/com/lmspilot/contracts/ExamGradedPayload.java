package com.lmspilot.contracts;

import java.time.Instant;
import java.util.*;

public record ExamGradedPayload(UUID sessionId, UUID examId, UUID userId, double score, double maxScore, boolean passed, String status, UUID enrollmentId, UUID courseId, UUID lessonId, Boolean effectivePassed, Double effectivePercentage, String scoreStrategy) {
    public ExamGradedPayload { if (scoreStrategy == null || scoreStrategy.isBlank()) scoreStrategy = "LATEST"; }
}
