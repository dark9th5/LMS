package com.lmspilot.identity.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "roles")
class RoleEntity(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 80)
    var code: String = "",

    @Column(nullable = false, length = 160)
    var name: String = "",

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = [JoinColumn(name = "role_id")])
    @Column(name = "permission", nullable = false, length = 120)
    var permissions: MutableSet<String> = mutableSetOf(),

    @Column(nullable = false)
    var systemRole: Boolean = false,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Version
    var version: Long = 0,
)

enum class AccountStatus { ACTIVE, LOCKED, DISABLED }

@Entity
@Table(name = "user_accounts")
class UserAccountEntity(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 80)
    var code: String = "",

    @Column(nullable = false, unique = true, length = 120)
    var username: String = "",

    @Column(nullable = false, length = 255)
    var passwordHash: String = "",

    @Column(nullable = false, length = 180)
    var fullName: String = "",

    @Column(length = 180)
    var email: String? = null,

    var organizationUnitId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: AccountStatus = AccountStatus.ACTIVE,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    var roles: MutableSet<RoleEntity> = mutableSetOf(),

    @Column(nullable = false)
    var failedLoginCount: Int = 0,

    var lockedUntil: Instant? = null,

    var lastLoginAt: Instant? = null,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Version
    var version: Long = 0,
)

@Entity
@Table(name = "refresh_tokens", indexes = [Index(name = "idx_refresh_hash", columnList = "token_hash", unique = true)])
class RefreshTokenEntity(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    var tokenHash: String = "",

    @Column(nullable = false)
    var issuedAt: Instant = Instant.now(),

    @Column(nullable = false)
    var expiresAt: Instant = Instant.now(),

    var revokedAt: Instant? = null,

    @Column(length = 255)
    var userAgent: String? = null,

    @Column(length = 80)
    var ipAddress: String? = null,
)
