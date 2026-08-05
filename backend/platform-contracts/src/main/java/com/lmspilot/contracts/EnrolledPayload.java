package com.lmspilot.contracts;

import java.time.Instant;
import java.util.*;

public record EnrolledPayload(UUID enrollmentId, UUID classId, UUID courseId, UUID userId, Instant dueAt) {}
