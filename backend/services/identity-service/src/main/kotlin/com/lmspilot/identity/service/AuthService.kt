package com.lmspilot.identity.service

import com.lmspilot.contracts.AuditPayload
import com.lmspilot.contracts.EventTypes
import com.lmspilot.identity.api.ChangePasswordRequest
import com.lmspilot.identity.api.LoginRequest
import com.lmspilot.identity.api.SessionView
import com.lmspilot.identity.api.TokenResponse
import com.lmspilot.identity.api.UserSummary
import com.lmspilot.identity.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
    private val users: UserAccountRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val passwordPolicy: PasswordPolicyService,
    private val tokenService: TokenService,
    private val authorization: AuthorizationService,
    private val ldap: LdapAuthenticationService,
    private val events: DomainEventPublisher,
    @Value("\${identity.lockout.max-failed-attempts:5}") private val maxFailedAttempts: Int,
    @Value("\${identity.lockout.duration:PT15M}") private val lockoutDuration: Duration,
) {
    @Transactional
    fun login(input: LoginRequest, request: HttpServletRequest): TokenResponse {
        val user = users.findByUsernameIgnoreCase(input.username.trim()) ?: throw invalidCredentials()
        val now = Instant.now()
        if (user.status != AccountStatus.ACTIVE || (user.lockedUntil?.isAfter(now) == true)) {
            audit(user, "LOGIN_DENIED", "DENIED", request)
            throw ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "Tài khoản đang bị khóa hoặc ngừng sử dụng")
        }
        val localAuthenticated = passwordEncoder.matches(input.password, user.passwordHash)
        val ldapAuthenticated = !localAuthenticated && !user.protectedAccount && ldap.authenticate(user.username, input.password)
        if (!localAuthenticated && !ldapAuthenticated) {
            user.failedLoginCount += 1
            if (user.failedLoginCount >= maxFailedAttempts.coerceAtLeast(1)) user.lockedUntil = now.plus(lockoutDuration)
            user.updatedAt = now
            audit(user, "LOGIN_FAILED", "FAILED", request)
            throw invalidCredentials()
        }
        user.failedLoginCount = 0
        user.lockedUntil = null
        user.lastLoginAt = now
        user.updatedAt = now
        val rawRefresh = tokenService.newRefreshToken()
        val session = refreshTokens.save(
            RefreshTokenEntity(
                userId = user.id,
                tokenHash = tokenService.hashRefreshToken(rawRefresh),
                expiresAt = now.plus(tokenService.refreshTtl),
                lastUsedAt = now,
                userAgent = request.getHeader("User-Agent")?.take(255),
                ipAddress = request.remoteAddr?.take(80),
            )
        )
        val access = tokenService.issueAccessToken(user, session.id)
        audit(user, if (ldapAuthenticated) "LOGIN_SUCCESS_LDAP" else "LOGIN_SUCCESS_LOCAL", "SUCCESS", request)
        return TokenResponse(access.first, rawRefresh, expiresInSeconds = access.second, user = user.toSummary(authorization.capabilities(user.id)))
    }

    @Transactional
    fun refresh(rawToken: String, request: HttpServletRequest): TokenResponse {
        val stored = refreshTokens.findByTokenHash(tokenService.hashRefreshToken(rawToken))
            ?: throw ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token không hợp lệ")
        val now = Instant.now()
        if (stored.revokedAt != null) {
            // A rotated token being presented again is a likely token theft signal.
            refreshTokens.revokeAllByUserId(stored.userId, now, "REFRESH_TOKEN_REUSE_DETECTED")
            events.publish(EventTypes.USER_SESSION_REVOKED, "identity-service", stored.userId.toString(), mapOf("userId" to stored.userId, "reason" to "REFRESH_TOKEN_REUSE_DETECTED"))
            throw ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED", "Phiên đăng nhập đã bị thu hồi do phát hiện token được sử dụng lại")
        }
        if (stored.expiresAt.isBefore(now)) {
            stored.revokedAt = now
            stored.revokedReason = "EXPIRED"
            throw ApiException(HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN", "Refresh token đã hết hiệu lực")
        }
        val user = users.findById(stored.userId).orElseThrow { invalidCredentials() }
        if (user.status != AccountStatus.ACTIVE) throw ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "Tài khoản không hoạt động")
        stored.revokedAt = now
        stored.revokedReason = "ROTATED"
        stored.lastUsedAt = now
        val newRaw = tokenService.newRefreshToken()
        val session = refreshTokens.save(
            RefreshTokenEntity(
                userId = user.id,
                tokenHash = tokenService.hashRefreshToken(newRaw),
                expiresAt = now.plus(tokenService.refreshTtl),
                lastUsedAt = now,
                userAgent = request.getHeader("User-Agent")?.take(255),
                ipAddress = request.remoteAddr?.take(80),
            )
        )
        val access = tokenService.issueAccessToken(user, session.id)
        return TokenResponse(access.first, newRaw, expiresInSeconds = access.second, user = user.toSummary(authorization.capabilities(user.id)))
    }

    @Transactional(readOnly = true)
    fun me(userId: UUID): UserSummary = users.findById(userId).orElseThrow { invalidCredentials() }.toSummary(authorization.capabilities(userId))

    @Transactional
    fun changePassword(userId: UUID, input: ChangePasswordRequest) {
        val user = users.findById(userId).orElseThrow { invalidCredentials() }
        if (!passwordEncoder.matches(input.currentPassword, user.passwordHash)) throw ApiException(HttpStatus.UNAUTHORIZED, "CURRENT_PASSWORD_INVALID", "Mật khẩu hiện tại không đúng")
        passwordPolicy.change(user, input.newPassword, forceChange = false, reason = "PASSWORD_CHANGED")
    }

    @Transactional(readOnly = true)
    fun sessions(userId: UUID): List<SessionView> {
        val currentSessionId = CurrentUser.jwt().getClaimAsString("sid")?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        return refreshTokens.findAllByUserIdOrderByIssuedAtDesc(userId).map {
            SessionView(it.id, it.issuedAt, it.expiresAt, it.revokedAt, it.revokedReason, it.lastUsedAt, it.userAgent, it.ipAddress, it.id == currentSessionId)
        }
    }

    @Transactional
    fun revokeSession(userId: UUID, sessionId: UUID, reason: String = "USER_REVOKED") {
        val session = refreshTokens.findById(sessionId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Không tìm thấy phiên đăng nhập") }
        if (session.userId != userId) throw ApiException(HttpStatus.FORBIDDEN, "SESSION_SCOPE_DENIED", "Không thể thu hồi phiên của người dùng khác")
        if (session.revokedAt == null) {
            session.revokedAt = Instant.now()
            session.revokedReason = reason
            events.publish(EventTypes.USER_SESSION_REVOKED, "identity-service", userId.toString(), mapOf("userId" to userId, "sessionId" to sessionId, "reason" to reason))
        }
    }

    @Transactional
    fun revokeAllSessions(userId: UUID, reason: String = "USER_REVOKED_ALL"): Int {
        val count = refreshTokens.revokeAllByUserId(userId, Instant.now(), reason)
        events.publish(EventTypes.USER_SESSION_REVOKED, "identity-service", userId.toString(), mapOf("userId" to userId, "reason" to reason, "count" to count))
        return count
    }

    @Transactional
    fun logout(rawToken: String) {
        refreshTokens.findByTokenHash(tokenService.hashRefreshToken(rawToken))?.let {
            if (it.revokedAt == null) {
                it.revokedAt = Instant.now()
                it.revokedReason = "LOGOUT"
            }
        }
    }

    private fun invalidCredentials() = ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng")

    private fun audit(user: UserAccountEntity, action: String, outcome: String, request: HttpServletRequest) {
        events.publish(EventTypes.AUDIT_RECORDED, "identity-service", user.id.toString(), AuditPayload(user.id.toString(), user.username, action, "UserAccount", user.id.toString(), outcome, ipAddress = request.remoteAddr))
    }
}

fun UserAccountEntity.toSummary(effectivePermissions: Set<String>? = null): UserSummary = UserSummary(
    id = id,
    code = code,
    username = username,
    fullName = fullName,
    email = email,
    organizationUnitId = organizationUnitId,
    status = status,
    accountType = accountType,
    protectedAccount = protectedAccount,
    roles = roles.map { it.code }.toSet(),
    primaryRole = roles.singleOrNull()?.code ?: "STUDENT",
    permissions = effectivePermissions ?: roles.flatMap { it.permissions }.toSet(),
    lastLoginAt = lastLoginAt,
    mustChangePassword = mustChangePassword,
)
