package com.lmspilot.contracts;

import java.time.Instant;
import java.util.*;

public record UserCreatedPayload(UUID userId, String username, String fullName, UUID organizationUnitId, Set<String> roles) {}
