package com.lmspilot.identity.api

import com.lmspilot.identity.domain.AccountStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String,
)

data class RefreshRequest(@field:NotBlank val refreshToken: String)
data class LogoutRequest(@field:NotBlank val refreshToken: String)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: UserSummary,
)

data class UserSummary(
    val id: UUID,
    val code: String,
    val username: String,
    val fullName: String,
    val email: String?,
    val organizationUnitId: UUID?,
    val status: AccountStatus,
    val roles: Set<String>,
    val permissions: Set<String>,
    val lastLoginAt: Instant?,
)

data class CreateUserRequest(
    @field:NotBlank @field:Size(max = 80) val code: String,
    @field:NotBlank @field:Size(min = 3, max = 120) val username: String,
    @field:NotBlank @field:Size(min = 12, max = 128) val password: String,
    @field:NotBlank @field:Size(max = 180) val fullName: String,
    @field:Email val email: String? = null,
    val organizationUnitId: UUID? = null,
    @field:NotEmpty val roleCodes: Set<String>,
)

data class UpdateUserRequest(
    @field:NotBlank @field:Size(max = 180) val fullName: String,
    @field:Email val email: String? = null,
    val organizationUnitId: UUID? = null,
    @field:NotEmpty val roleCodes: Set<String>,
    val status: AccountStatus,
)

data class ResetPasswordRequest(@field:NotBlank @field:Size(min = 12, max = 128) val newPassword: String)

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

data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
