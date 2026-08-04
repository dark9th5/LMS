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
enum class AccountType { SYSTEM_ADMIN, USER }
enum class PrincipalType { USER, ROLE }
enum class ScopeType { SYSTEM, BRANCH, DEPARTMENT, GROUP, COURSE, EXAM }
enum class GrantEffect { ALLOW, DENY }

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

    /** Compatibility-only primary unit. Membership is owned by organization-service. */
    var organizationUnitId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: AccountStatus = AccountStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var accountType: AccountType = AccountType.USER,

    /** Bootstrap account cannot be disabled, deleted or downgraded through normal APIs. */
    @Column(nullable = false)
    var protectedAccount: Boolean = false,

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
    var mustChangePassword: Boolean = true,

    var passwordChangedAt: Instant? = null,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Version
    var version: Long = 0,
)

@Entity
@Table(
    name = "authorization_grants",
    indexes = [
        Index(name = "idx_auth_grant_principal", columnList = "principal_type,principal_id"),
        Index(name = "idx_auth_grant_scope", columnList = "scope_type,scope_id"),
    ],
)
class AuthorizationGrantEntity(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false, length = 20)
    var principalType: PrincipalType = PrincipalType.USER,

    @Column(name = "principal_id", nullable = false)
    var principalId: UUID = UUID.randomUUID(),

    @Column(name = "permission_code", nullable = false, length = 120)
    var permissionCode: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    var scopeType: ScopeType = ScopeType.SYSTEM,

    @Column(name = "scope_id")
    var scopeId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var effect: GrantEffect = GrantEffect.ALLOW,

    var validFrom: Instant? = null,
    var validUntil: Instant? = null,

    @Column(nullable = false)
    var createdBy: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(
    name = "scoped_role_assignments",
    indexes = [
        Index(name = "idx_scoped_role_user", columnList = "user_id"),
        Index(name = "idx_scoped_role_scope", columnList = "scope_type,scope_id"),
    ],
)
class ScopedRoleAssignmentEntity(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    var role: RoleEntity = RoleEntity(),

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    var scopeType: ScopeType = ScopeType.SYSTEM,

    @Column(name = "scope_id")
    var scopeId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var effect: GrantEffect = GrantEffect.ALLOW,

    var validFrom: Instant? = null,
    var validUntil: Instant? = null,

    @Column(nullable = false)
    var createdBy: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "bulk_operations")
class BulkOperationEntity(
    @Id
    @Column(length = 120)
    var operationId: String = "",

    @Column(nullable = false, length = 80)
    var operationType: String = "",

    @Column(nullable = false)
    var requestedBy: UUID = UUID.randomUUID(),

    @Column(nullable = false, columnDefinition = "text")
    var resultJson: String = "{}",

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
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

    @Column(length = 120)
    var revokedReason: String? = null,

    var lastUsedAt: Instant? = null,

    @Column(length = 255)
    var userAgent: String? = null,

    @Column(length = 80)
    var ipAddress: String? = null,
)


@Entity
@Table(name = "password_history", indexes = [Index(name = "idx_password_history_user", columnList = "user_id,created_at")])
class PasswordHistoryEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(name = "password_hash", nullable = false, length = 255) var passwordHash: String = "",
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "identity_system_locks")
class IdentitySystemLockEntity(
    @Id
    @Column(name = "lock_name", nullable = false, length = 120)
    var lockName: String = "",
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
