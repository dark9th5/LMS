package com.lmspilot.support.security;

import com.lmspilot.support.api.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LicenseGuard {
    private static final Logger log = LoggerFactory.getLogger(LicenseGuard.class);
    private final RestClient client;
    private final String serviceToken;
    private final Duration cacheTtl;
    private final Duration staleTolerance;
    private volatile CacheEntry cached;

    public LicenseGuard(RestClient.Builder builder,
                        @Value("${license-service.url:http://localhost:8090}") String baseUrl,
                        @Value("${lmspilot.internal-token}") String serviceToken,
                        @Value("${lmspilot.license-cache-ttl:PT1M}") Duration cacheTtl,
                        @Value("${lmspilot.license-stale-tolerance:PT15M}") Duration staleTolerance) {
        this.client = builder.baseUrl(baseUrl).build();
        this.serviceToken = serviceToken;
        this.cacheTtl = cacheTtl;
        this.staleTolerance = staleTolerance;
    }

    public LicenseEntitlementsView current() {
        Instant now = Instant.now();
        CacheEntry local = cached;
        if (local != null && Duration.between(local.loadedAt(), now).compareTo(cacheTtl) <= 0) return local.value();
        try {
            LicenseEntitlementsView value = client.get().uri("/internal/v1/license/entitlements")
                .header("X-Service-Token", serviceToken).retrieve().body(LicenseEntitlementsView.class);
            if (value == null) throw new IllegalStateException("License service returned an empty body");
            cached = new CacheEntry(value, now);
            return value;
        } catch (RuntimeException cause) {
            local = cached;
            if (local != null && Duration.between(local.loadedAt(), now).compareTo(staleTolerance) <= 0) {
                log.warn("license-service unavailable; using a recently cached entitlement snapshot", cause);
                return local.value();
            }
            log.error("license-service unavailable and no acceptable cached entitlement exists", cause);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "LICENSE_SERVICE_UNAVAILABLE", "Không thể xác nhận trạng thái license");
        }
    }

    public LicenseEntitlementsView requireWritable() {
        LicenseEntitlementsView value = current();
        if (value.readOnly() || Set.of("EXPIRED", "INVALID").contains(value.status())) {
            throw new ApiException(HttpStatus.LOCKED, "LICENSE_READ_ONLY", "License hiện chỉ cho phép đọc dữ liệu");
        }
        return value;
    }

    public LicenseEntitlementsView requireFeature(String feature) { return requireFeature(feature, true); }
    public LicenseEntitlementsView requireFeature(String feature, boolean write) {
        LicenseEntitlementsView value = write ? requireWritable() : current();
        String normalized = feature.trim().toUpperCase(Locale.ROOT);
        Set<String> licensed = value.features().stream().map(v -> v.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        if (!licensed.contains(normalized)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FEATURE_NOT_LICENSED", "Tính năng " + normalized + " không có trong license hiện tại");
        }
        return value;
    }

    public void validateEnabledFeatures(Set<String> enabledFeatureKeys) {
        LicenseEntitlementsView value = requireWritable();
        Set<String> licensed = value.features().stream().map(v -> v.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        Set<String> unsupported = enabledFeatureKeys.stream().map(v -> v.toUpperCase(Locale.ROOT))
            .filter(v -> !licensed.contains(v)).collect(Collectors.toCollection(TreeSet::new));
        if (!unsupported.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "FEATURE_FLAG_EXCEEDS_LICENSE",
                "Không thể bật tính năng chưa có trong license: " + String.join(", ", unsupported));
        }
    }

    private record CacheEntry(LicenseEntitlementsView value, Instant loadedAt) {}
}
