package com.lmspilot.identity.config

import com.lmspilot.contracts.DefaultAccessProfiles
import com.lmspilot.identity.domain.RoleEntity
import com.lmspilot.identity.domain.RoleRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Ensures permission-first access profiles exist in every environment.
 * These profiles are composable templates for USER accounts, not account types.
 */
@Component
class SystemAccessProfileBootstrap(
    private val roles: RoleRepository,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        DefaultAccessProfiles.profiles.forEach { profile ->
            val existing = roles.findByCodeIgnoreCase(profile.code)
            if (existing == null) {
                roles.save(
                    RoleEntity(
                        code = profile.code,
                        name = profile.name,
                        permissions = profile.permissions.toMutableSet(),
                        systemRole = true,
                    )
                )
            } else if (existing.systemRole) {
                existing.name = profile.name
                existing.permissions = profile.permissions.toMutableSet()
                existing.updatedAt = Instant.now()
            }
        }
    }
}
