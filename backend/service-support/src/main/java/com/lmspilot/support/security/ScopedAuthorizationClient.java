package com.lmspilot.support.security;

import com.lmspilot.support.api.ApiException;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ScopedAuthorizationClient {
    private static final Logger log = LoggerFactory.getLogger(ScopedAuthorizationClient.class);
    private final RestClient client;
    private final String serviceToken;

    public ScopedAuthorizationClient(RestClient.Builder builder,
        @Value("${identity-service.url:http://localhost:8081}") String baseUrl,
        @Value("${lmspilot.internal-token}") String serviceToken) {
        this.client = builder.baseUrl(baseUrl).build();
        this.serviceToken = serviceToken;
    }
    public boolean allowed(String permission, String scopeType, UUID scopeId) {
        return allowedForUser(CurrentUser.id(), permission, scopeType, scopeId);
    }
    public boolean allowedForUser(UUID userId, String permission, String scopeType, UUID scopeId) {
        try {
            ScopeAuthorizationCheckResponse response = client.get().uri(uri -> {
                var query = uri.path("/internal/v1/authorization/check").queryParam("userId", userId)
                    .queryParam("permission", permission).queryParam("scopeType", scopeType);
                if (scopeId != null) query.queryParam("scopeId", scopeId);
                return query.build();
            }).header("X-Service-Token", serviceToken).retrieve().body(ScopeAuthorizationCheckResponse.class);
            return response != null && response.allowed();
        } catch (RuntimeException cause) {
            log.warn("Scoped authorization check failed user={} permission={} scopeType={} scopeId={}", userId, permission, scopeType, scopeId, cause);
            return false;
        }
    }
    public Set<UUID> scopeIds(String permission, String scopeType) { return scopeIdsForUser(CurrentUser.id(), permission, scopeType); }
    public Set<UUID> scopeIdsForUser(UUID userId, String permission, String scopeType) {
        try {
            ScopeIdsResponse response = client.get().uri(uri -> uri.path("/internal/v1/authorization/scope-ids")
                .queryParam("userId", userId).queryParam("permission", permission).queryParam("scopeType", scopeType).build())
                .header("X-Service-Token", serviceToken).retrieve().body(ScopeIdsResponse.class);
            return response == null ? Set.of() : response.scopeIds();
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
}
