package com.lmspilot.contracts;

import java.util.Set;

public record AccessProfileDefinition(
    String code, String name, String description, Set<String> permissions,
    Set<String> recommendedScopes, PermissionRisk risk
) {
    public AccessProfileDefinition {
        permissions = Set.copyOf(permissions);
        recommendedScopes = Set.copyOf(recommendedScopes);
    }
}
