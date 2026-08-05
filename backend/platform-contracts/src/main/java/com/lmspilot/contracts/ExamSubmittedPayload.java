package com.lmspilot.contracts;

import java.time.Instant;
import java.util.*;

public record ExamSubmittedPayload(UUID sessionId, UUID examId, UUID userId, Instant submittedAt) {}
