package com.lmspilot.identity.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.Permissions
import com.lmspilot.identity.api.*
import com.lmspilot.identity.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AuthorizationService(
    private val users: UserAccountRepository,
    private val roles: RoleRepository,
    private val grants: AuthorizationGrantRepository,
    private val roleAssignments: ScopedRoleAssignmentRepository,
    private val bulkOperations: BulkOperationRepository,
    private val bulkGuard: BulkOperationGuard,
    private val objectMapper: ObjectMapper,
    private val organizationScopes: OrganizationScopeClient,
) {
    @Transactional
    fun grantBulk(input: BulkGrantRequest): BulkGrantResponse {
        val requestedBy = CurrentUser.id()
        bulkGuard.replay(input.operationId, "BULK_GRANT", requestedBy)?.let {
            return objectMapper.readValue(it.resultJson, BulkGrantResponse::class.java)
        }
        val foundUsers = users.findAllById(input.userIds)
        if (foundUsers.size != input.userIds.size) {
            throw ApiException(HttpStatus.BAD_REQUEST, "USER_NOT_FOUND", "Một hoặc nhiều người dùng không tồn tại")
        }

        val knownPermissions = permissionCatalog()
        val createdGrants = mutableListOf<AuthorizationGrantEntity>()
        val createdRoles = mutableListOf<ScopedRoleAssignmentEntity>()
        for (user in foundUsers) {
            for (grant in input.grants) {
                if (grant.roleCode != null) {
                    val role = roles.findByCodeIgnoreCase(grant.roleCode)
                        ?: throw ApiException(HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND", "Vai trò ${grant.roleCode} không tồn tại")
                    createdRoles += roleAssignments.save(
                        ScopedRoleAssignmentEntity(
                            userId = user.id,
                            role = role,
                            scopeType = grant.scopeType,
                            scopeId = grant.scopeId,
                            effect = grant.effect,
                            validFrom = grant.validFrom,
                            validUntil = grant.validUntil,
                            createdBy = requestedBy,
                        )
                    )
                } else {
                    val permission = grant.permissionCode!!
                    if (permission !in knownPermissions) {
                        throw ApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_PERMISSION", "Quyền không hợp lệ: $permission")
                    }
                    createdGrants += grants.save(
                        AuthorizationGrantEntity(
                            principalType = PrincipalType.USER,
                            principalId = user.id,
                            permissionCode = permission,
                            scopeType = grant.scopeType,
                            scopeId = grant.scopeId,
                            effect = grant.effect,
                            validFrom = grant.validFrom,
                            validUntil = grant.validUntil,
                            createdBy = requestedBy,
                        )
                    )
                }
            }
        }
        val response = BulkGrantResponse(
            operationId = input.operationId,
            permissionGrants = createdGrants.map { it.response() },
            roleAssignments = createdRoles.map { it.response() },
        )
        bulkOperations.save(
            BulkOperationEntity(
                operationId = input.operationId,
                operationType = "BULK_GRANT",
                requestedBy = requestedBy,
                resultJson = objectMapper.writeValueAsString(response),
            )
        )
        return response
    }

    @Transactional
    fun revokeBulk(input: RevokeGrantsRequest): RevokeGrantsResponse {
        val requestedBy = CurrentUser.id()
        bulkGuard.replay(input.operationId, "BULK_REVOKE", requestedBy)?.let {
            return objectMapper.readValue(it.resultJson, RevokeGrantsResponse::class.java)
        }
        val response = RevokeGrantsResponse(
            permissionGrantsDeleted = if (input.grantIds.isEmpty()) 0 else grants.deleteAllByIdInAndPrincipalType(input.grantIds, PrincipalType.USER),
            roleAssignmentsDeleted = if (input.roleAssignmentIds.isEmpty()) 0 else roleAssignments.deleteAllByIdIn(input.roleAssignmentIds),
        )
        bulkOperations.save(
            BulkOperationEntity(
                operationId = input.operationId,
                operationType = "BULK_REVOKE",
                requestedBy = requestedBy,
                resultJson = objectMapper.writeValueAsString(response),
            )
        )
        return response
    }


    @Transactional(readOnly = true)
    fun permissionsForToken(user: UserAccountEntity): Set<String> {
        if (user.accountType == AccountType.SYSTEM_ADMIN) return permissionCatalog()
        val now = Instant.now()
        val active: (Instant?, Instant?) -> Boolean = { from, until ->
            (from == null || !from.isAfter(now)) && (until == null || until.isAfter(now))
        }
        val direct = grants.findAllByPrincipalTypeAndPrincipalId(PrincipalType.USER, user.id)
            .filter { active(it.validFrom, it.validUntil) }
        val scopedRoles = roleAssignments.findAllByUserId(user.id)
            .filter { active(it.validFrom, it.validUntil) }
        val allowed = user.roles.flatMap { it.permissions }.toSet() +
            direct.filter { it.effect == GrantEffect.ALLOW }.map { it.permissionCode } +
            scopedRoles.filter { it.effect == GrantEffect.ALLOW }.flatMap { it.role.permissions }
        val systemDenied = direct.filter { it.scopeType == ScopeType.SYSTEM && it.effect == GrantEffect.DENY }.map { it.permissionCode }.toSet() +
            scopedRoles.filter { it.scopeType == ScopeType.SYSTEM && it.effect == GrantEffect.DENY }.flatMap { it.role.permissions }.toSet()
        return allowed - systemDenied
    }

    @Transactional(readOnly = true)
    fun effective(userId: UUID, scopeType: ScopeType, scopeId: UUID?): EffectivePermissionResponse {
        validateScope(scopeType, scopeId)
        val user = users.findById(userId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng")
        }
        if (user.accountType == AccountType.SYSTEM_ADMIN) {
            return EffectivePermissionResponse(user.id, scopeType, scopeId, permissionCatalog(), emptySet())
        }

        val now = Instant.now()
        val applicableUnitIds = organizationScopes.applicableUnitIds(scopeType, scopeId)
        val inScope: (ScopeType, UUID?) -> Boolean = { type, id ->
            when {
                type == ScopeType.SYSTEM -> true
                scopeType in OrganizationScopeClient.ORGANIZATION_SCOPES && type in OrganizationScopeClient.ORGANIZATION_SCOPES -> id != null && id in applicableUnitIds
                else -> type == scopeType && id == scopeId
            }
        }
        val active: (Instant?, Instant?) -> Boolean = { from, until ->
            (from == null || !from.isAfter(now)) && (until == null || until.isAfter(now))
        }

        val direct = grants.findAllByPrincipalTypeAndPrincipalIdIn(PrincipalType.USER, listOf(user.id))
            .filter { inScope(it.scopeType, it.scopeId) && active(it.validFrom, it.validUntil) }
        val scopedRoles = roleAssignments.findAllByUserId(user.id)
            .filter { inScope(it.scopeType, it.scopeId) && active(it.validFrom, it.validUntil) }

        val base = user.roles.flatMap { it.permissions }.toSet()
        val roleAllowed = scopedRoles.filter { it.effect == GrantEffect.ALLOW }.flatMap { it.role.permissions }.toSet()
        val roleDenied = scopedRoles.filter { it.effect == GrantEffect.DENY }.flatMap { it.role.permissions }.toSet()
        val permissionAllowed = direct.filter { it.effect == GrantEffect.ALLOW }.map { it.permissionCode }.toSet()
        val permissionDenied = direct.filter { it.effect == GrantEffect.DENY }.map { it.permissionCode }.toSet()
        val denied = roleDenied + permissionDenied
        val allowed = (base + roleAllowed + permissionAllowed) - denied
        return EffectivePermissionResponse(user.id, scopeType, scopeId, allowed, denied)
    }


    @Transactional(readOnly = true)
    fun capabilities(userId: UUID): Set<String> {
        val user = users.findById(userId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng")
        }
        return permissionsForToken(user)
    }

    /**
     * Exact resource authorization used by other services after the coarse JWT gate.
     * Base account roles are intentionally excluded: an INSTRUCTOR may own or be
     * assigned to resources, but the role alone must not unlock every course/exam.
     * Explicit SYSTEM and matching scoped grants/roles are evaluated, with DENY
     * taking precedence.
     */
    @Transactional(readOnly = true)
    fun check(userId: UUID, permission: String, scopeType: ScopeType, scopeId: UUID?): Boolean {
        validateScope(scopeType, scopeId)
        val user = users.findById(userId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng")
        }
        if (user.accountType == AccountType.SYSTEM_ADMIN) return true
        val now = Instant.now()
        fun active(from: Instant?, until: Instant?) =
            (from == null || !from.isAfter(now)) && (until == null || until.isAfter(now))
        val applicableUnitIds = organizationScopes.applicableUnitIds(scopeType, scopeId)
        fun inScope(type: ScopeType, id: UUID?): Boolean = when {
            type == ScopeType.SYSTEM -> true
            scopeType in OrganizationScopeClient.ORGANIZATION_SCOPES && type in OrganizationScopeClient.ORGANIZATION_SCOPES ->
                id != null && id in applicableUnitIds
            else -> type == scopeType && id == scopeId
        }
        val direct = grants.findAllByPrincipalTypeAndPrincipalId(PrincipalType.USER, userId)
            .filter { it.permissionCode == permission && inScope(it.scopeType, it.scopeId) && active(it.validFrom, it.validUntil) }
        val assignedRoles = roleAssignments.findAllByUserId(userId)
            .filter { permission in it.role.permissions && inScope(it.scopeType, it.scopeId) && active(it.validFrom, it.validUntil) }
        val denied = direct.any { it.effect == GrantEffect.DENY } || assignedRoles.any { it.effect == GrantEffect.DENY }
        val allowed = direct.any { it.effect == GrantEffect.ALLOW } || assignedRoles.any { it.effect == GrantEffect.ALLOW }
        return allowed && !denied
    }

    /**
     * Returns explicit resource ids for which a permission is active. Global
     * SYSTEM capabilities are intentionally not expanded here; callers check
     * them through [check] first, then use this set for scoped list queries.
     */
    @Transactional(readOnly = true)
    fun scopeIds(userId: UUID, permission: String, scopeType: ScopeType): Set<UUID> {
        require(scopeType != ScopeType.SYSTEM) { "scopeType must identify a resource" }
        val user = users.findById(userId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng")
        }
        if (user.accountType == AccountType.SYSTEM_ADMIN) return emptySet()

        val now = Instant.now()
        fun active(from: Instant?, until: Instant?) =
            (from == null || !from.isAfter(now)) && (until == null || until.isAfter(now))

        val direct = grants.findAllByPrincipalTypeAndPrincipalId(PrincipalType.USER, userId)
            .filter { it.scopeType == scopeType && it.scopeId != null && it.permissionCode == permission && active(it.validFrom, it.validUntil) }
        val assignedRoles = roleAssignments.findAllByUserId(userId)
            .filter { it.scopeType == scopeType && it.scopeId != null && permission in it.role.permissions && active(it.validFrom, it.validUntil) }
        val allowed = direct.filter { it.effect == GrantEffect.ALLOW }.mapNotNull { it.scopeId }.toSet() +
            assignedRoles.filter { it.effect == GrantEffect.ALLOW }.mapNotNull { it.scopeId }
        val denied = direct.filter { it.effect == GrantEffect.DENY }.mapNotNull { it.scopeId }.toSet() +
            assignedRoles.filter { it.effect == GrantEffect.DENY }.mapNotNull { it.scopeId }
        return allowed - denied
    }

    @Transactional(readOnly = true)
    fun assignments(userId: UUID): Map<String, Any> {
        users.findById(userId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng")
        }
        return mapOf(
            "permissionGrants" to grants.findAllByPrincipalTypeAndPrincipalId(PrincipalType.USER, userId).map { it.response() },
            "roleAssignments" to roleAssignments.findAllByUserIdOrderByCreatedAtDesc(userId).map { it.response() },
        )
    }

    private fun validateScope(scopeType: ScopeType, scopeId: UUID?) {
        if ((scopeType == ScopeType.SYSTEM) != (scopeId == null)) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_SCOPE", "scopeId không hợp lệ với scopeType")
        }
    }

    private fun permissionCatalog(): Set<String> = Permissions::class.java.declaredFields
        .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) }
        .mapNotNull { runCatching { it.get(null) as? String }.getOrNull() }
        .toSet()
}

private fun AuthorizationGrantEntity.response() = GrantResponse(
    id, principalType, principalId, permissionCode, scopeType, scopeId, effect, validFrom, validUntil,
)

private fun ScopedRoleAssignmentEntity.response() = RoleAssignmentResponse(
    id, userId, role.code, scopeType, scopeId, effect, validFrom, validUntil,
)
