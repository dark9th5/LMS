package com.lmspilot.identity.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.*
import com.lmspilot.identity.api.*
import com.lmspilot.identity.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class UserManagementService(
    private val users: UserAccountRepository,
    private val roles: RoleRepository,
    private val bulkOperations: BulkOperationRepository,
    private val bulkGuard: BulkOperationGuard,
    private val passwordPolicy: PasswordPolicyService,
    private val refreshTokens: RefreshTokenRepository,
    private val authorization: AuthorizationService,
    private val capacity: UserCapacityService,
    private val organization: OrganizationScopeClient,
    private val events: DomainEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun search(query: String?, status: AccountStatus?, role: String?, page: Int, size: Int): PageResponse<UserSummary> {
        val result = users.search(
            query?.takeIf { it.isNotBlank() }, status, role?.takeIf { it.isNotBlank() },
            PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by("createdAt").descending()),
        )
        return PageResponse(result.content.map { it.toSummary(authorization.capabilities(it.id)) }, result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): UserSummary = users.findById(id).orElseThrow { notFound() }.let { it.toSummary(authorization.capabilities(it.id)) }

    @Transactional
    fun create(input: CreateUserRequest): UserSummary {
        organization.requireActiveUnit(input.organizationUnitId)
        capacity.requireCapacity(1)
        return createEntity(input).let { it.toSummary(authorization.capabilities(it.id)) }
    }

    @Transactional
    fun bulkCreate(input: BulkCreateUsersRequest): BulkCreateUsersResponse {
        val requestedBy = CurrentUser.id()
        bulkGuard.replay(input.operationId, "BULK_CREATE_USERS", requestedBy)?.let {
            return objectMapper.readValue(it.resultJson, BulkCreateUsersResponse::class.java)
        }

        val normalizedCodes = input.users.map { it.code.trim().lowercase() }
        val normalizedUsernames = input.users.map { it.username.trim().lowercase() }
        if (normalizedCodes.size != normalizedCodes.toSet().size) conflict("Mã người dùng bị trùng trong tệp nhập")
        if (normalizedUsernames.size != normalizedUsernames.toSet().size) conflict("Tên đăng nhập bị trùng trong tệp nhập")

        input.users.forEach { validateUnique(it) }
        val requestedUnits = input.users.mapNotNull { it.organizationUnitId }.toSet()
        val activeUnits = organization.existingActiveUnitIds(requestedUnits)
        val invalidUnits = requestedUnits - activeUnits
        if (invalidUnits.isNotEmpty()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "ORGANIZATION_UNIT_INVALID", "Đơn vị không tồn tại hoặc đã ngừng hoạt động: ${invalidUnits.joinToString()}")
        }
        capacity.requireCapacity(input.users.size)
        val created = input.users.map { createEntity(it, validateUnique = false).let { created -> created.toSummary(authorization.capabilities(created.id)) } }
        val response = BulkCreateUsersResponse(input.operationId, created)
        bulkOperations.save(
            BulkOperationEntity(
                operationId = input.operationId,
                operationType = "BULK_CREATE_USERS",
                requestedBy = requestedBy,
                resultJson = objectMapper.writeValueAsString(response),
            )
        )
        return response
    }

    @Transactional
    fun update(id: UUID, input: UpdateUserRequest): UserSummary {
        val user = users.findById(id).orElseThrow { notFound() }
        protectBootstrapAccount(user, input)
        organization.requireActiveUnit(input.organizationUnitId)
        if (user.status == AccountStatus.DISABLED && input.status != AccountStatus.DISABLED) capacity.requireCapacity(1)
        user.fullName = input.fullName.trim()
        user.email = input.email?.trim()
        user.organizationUnitId = input.organizationUnitId
        val rolesChanged = user.roles.map { it.code }.toSet() != input.roleCodes.map(String::uppercase).toSet()
        user.roles = resolveRoles(input.roleCodes)
        user.status = input.status
        if (rolesChanged || input.status != AccountStatus.ACTIVE) refreshTokens.revokeAllByUserId(user.id, Instant.now(), if (rolesChanged) "ROLE_CHANGED" else "ACCOUNT_STATUS_CHANGED")
        user.updatedAt = Instant.now()
        events.publish(
            EventTypes.USER_STATUS_CHANGED,
            "identity-service",
            user.id.toString(),
            mapOf("userId" to user.id, "status" to user.status.name),
        )
        return user.toSummary(authorization.capabilities(user.id))
    }

    @Transactional
    fun resetPassword(id: UUID, input: ResetPasswordRequest) {
        val user = users.findById(id).orElseThrow { notFound() }
        passwordPolicy.change(user, input.newPassword, input.forceChangeOnNextLogin, "ADMIN_PASSWORD_RESET")
    }

    @Transactional(readOnly = true)
    fun listRoles(): List<RoleResponse> = roles.findAll(Sort.by("name")).map { it.toResponse() }

    @Transactional
    fun createRole(input: RoleRequest): RoleResponse {
        if (roles.existsByCodeIgnoreCase(input.code)) conflict("Mã vai trò đã tồn tại")
        validatePermissions(input.permissions)
        return roles.save(
            RoleEntity(
                code = input.code.trim().uppercase(),
                name = input.name.trim(),
                permissions = input.permissions.toMutableSet(),
            )
        ).toResponse()
    }

    @Transactional
    fun updateRole(id: UUID, input: RoleRequest): RoleResponse {
        val role = roles.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "Không tìm thấy vai trò")
        }
        validatePermissions(input.permissions)
        if (role.systemRole && role.code == "ADMIN" && !input.permissions.containsAll(DefaultRolePermissions.ADMIN)) {
            throw ApiException(
                HttpStatus.CONFLICT,
                "PROTECTED_ROLE",
                "Không thể thu hồi quyền lõi của vai trò quản trị hệ thống",
            )
        }
        role.name = input.name.trim()
        role.permissions = input.permissions.toMutableSet()
        role.updatedAt = Instant.now()
        return role.toResponse()
    }

    private fun createEntity(input: CreateUserRequest, validateUnique: Boolean = true): UserAccountEntity {
        if (validateUnique) validateUnique(input)
        val assignedRoles = resolveRoles(input.roleCodes)
        val user = users.save(
            UserAccountEntity(
                code = input.code.trim(),
                username = input.username.trim().lowercase(),
                passwordHash = passwordPolicy.encodeInitial(input.password, input.username.trim().lowercase(), input.code.trim()),
                fullName = input.fullName.trim(),
                email = input.email?.trim(),
                organizationUnitId = input.organizationUnitId,
                accountType = AccountType.USER,
                protectedAccount = false,
                mustChangePassword = input.mustChangePassword,
                passwordChangedAt = Instant.now(),
                roles = assignedRoles,
            )
        )
        events.publish(
            EventTypes.USER_CREATED,
            "identity-service",
            user.id.toString(),
            UserCreatedPayload(
                user.id,
                user.username,
                user.fullName,
                user.organizationUnitId,
                user.roles.map { it.code }.toSet(),
            ),
        )
        return user
    }

    private fun validateUnique(input: CreateUserRequest) {
        if (users.existsByCodeIgnoreCase(input.code.trim())) conflict("Mã người dùng đã tồn tại: ${input.code}")
        if (users.existsByUsernameIgnoreCase(input.username.trim())) conflict("Tên đăng nhập đã tồn tại: ${input.username}")
    }

    private fun protectBootstrapAccount(user: UserAccountEntity, input: UpdateUserRequest) {
        if (!user.protectedAccount) return
        if (input.status != AccountStatus.ACTIVE) {
            throw ApiException(HttpStatus.CONFLICT, "PROTECTED_ACCOUNT", "Không thể khóa tài khoản quản trị gốc")
        }
        if (input.roleCodes.none { it.equals("ADMIN", ignoreCase = true) }) {
            throw ApiException(HttpStatus.CONFLICT, "PROTECTED_ACCOUNT", "Tài khoản quản trị gốc phải giữ role ADMIN")
        }
    }

    private fun resolveRoles(codes: Set<String>): MutableSet<RoleEntity> {
        if (codes.isEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "ROLE_REQUIRED", "Phải gán ít nhất một vai trò")
        return codes.map { code ->
            roles.findByCodeIgnoreCase(code)
                ?: throw ApiException(HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND", "Vai trò $code không tồn tại")
        }.toMutableSet()
    }

    private fun validatePermissions(input: Set<String>) {
        val known = Permissions::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .mapNotNull { runCatching { it.get(null) as? String }.getOrNull() }
            .toSet()
        val unknown = input - known
        if (unknown.isNotEmpty()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_PERMISSION", "Quyền không hợp lệ: ${unknown.joinToString()}")
        }
    }

    private fun notFound() = ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng")
    private fun conflict(message: String): Nothing = throw ApiException(HttpStatus.CONFLICT, "DUPLICATE_USER", message)
}

fun RoleEntity.toResponse() = RoleResponse(id, code, name, permissions.toSet(), systemRole)
