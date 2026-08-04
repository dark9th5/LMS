package com.lmspilot.identity.api

import com.lmspilot.contracts.AccessProfileDefinition
import com.lmspilot.contracts.DefaultAccessProfiles
import com.lmspilot.contracts.PermissionCatalog
import com.lmspilot.contracts.PermissionDefinition
import com.lmspilot.contracts.Permissions
import com.lmspilot.identity.service.AuthorizationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class PermissionCatalogResponse(
    val permissions: List<String>,
    val groups: Map<String, List<String>>,
    val definitions: List<PermissionDefinition>,
    val profiles: List<AccessProfileDefinition>,
    /** Compatibility field for older clients. New UI uses profiles. */
    val defaultRoles: Map<String, Set<String>>,
)

@RestController
@RequestMapping("/api/v1/authorization")
class AuthorizationCatalogController(private val authorization: AuthorizationService) {
    @GetMapping("/catalog")
    @PreAuthorize("hasAnyAuthority('${Permissions.ROLES_READ}','${Permissions.AUTHORIZATION_GRANT}','${Permissions.USERS_READ}')")
    fun catalog(): PermissionCatalogResponse {
        val definitions = PermissionCatalog.all()
        val permissions = definitions.map { it.code }
        return PermissionCatalogResponse(
            permissions = permissions,
            groups = definitions.groupBy { it.group }
                .mapValues { (_, items) -> items.map { it.code } }
                .toSortedMap(),
            definitions = definitions,
            profiles = DefaultAccessProfiles.profiles,
            defaultRoles = DefaultAccessProfiles.profiles.associate { it.code to it.permissions },
        )
    }

    @GetMapping("/users/{userId}/assignments")
    @PreAuthorize("hasAnyAuthority('${Permissions.USERS_READ}','${Permissions.AUTHORIZATION_GRANT}')")
    fun assignments(@PathVariable userId: UUID) = authorization.assignments(userId)
}

data class InternalAuthorizationCheckResponse(val allowed: Boolean)
data class InternalScopeIdsResponse(val scopeIds: Set<UUID>)

@RestController
@RequestMapping("/internal/v1/authorization")
class InternalAuthorizationController(
    private val authorization: AuthorizationService,
    private val internal: com.lmspilot.support.security.InternalTokenAuthorizer,
) {
    @GetMapping("/check")
    fun check(
        @RequestParam userId: UUID,
        @RequestParam permission: String,
        @RequestParam scopeType: com.lmspilot.identity.domain.ScopeType,
        @RequestParam(required = false) scopeId: UUID?,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ): InternalAuthorizationCheckResponse {
        internal.require(token)
        return InternalAuthorizationCheckResponse(authorization.check(userId, permission, scopeType, scopeId))
    }

    @GetMapping("/scope-ids")
    fun scopeIds(
        @RequestParam userId: UUID,
        @RequestParam permission: String,
        @RequestParam scopeType: com.lmspilot.identity.domain.ScopeType,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ): InternalScopeIdsResponse {
        internal.require(token)
        return InternalScopeIdsResponse(authorization.scopeIds(userId, permission, scopeType))
    }
}
