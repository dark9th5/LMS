package com.lmspilot.support.security;

import com.lmspilot.support.api.ApiException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ScopedAuthorizationClient {
    private static final Logger log = LoggerFactory.getLogger(ScopedAuthorizationClient.class);
    private final RestClient client;
    private final String serviceToken;
    private final long ttlNanos;
    private final int maxEntries;
    private final ConcurrentHashMap<String, CacheValue<Boolean>> allowedCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheValue<Set<UUID>>> scopeCache = new ConcurrentHashMap<>();
    private final AtomicInteger writes = new AtomicInteger();

    public ScopedAuthorizationClient(RestClient.Builder builder,
        @Value("${identity-service.url:http://localhost:8081}") String baseUrl,
        @Value("${lmspilot.internal-token}") String serviceToken,
        @Value("${authorization-client.connect-timeout-ms:1200}") int connectTimeoutMs,
        @Value("${authorization-client.read-timeout-ms:2500}") int readTimeoutMs,
        @Value("${authorization-client.cache-ttl:PT4S}") Duration cacheTtl,
        @Value("${authorization-client.cache-max-entries:5000}") int maxEntries) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(Math.max(250, connectTimeoutMs)));
        requestFactory.setReadTimeout(Duration.ofMillis(Math.max(500, readTimeoutMs)));
        this.client = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.serviceToken = serviceToken;
        this.ttlNanos = Math.max(0, cacheTtl.toNanos());
        this.maxEntries = Math.max(256, maxEntries);
    }

    public boolean allowed(String permission, String scopeType, UUID scopeId) {
        return allowedForUser(CurrentUser.id(), permission, scopeType, scopeId);
    }

    public boolean allowedForUser(UUID userId, String permission, String scopeType, UUID scopeId) {
        String key = userId + "|" + permission + "|" + scopeType + "|" + scopeId;
        Boolean cached = get(allowedCache, key);
        if (cached != null) return cached;
        try {
            ScopeAuthorizationCheckResponse response = client.get().uri(uri -> {
                var query = uri.path("/internal/v1/authorization/check").queryParam("userId", userId)
                    .queryParam("permission", permission).queryParam("scopeType", scopeType);
                if (scopeId != null) query.queryParam("scopeId", scopeId);
                return query.build();
            }).header("X-Service-Token", serviceToken).retrieve().body(ScopeAuthorizationCheckResponse.class);
            boolean result = response != null && response.allowed();
            put(allowedCache, key, result);
            return result;
        } catch (RuntimeException cause) {
            log.warn("Scoped authorization check failed user={} permission={} scopeType={} scopeId={}", userId, permission, scopeType, scopeId, cause);
            return false;
        }
    }

    public Set<UUID> scopeIds(String permission, String scopeType) {
        return scopeIdsForUser(CurrentUser.id(), permission, scopeType);
    }

    public Set<UUID> scopeIdsForUser(UUID userId, String permission, String scopeType) {
        String key = userId + "|" + permission + "|" + scopeType;
        Set<UUID> cached = get(scopeCache, key);
        if (cached != null) return cached;
        try {
            ScopeIdsResponse response = client.get().uri(uri -> uri.path("/internal/v1/authorization/scope-ids")
                .queryParam("userId", userId).queryParam("permission", permission).queryParam("scopeType", scopeType).build())
                .header("X-Service-Token", serviceToken).retrieve().body(ScopeIdsResponse.class);
            Set<UUID> result = response == null || response.scopeIds() == null ? Set.of() : Set.copyOf(response.scopeIds());
            put(scopeCache, key, result);
            return result;
        } catch (RuntimeException cause) {
            log.warn("Scoped authorization id lookup failed user={} permission={} scopeType={}", userId, permission, scopeType, cause);
            return Set.of();
        }
    }

    public void require(String permission, String scopeType, UUID scopeId) {
        require(permission, scopeType, scopeId, "Tài nguyên ngoài phạm vi được cấp");
    }

    public void require(String permission, String scopeType, UUID scopeId, String message) {
        if (!allowed(permission, scopeType, scopeId)) throw new ApiException(HttpStatus.FORBIDDEN, "RESOURCE_OUT_OF_SCOPE", message);
    }

    private <T> T get(ConcurrentHashMap<String, CacheValue<T>> cache, String key) {
        CacheValue<T> value = cache.get(key);
        if (value == null) return null;
        if (value.expiresAtNanos() <= System.nanoTime()) {
            cache.remove(key, value);
            return null;
        }
        return value.value();
    }

    private <T> void put(ConcurrentHashMap<String, CacheValue<T>> cache, String key, T value) {
        if (ttlNanos <= 0) return;
        cache.put(key, new CacheValue<>(value, System.nanoTime() + ttlNanos));
        if ((writes.incrementAndGet() & 255) == 0) prune(cache);
    }

    private <T> void prune(ConcurrentHashMap<String, CacheValue<T>> cache) {
        long now = System.nanoTime();
        cache.entrySet().removeIf(entry -> entry.getValue().expiresAtNanos() <= now);
        if (cache.size() > maxEntries) cache.clear();
    }

    private record CacheValue<T>(T value, long expiresAtNanos) {}
}
