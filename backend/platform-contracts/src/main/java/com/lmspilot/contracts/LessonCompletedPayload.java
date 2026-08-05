package com.lmspilot.contracts;

import java.time.Instant;
import java.util.*;

public record LessonCompletedPayload(UUID enrollmentId, UUID courseId, UUID lessonId, UUID userId, int progressPercent) {}
