package com.lmspilot.support.security

import com.lmspilot.support.api.ApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

data class ScopeAuthorizationCheckResponse(val allowed: Boolean = false)
data class ScopeIdsResponse(val scopeIds: Set<UUID> = emptySet())

/**
 * Resource services use this client after the coarse JWT authority gate.
 * It prevents a scoped role from becoming a global permission merely because
 * its capability is present in the access token for UI/routing purposes.
 */
@Component
class ScopedAuthorizationClient(
    builder: RestClient.Builder,
    @Value("\${identity-service.url:http://localhost:8081}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = builder.baseUrl(baseUrl).build()

    fun allowed(permission: String, scopeType: String, scopeId: UUID?): Boolean =
        allowedForUser(CurrentUser.id(), permission, scopeType, scopeId)

    fun allowedForUser(userId: UUID, permission: String, scopeType: String, scopeId: UUID?): Boolean {
        if (userId == CurrentUser.id() && CurrentUser.isSystemAdmin()) return true
        return runCatching {
            client.get()
                .uri { uri ->
                    val query = uri.path("/internal/v1/authorization/check")
                        .queryParam("userId", userId)
                        .queryParam("permission", permission)
                        .queryParam("scopeType", scopeType)
                    if (scopeId != null) query.queryParam("scopeId", scopeId)
                    query.build()
                }
                .header("X-Service-Token", serviceToken)
                .retrieve()
                .body(ScopeAuthorizationCheckResponse::class.java)
                ?.allowed == true
        }.getOrElse { cause ->
            log.warn("Scoped authorization check failed user={} permission={} scopeType={} scopeId={}", userId, permission, scopeType, scopeId, cause)
            false
        }
    }

    fun scopeIds(permission: String, scopeType: String): Set<UUID> =
        scopeIdsForUser(CurrentUser.id(), permission, scopeType)

    fun scopeIdsForUser(userId: UUID, permission: String, scopeType: String): Set<UUID> = runCatching {
        client.get()
            .uri { uri -> uri.path("/internal/v1/authorization/scope-ids")
                .queryParam("userId", userId)
                .queryParam("permission", permission)
                .queryParam("scopeType", scopeType)
                .build() }
            .header("X-Service-Token", serviceToken)
            .retrieve()
            .body(ScopeIdsResponse::class.java)
            ?.scopeIds.orEmpty()
    }.getOrElse { cause ->
        log.warn("Scoped authorization id lookup failed user={} permission={} scopeType={}", userId, permission, scopeType, cause)
        emptySet()
    }

    fun require(permission: String, scopeType: String, scopeId: UUID?, message: String = "Tài nguyên ngoài phạm vi được cấp") {
        if (!allowed(permission, scopeType, scopeId)) {
            throw ApiException(HttpStatus.FORBIDDEN, "RESOURCE_OUT_OF_SCOPE", message)
        }
    }
}
