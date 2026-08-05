package com.lmspilot.support.security;

import java.util.Set;
import java.util.UUID;
public record ScopeIdsResponse(Set<UUID> scopeIds) { public ScopeIdsResponse { scopeIds = scopeIds == null ? Set.of() : Set.copyOf(scopeIds); } }
