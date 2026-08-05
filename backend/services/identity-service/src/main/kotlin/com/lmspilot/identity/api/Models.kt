package com.lmspilot.identity.api

import com.lmspilot.identity.domain.*
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.time.Instant
import java.util.UUID

data class LoginRequest(@field:NotBlank val username: String, @field:NotBlank val password: String)
data class RefreshRequest(@field:NotBlank val refreshToken: String)
data class LogoutRequest(@field:NotBlank val refreshToken: String)
data class ChangePasswordRequest(@field:NotBlank val currentPassword: String, @field:NotBlank @field:Size(min = 12, max = 128) val newPassword: String)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: UserSummary,
)

data class StudentDirectoryItem(
    val id: UUID,
    val code: String,
    val fullName: String,
    val email: String?,
)

data class UserSummary(
    val id: UUID,
    val code: String,
    val username: String,
    val fullName: String,
    val email: String?,
    val organizationUnitId: UUID?,
    val status: AccountStatus,
    val accountType: AccountType,
    val protectedAccount: Boolean,
    val roles: Set<String>,
    val primaryRole: String,
    val permissions: Set<String>,
    val lastLoginAt: Instant?,
    val mustChangePassword: Boolean,
)

data class CreateUserRequest(
    @field:NotBlank @field:Size(max = 80) val code: String,
    @field:NotBlank @field:Size(min = 3, max = 120) val username: String,
    @field:NotBlank @field:Size(min = 12, max = 128) val password: String,
    @field:NotBlank @field:Size(max = 180) val fullName: String,
    @field:Email val email: String? = null,
    val organizationUnitId: UUID? = null,
    @field:Size(min = 1, max = 1) val roleCodes: Set<String>,
    val mustChangePassword: Boolean = true,
)

data class BulkCreateUsersRequest(
    @field:NotBlank @field:Size(max = 120) val operationId: String,
    @field:Valid @field:Size(min = 1, max = 1000) val users: List<CreateUserRequest>,
)

data class BulkCreateUsersResponse(
    val operationId: String,
    val created: List<UserSummary>,
)

data class UpdateUserRequest(
    @field:NotBlank @field:Size(max = 180) val fullName: String,
    @field:Email val email: String? = null,
    val organizationUnitId: UUID? = null,
    @field:Size(min = 1, max = 1) val roleCodes: Set<String>,
    val status: AccountStatus,
)

data class ResetPasswordRequest(@field:NotBlank @field:Size(min = 12, max = 128) val newPassword: String, val forceChangeOnNextLogin: Boolean = true)

data class SessionView(val id: UUID, val issuedAt: Instant, val expiresAt: Instant, val revokedAt: Instant?, val revokedReason: String?, val lastUsedAt: Instant?, val userAgent: String?, val ipAddress: String?, val current: Boolean)

data class RoleRequest(
    @field:NotBlank @field:Size(max = 80) val code: String,
    @field:NotBlank @field:Size(max = 160) val name: String,
    val permissions: Set<String>,
)

data class RoleResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val permissions: Set<String>,
    val systemRole: Boolean,
)

data class GrantInput(
    val roleCode: String? = null,
    val permissionCode: String? = null,
    val scopeType: ScopeType,
    val scopeId: UUID? = null,
    val effect: GrantEffect = GrantEffect.ALLOW,
    val validFrom: Instant? = null,
    val validUntil: Instant? = null,
) {
    @AssertTrue(message = "Phải cung cấp đúng một trong roleCode hoặc permissionCode")
    fun hasExactlyOneSubject(): Boolean = (roleCode == null) xor (permissionCode == null)

    @AssertTrue(message = "scopeId phải null với SYSTEM và bắt buộc với scope khác")
    fun validScope(): Boolean = (scopeType == ScopeType.SYSTEM && scopeId == null) ||
        (scopeType != ScopeType.SYSTEM && scopeId != null)
}

data class BulkGrantRequest(
    @field:NotBlank @field:Size(max = 120) val operationId: String,
    @field:Size(min = 1, max = 1000) val userIds: Set<UUID>,
    @field:Valid @field:Size(min = 1, max = 100) val grants: List<GrantInput>,
)

data class GrantResponse(
    val id: UUID,
    val principalType: PrincipalType,
    val principalId: UUID,
    val permissionCode: String,
    val scopeType: ScopeType,
    val scopeId: UUID?,
    val effect: GrantEffect,
    val validFrom: Instant?,
    val validUntil: Instant?,
)

data class RoleAssignmentResponse(
    val id: UUID,
    val userId: UUID,
    val roleCode: String,
    val scopeType: ScopeType,
    val scopeId: UUID?,
    val effect: GrantEffect,
    val validFrom: Instant?,
    val validUntil: Instant?,
)

data class BulkGrantResponse(
    val operationId: String,
    val permissionGrants: List<GrantResponse>,
    val roleAssignments: List<RoleAssignmentResponse>,
    val duplicateAssignments: Int = 0,
)

data class EffectivePermissionResponse(
    val userId: UUID,
    val scopeType: ScopeType,
    val scopeId: UUID?,
    val allowed: Set<String>,
    val denied: Set<String>,
)

data class RevokeGrantsRequest(
    @field:NotBlank @field:Size(max = 120) val operationId: String,
    @field:Size(max = 5000) val grantIds: Set<UUID> = emptySet(),
    @field:Size(max = 5000) val roleAssignmentIds: Set<UUID> = emptySet(),
) {
    @AssertTrue(message = "Phải có ít nhất một grant hoặc role assignment để thu hồi")
    fun hasTarget(): Boolean = grantIds.isNotEmpty() || roleAssignmentIds.isNotEmpty()
}

data class RevokeGrantsResponse(val permissionGrantsDeleted: Long, val roleAssignmentsDeleted: Long)

data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

enum class UserImportMode { CREATE_ONLY, UPSERT }
enum class UserImportFailurePolicy { ATOMIC, PARTIAL }
enum class UserImportAction { CREATE, UPDATE, SKIP }

data class UserImportMappingRequest(
    @field:NotBlank val codeColumn: String = "code",
    @field:NotBlank val usernameColumn: String = "username",
    @field:NotBlank val fullNameColumn: String = "fullName",
    val emailColumn: String? = "email",
    val organizationUnitIdColumn: String? = "organizationUnitId",
    val roleCodesColumn: String? = "roles",
    val passwordColumn: String? = "password",
    val statusColumn: String? = "status",
    val defaultRoleCodes: Set<String> = setOf("STUDENT"),
    @field:Size(max = 128) val defaultPassword: String? = null,
    val mode: UserImportMode = UserImportMode.CREATE_ONLY,
    val failurePolicy: UserImportFailurePolicy = UserImportFailurePolicy.PARTIAL,
    val updatePasswordOnUpsert: Boolean = false,
)

data class UserImportDetectedMapping(
    val codeColumn: String?,
    val usernameColumn: String?,
    val fullNameColumn: String?,
    val emailColumn: String?,
    val organizationUnitIdColumn: String?,
    val roleCodesColumn: String?,
    val passwordColumn: String?,
    val statusColumn: String?,
)

data class UserImportInspectionResponse(
    val fileName: String,
    val headers: List<String>,
    val samples: List<Map<String, String>>,
    val detectedMapping: UserImportDetectedMapping,
)

data class UserImportRowPreview(
    val rowNumber: Int,
    val code: String,
    val username: String,
    val fullName: String,
    val email: String?,
    val organizationUnitId: UUID?,
    val roleCodes: Set<String>,
    val status: AccountStatus,
    val action: UserImportAction,
    val valid: Boolean,
    val errors: List<String>,
)

data class UserImportPreviewResponse(
    val fileName: String,
    val headers: List<String>,
    val totalRows: Int,
    val validRows: Int,
    val invalidRows: Int,
    val creates: Int,
    val updates: Int,
    val rows: List<UserImportRowPreview>,
)

data class UserImportRowResult(
    val rowNumber: Int,
    val userId: UUID?,
    val code: String,
    val username: String,
    val action: UserImportAction,
    val success: Boolean,
    val errors: List<String>,
)

data class UserImportCommitResponse(
    val operationId: String,
    val fileName: String,
    val totalRows: Int,
    val created: Int,
    val updated: Int,
    val skipped: Int,
    val failed: Int,
    val committed: Boolean,
    val results: List<UserImportRowResult>,
)

/** Dry-run model used by the permission console before a bulk grant is committed. */
data class BulkGrantPreviewRequest(
    @field:Size(min = 1, max = 1000) val userIds: Set<UUID>,
    @field:Valid @field:Size(min = 1, max = 100) val grants: List<GrantInput>,
)

data class UserGrantPreview(
    val userId: UUID,
    val fullName: String,
    val addedPermissions: Set<String>,
    val alreadyAllowed: Set<String>,
    val deniedPermissions: Set<String>,
    val excludedByScope: Set<String>,
)

data class BulkGrantPreviewResponse(
    val affectedUsers: Int,
    val assignmentsToCreate: Int,
    val duplicateAssignments: Int,
    val criticalPermissions: Set<String>,
    val users: List<UserGrantPreview>,
)

data class PermissionSourceResponse(
    val permissionCode: String,
    val sourceType: String,
    val sourceId: UUID?,
    val sourceLabel: String,
    val effect: GrantEffect,
    val scopeType: ScopeType,
    val scopeId: UUID?,
    val validFrom: Instant?,
    val validUntil: Instant?,
    val active: Boolean,
    val applicable: Boolean,
)

data class AuthorizationExplanationResponse(
    val effective: EffectivePermissionResponse,
    val sources: List<PermissionSourceResponse>,
)
