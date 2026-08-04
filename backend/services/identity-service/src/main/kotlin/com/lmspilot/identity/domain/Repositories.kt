package com.lmspilot.identity.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import java.time.Instant
import java.util.UUID

interface RoleRepository : JpaRepository<RoleEntity, UUID> {
    fun findByCodeIgnoreCase(code: String): RoleEntity?
    fun existsByCodeIgnoreCase(code: String): Boolean
}

interface UserAccountRepository : JpaRepository<UserAccountEntity, UUID> {
    fun findByUsernameIgnoreCase(username: String): UserAccountEntity?
    fun findByCodeIgnoreCase(code: String): UserAccountEntity?
    fun existsByUsernameIgnoreCase(username: String): Boolean
    fun existsByCodeIgnoreCase(code: String): Boolean
    fun countByStatusNot(status: AccountStatus): Long

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

interface AuthorizationGrantRepository : JpaRepository<AuthorizationGrantEntity, UUID> {
    fun findAllByPrincipalTypeAndPrincipalId(principalType: PrincipalType, principalId: UUID): List<AuthorizationGrantEntity>
    fun findAllByPrincipalTypeAndPrincipalIdIn(principalType: PrincipalType, principalIds: Collection<UUID>): List<AuthorizationGrantEntity>
    fun deleteAllByIdInAndPrincipalType(ids: Collection<UUID>, principalType: PrincipalType): Long
}

interface ScopedRoleAssignmentRepository : JpaRepository<ScopedRoleAssignmentEntity, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<ScopedRoleAssignmentEntity>
    fun findAllByUserId(userId: UUID): List<ScopedRoleAssignmentEntity>
    fun deleteAllByIdIn(ids: Collection<UUID>): Long
}

interface BulkOperationRepository : JpaRepository<BulkOperationEntity, String>

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?
    fun findAllByUserIdOrderByIssuedAtDesc(userId: UUID): List<RefreshTokenEntity>
    fun deleteByExpiresAtBefore(cutoff: Instant): Long

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :now, t.revokedReason = :reason where t.userId = :userId and t.revokedAt is null")
    fun revokeAllByUserId(@Param("userId") userId: UUID, @Param("now") now: Instant, @Param("reason") reason: String): Int
}

interface PasswordHistoryRepository : JpaRepository<PasswordHistoryEntity, UUID> {
    fun findTop10ByUserIdOrderByCreatedAtDesc(userId: UUID): List<PasswordHistoryEntity>
}


interface IdentitySystemLockRepository : JpaRepository<IdentitySystemLockEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from IdentitySystemLockEntity l where l.lockName = :lockName")
    fun lock(@Param("lockName") lockName: String): IdentitySystemLockEntity?
}
