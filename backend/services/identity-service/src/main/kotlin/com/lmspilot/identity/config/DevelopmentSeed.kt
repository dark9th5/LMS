package com.lmspilot.identity.config

import com.lmspilot.contracts.DefaultRolePermissions
import com.lmspilot.identity.domain.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class DevelopmentSeed(
    private val roles: RoleRepository,
    private val users: UserAccountRepository,
    private val encoder: PasswordEncoder,
    @Value("\${lmspilot.seed-demo:false}") private val enabled: Boolean,
    @Value("\${lmspilot.default-admin-password}") private val password: String,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled) return
        val admin = ensureRole("ADMIN", "Quản trị viên", DefaultRolePermissions.ADMIN)
        val instructor = ensureRole("INSTRUCTOR", "Giảng viên", DefaultRolePermissions.INSTRUCTOR)
        val learner = ensureRole("LEARNER", "Học viên", DefaultRolePermissions.LEARNER)
        ensureUser(UUID.fromString("00000000-0000-0000-0000-000000000001"), "ADM001", "admin", "Quản trị hệ thống", admin, AccountType.SYSTEM_ADMIN, true)
        ensureUser(UUID.fromString("00000000-0000-0000-0000-000000000002"), "INS001", "instructor", "Giảng viên mẫu", instructor)
        ensureUser(UUID.fromString("00000000-0000-0000-0000-000000000003"), "LEA001", "learner", "Học viên mẫu", learner)
        log.warn("Development seed is enabled. Change temporary passwords immediately outside local development.")
    }

    private fun ensureRole(code: String, name: String, permissions: Set<String>): RoleEntity =
        roles.findByCodeIgnoreCase(code) ?: roles.save(RoleEntity(code = code, name = name, permissions = permissions.toMutableSet(), systemRole = true))

    private fun ensureUser(id: UUID, code: String, username: String, fullName: String, role: RoleEntity, accountType: AccountType = AccountType.USER, protectedAccount: Boolean = false) {
        if (users.findByUsernameIgnoreCase(username) == null) {
            users.save(UserAccountEntity(id = id, code = code, username = username, fullName = fullName, passwordHash = encoder.encode(password), roles = mutableSetOf(role), accountType = accountType, protectedAccount = protectedAccount, mustChangePassword = !protectedAccount, passwordChangedAt = java.time.Instant.now()))
        }
    }
}
