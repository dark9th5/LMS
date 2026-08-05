package com.lmspilot.contracts;

import java.time.Instant;
import java.util.*;

public record CourseCompletedPayload(UUID enrollmentId, UUID courseId, UUID userId, Instant completedAt) {}
