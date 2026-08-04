package com.lmspilot.identity.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.identity.domain.AccountStatus
import com.lmspilot.identity.domain.ScopeType
import com.lmspilot.identity.service.AuthService
import com.lmspilot.identity.service.AuthorizationService
import com.lmspilot.identity.service.UserManagementService
import com.lmspilot.identity.service.UserImportService
import com.lmspilot.support.security.CurrentUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val auth: AuthService) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody input: LoginRequest, request: HttpServletRequest) = auth.login(input, request)

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody input: RefreshRequest, request: HttpServletRequest) = auth.refresh(input.refreshToken, request)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody input: LogoutRequest) = auth.logout(input.refreshToken)

    @GetMapping("/me")
    fun me() = auth.me(CurrentUser.id())

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(@Valid @RequestBody input: ChangePasswordRequest) = auth.changePassword(CurrentUser.id(), input)

    @GetMapping("/sessions")
    fun sessions() = auth.sessions(CurrentUser.id())

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeSession(@PathVariable sessionId: UUID) = auth.revokeSession(CurrentUser.id(), sessionId)

    @DeleteMapping("/sessions")
    fun revokeAllSessions() = mapOf("revoked" to auth.revokeAllSessions(CurrentUser.id()))
}

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val service: UserManagementService,
    private val imports: UserImportService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('${Permissions.USERS_READ}')")
    fun search(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) status: AccountStatus?,
        @RequestParam(required = false) role: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = service.search(query, status, role, page, size)

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('${Permissions.USERS_READ}')")
    fun get(@PathVariable id: UUID) = service.get(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('${Permissions.USERS_CREATE}', '${Permissions.USERS_WRITE}')")
    fun create(@Valid @RequestBody input: CreateUserRequest) = service.create(input)

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('${Permissions.USERS_BULK_MANAGE}', '${Permissions.USERS_WRITE}')")
    fun bulkCreate(@Valid @RequestBody input: BulkCreateUsersRequest) = service.bulkCreate(input)

    @PostMapping("/import/inspect", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAnyAuthority('${Permissions.USERS_BULK_MANAGE}', '${Permissions.USERS_WRITE}')")
    fun inspectImport(@RequestPart("file") file: MultipartFile) = imports.inspect(file)

    @PostMapping("/import/preview", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAnyAuthority('${Permissions.USERS_BULK_MANAGE}', '${Permissions.USERS_WRITE}')")
    fun previewImport(
        @RequestPart("file") file: MultipartFile,
        @Valid @RequestPart("mapping") mapping: UserImportMappingRequest,
    ) = imports.preview(file, mapping)

    @PostMapping("/import/commit", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAnyAuthority('${Permissions.USERS_BULK_MANAGE}', '${Permissions.USERS_WRITE}')")
    fun commitImport(
        @RequestPart("file") file: MultipartFile,
        @Valid @RequestPart("mapping") mapping: UserImportMappingRequest,
        @RequestPart("operationId") operationId: String,
    ) = imports.commit(file, mapping, operationId)

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('${Permissions.USERS_UPDATE}', '${Permissions.USERS_WRITE}')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody input: UpdateUserRequest) = service.update(id, input)

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('${Permissions.USERS_UPDATE}', '${Permissions.USERS_WRITE}')")
    fun resetPassword(@PathVariable id: UUID, @Valid @RequestBody input: ResetPasswordRequest) = service.resetPassword(id, input)
}

@RestController
@RequestMapping("/api/v1/roles")
class RoleController(private val service: UserManagementService) {
    @GetMapping
    @PreAuthorize("hasAnyAuthority('${Permissions.ROLES_READ}', '${Permissions.USERS_READ}')")
    fun list() = service.listRoles()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.ROLES_MANAGE}')")
    fun create(@Valid @RequestBody input: RoleRequest) = service.createRole(input)

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('${Permissions.ROLES_MANAGE}')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody input: RoleRequest) = service.updateRole(id, input)
}

@RestController
@RequestMapping("/api/v1/authorization")
class AuthorizationController(private val service: AuthorizationService) {
    @PostMapping("/grants/preview")
    @PreAuthorize("hasAuthority('${Permissions.AUTHORIZATION_GRANT}')")
    fun previewBulk(@Valid @RequestBody input: BulkGrantPreviewRequest) = service.previewBulk(input)

    @PostMapping("/grants/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.AUTHORIZATION_GRANT}')")
    fun grantBulk(@Valid @RequestBody input: BulkGrantRequest) = service.grantBulk(input)

    @DeleteMapping("/grants/bulk")
    @PreAuthorize("hasAuthority('${Permissions.AUTHORIZATION_REVOKE}')")
    fun revokeBulk(@Valid @RequestBody input: RevokeGrantsRequest) = service.revokeBulk(input)

    @GetMapping("/effective")
    @PreAuthorize("hasAnyAuthority('${Permissions.USERS_READ}', '${Permissions.AUTHORIZATION_GRANT}')")
    fun effective(
        @RequestParam userId: UUID,
        @RequestParam scopeType: ScopeType,
        @RequestParam(required = false) scopeId: UUID?,
    ) = service.effective(userId, scopeType, scopeId)

    @GetMapping("/explain")
    @PreAuthorize("hasAnyAuthority('${Permissions.USERS_READ}', '${Permissions.AUTHORIZATION_GRANT}', '${Permissions.AUTHORIZATION_REVOKE}')")
    fun explain(
        @RequestParam userId: UUID,
        @RequestParam scopeType: ScopeType,
        @RequestParam(required = false) scopeId: UUID?,
    ) = service.explain(userId, scopeType, scopeId)
}


data class InternalUserContactResponse(
    val userId: UUID,
    val username: String,
    val fullName: String,
    val email: String?,
    val active: Boolean,
)

@RestController
@RequestMapping("/internal/v1/users")
class InternalUserController(
    private val service: UserManagementService,
    private val internal: com.lmspilot.support.security.InternalTokenAuthorizer,
) {
    @GetMapping("/{id}/contact")
    fun contact(
        @PathVariable id: UUID,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ): InternalUserContactResponse {
        internal.require(token)
        val user = service.get(id)
        return InternalUserContactResponse(
            user.id,
            user.username,
            user.fullName,
            user.email,
            user.status == AccountStatus.ACTIVE,
        )
    }
}


@RestController
@RequestMapping("/api/v1/users/{userId}/sessions")
class UserSessionAdminController(private val auth: AuthService) {
    @GetMapping
    @PreAuthorize("hasAuthority('${Permissions.USERS_SESSIONS_MANAGE}')")
    fun list(@PathVariable userId: UUID) = auth.sessions(userId)

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('${Permissions.USERS_SESSIONS_MANAGE}')")
    fun revoke(@PathVariable userId: UUID, @PathVariable sessionId: UUID) = auth.revokeSession(userId, sessionId, "ADMIN_REVOKED")

    @DeleteMapping
    @PreAuthorize("hasAuthority('${Permissions.USERS_SESSIONS_MANAGE}')")
    fun revokeAll(@PathVariable userId: UUID) = mapOf("revoked" to auth.revokeAllSessions(userId, "ADMIN_REVOKED_ALL"))
}
