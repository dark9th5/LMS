package com.lmspilot.support.security;

import java.time.Instant;
import java.util.Set;

public record LicenseEntitlementsView(
    String licenseId, String edition, int maxUsers, Set<String> features,
    String status, boolean readOnly, Instant expiresAt, Instant graceEndsAt
) {
    public LicenseEntitlementsView {
        licenseId = licenseId == null ? "unknown" : licenseId;
        edition = edition == null ? "UNKNOWN" : edition;
        features = features == null ? Set.of() : Set.copyOf(features);
        status = status == null ? "INVALID" : status;
    }
}
