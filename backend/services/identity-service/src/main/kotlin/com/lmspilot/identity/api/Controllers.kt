package com.lmspilot.identity.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.identity.domain.AccountStatus
import com.lmspilot.identity.service.AuthService
import com.lmspilot.identity.service.UserManagementService
import com.lmspilot.support.security.CurrentUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
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
    fun me() = mapOf("id" to CurrentUser.id(), "username" to CurrentUser.username(), "roles" to CurrentUser.roles())
}

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val service: UserManagementService) {
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
    @PreAuthorize("hasAuthority('${Permissions.USERS_WRITE}')")
    fun create(@Valid @RequestBody input: CreateUserRequest) = service.create(input)

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('${Permissions.USERS_WRITE}')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody input: UpdateUserRequest) = service.update(id, input)

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('${Permissions.USERS_WRITE}')")
    fun resetPassword(@PathVariable id: UUID, @Valid @RequestBody input: ResetPasswordRequest) = service.resetPassword(id, input)
}

@RestController
@RequestMapping("/api/v1/roles")
class RoleController(private val service: UserManagementService) {
    @GetMapping
    @PreAuthorize("hasAuthority('${Permissions.USERS_READ}')")
    fun list() = service.listRoles()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.ROLES_MANAGE}')")
    fun create(@Valid @RequestBody input: RoleRequest) = service.createRole(input)

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('${Permissions.ROLES_MANAGE}')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody input: RoleRequest) = service.updateRole(id, input)
}
