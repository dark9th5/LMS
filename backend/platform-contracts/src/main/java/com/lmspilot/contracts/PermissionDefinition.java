package com.lmspilot.contracts;

import java.util.Set;

public record PermissionDefinition(
    String code, String group, String label, String description,
    Set<String> allowedScopes, PermissionRisk risk, boolean legacy
) {
    public PermissionDefinition { allowedScopes = Set.copyOf(allowedScopes); }
}
