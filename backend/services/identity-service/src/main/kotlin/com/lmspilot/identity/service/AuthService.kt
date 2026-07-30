package com.lmspilot.identity.service

import com.lmspilot.contracts.AuditPayload
import com.lmspilot.contracts.EventTypes
import com.lmspilot.identity.api.LoginRequest
import com.lmspilot.identity.api.TokenResponse
import com.lmspilot.identity.api.UserSummary
import com.lmspilot.identity.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AuthService(
    private val users: UserAccountRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
    private val events: DomainEventPublisher,
) {
    @Transactional
    fun login(input: LoginRequest, request: HttpServletRequest): TokenResponse {
        val user = users.findByUsernameIgnoreCase(input.username.trim())
            ?: throw invalidCredentials()
        val now = Instant.now()
        if (user.status != AccountStatus.ACTIVE || (user.lockedUntil?.isAfter(now) == true)) {
            audit(user, "LOGIN_DENIED", "DENIED", request)
            throw ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "Tài khoản đang bị khóa hoặc ngừng sử dụng")
        }
        if (!passwordEncoder.matches(input.password, user.passwordHash)) {
            user.failedLoginCount += 1
            if (user.failedLoginCount >= 5) user.lockedUntil = now.plusSeconds(15 * 60)
            user.updatedAt = now
            audit(user, "LOGIN_FAILED", "FAILED", request)
            throw invalidCredentials()
        }
        user.failedLoginCount = 0
        user.lockedUntil = null
        user.lastLoginAt = now
        user.updatedAt = now
        val access = tokenService.issueAccessToken(user)
        val rawRefresh = tokenService.newRefreshToken()
        refreshTokens.save(
            RefreshTokenEntity(
                userId = user.id,
                tokenHash = tokenService.hashRefreshToken(rawRefresh),
                expiresAt = now.plus(tokenService.refreshTtl),
                userAgent = request.getHeader("User-Agent")?.take(255),
                ipAddress = request.remoteAddr?.take(80),
            )
        )
        audit(user, "LOGIN_SUCCESS", "SUCCESS", request)
        return TokenResponse(access.first, rawRefresh, expiresInSeconds = access.second, user = user.toSummary())
    }

    @Transactional
    fun refresh(rawToken: String, request: HttpServletRequest): TokenResponse {
        val stored = refreshTokens.findByTokenHash(tokenService.hashRefreshToken(rawToken))
            ?: throw ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token không hợp lệ")
        if (stored.revokedAt != null || stored.expiresAt.isBefore(Instant.now())) {
            throw ApiException(HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN", "Refresh token đã hết hiệu lực")
        }
        val user = users.findById(stored.userId).orElseThrow { invalidCredentials() }
        if (user.status != AccountStatus.ACTIVE) throw ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "Tài khoản không hoạt động")
        stored.revokedAt = Instant.now()
        val newRaw = tokenService.newRefreshToken()
        refreshTokens.save(
            RefreshTokenEntity(
                userId = user.id,
                tokenHash = tokenService.hashRefreshToken(newRaw),
                expiresAt = Instant.now().plus(tokenService.refreshTtl),
                userAgent = request.getHeader("User-Agent")?.take(255),
                ipAddress = request.remoteAddr?.take(80),
            )
        )
        val access = tokenService.issueAccessToken(user)
        return TokenResponse(access.first, newRaw, expiresInSeconds = access.second, user = user.toSummary())
    }

    @Transactional
    fun logout(rawToken: String) {
        refreshTokens.findByTokenHash(tokenService.hashRefreshToken(rawToken))?.let { it.revokedAt = Instant.now() }
    }

    private fun invalidCredentials() = ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng")

    private fun audit(user: UserAccountEntity, action: String, outcome: String, request: HttpServletRequest) {
        events.publish(
            EventTypes.AUDIT_RECORDED, "identity-service", user.id.toString(),
            AuditPayload(user.id.toString(), user.username, action, "UserAccount", user.id.toString(), outcome, ipAddress = request.remoteAddr),
        )
    }
}

fun UserAccountEntity.toSummary(): UserSummary = UserSummary(
    id = id,
    code = code,
    username = username,
    fullName = fullName,
    email = email,
    organizationUnitId = organizationUnitId,
    status = status,
    roles = roles.map { it.code }.toSet(),
    permissions = roles.flatMap { it.permissions }.toSet(),
    lastLoginAt = lastLoginAt,
)
