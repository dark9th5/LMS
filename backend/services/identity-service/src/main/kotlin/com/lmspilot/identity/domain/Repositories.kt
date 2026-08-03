package com.lmspilot.identity.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface RoleRepository : JpaRepository<RoleEntity, UUID> {
    fun findByCodeIgnoreCase(code: String): RoleEntity?
    fun existsByCodeIgnoreCase(code: String): Boolean
}

interface UserAccountRepository : JpaRepository<UserAccountEntity, UUID> {
    fun findByUsernameIgnoreCase(username: String): UserAccountEntity?
    fun existsByUsernameIgnoreCase(username: String): Boolean
    fun existsByCodeIgnoreCase(code: String): Boolean

    @Query("""
        select distinct u from UserAccountEntity u left join u.roles r
        where (:query is null or lower(u.username) like lower(concat('%', cast(:query as string), '%'))
            or lower(u.fullName) like lower(concat('%', cast(:query as string), '%'))
            or lower(u.code) like lower(concat('%', cast(:query as string), '%')))
          and (:status is null or u.status = :status)
          and (:role is null or lower(r.code) = lower(cast(:role as string)))
    """)
    fun search(query: String?, status: AccountStatus?, role: String?, pageable: Pageable): Page<UserAccountEntity>
}

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?
    fun deleteByExpiresAtBefore(cutoff: Instant): Long
}
