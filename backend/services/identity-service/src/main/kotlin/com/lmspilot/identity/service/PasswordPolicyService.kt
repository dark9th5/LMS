package com.lmspilot.identity.service

import com.lmspilot.contracts.EventTypes
import com.lmspilot.identity.domain.PasswordHistoryEntity
import com.lmspilot.identity.domain.PasswordHistoryRepository
import com.lmspilot.identity.domain.RefreshTokenRepository
import com.lmspilot.identity.domain.UserAccountEntity
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PasswordPolicyService(
    private val encoder: PasswordEncoder,
    private val history: PasswordHistoryRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val events: DomainEventPublisher,
    @Value("\${identity.password-policy.min-length:12}") private val minLength: Int,
    @Value("\${identity.password-policy.require-uppercase:true}") private val requireUppercase: Boolean,
    @Value("\${identity.password-policy.require-lowercase:true}") private val requireLowercase: Boolean,
    @Value("\${identity.password-policy.require-digit:true}") private val requireDigit: Boolean,
    @Value("\${identity.password-policy.require-special:true}") private val requireSpecial: Boolean,
    @Value("\${identity.password-policy.history-count:5}") private val historyCount: Int,
) {
    fun encodeInitial(raw: String, username: String, code: String): String {
        validate(raw, username, code, emptyList())
        return encoder.encode(raw)
    }

    fun change(user: UserAccountEntity, raw: String, forceChange: Boolean, reason: String) {
        val previous = history.findTop10ByUserIdOrderByCreatedAtDesc(user.id)
            .take(historyCount.coerceIn(0, 10))
            .map { it.passwordHash } + user.passwordHash
        validate(raw, user.username, user.code, previous)
        if (user.passwordHash.isNotBlank()) history.save(PasswordHistoryEntity(userId = user.id, passwordHash = user.passwordHash))
        user.passwordHash = encoder.encode(raw)
        user.passwordChangedAt = Instant.now()
        user.mustChangePassword = forceChange
        user.failedLoginCount = 0
        user.lockedUntil = null
        user.updatedAt = Instant.now()
        refreshTokens.revokeAllByUserId(user.id, Instant.now(), reason)
        events.publish(EventTypes.USER_PASSWORD_CHANGED, "identity-service", user.id.toString(), mapOf("userId" to user.id, "reason" to reason, "mustChangePassword" to forceChange))
    }

    private fun validate(raw: String, username: String, code: String, previousHashes: List<String>) {
        val problems = mutableListOf<String>()
        if (raw.length < minLength.coerceAtLeast(12)) problems += "Mật khẩu phải có ít nhất ${minLength.coerceAtLeast(12)} ký tự"
        if (requireUppercase && raw.none(Char::isUpperCase)) problems += "Mật khẩu phải có chữ hoa"
        if (requireLowercase && raw.none(Char::isLowerCase)) problems += "Mật khẩu phải có chữ thường"
        if (requireDigit && raw.none(Char::isDigit)) problems += "Mật khẩu phải có chữ số"
        if (requireSpecial && raw.all { it.isLetterOrDigit() }) problems += "Mật khẩu phải có ký tự đặc biệt"
        val normalized = raw.lowercase()
        if (username.length >= 3 && normalized.contains(username.lowercase())) problems += "Mật khẩu không được chứa tên đăng nhập"
        if (code.length >= 3 && normalized.contains(code.lowercase())) problems += "Mật khẩu không được chứa mã người dùng"
        if (previousHashes.any { encoder.matches(raw, it) }) problems += "Mật khẩu không được trùng với ${historyCount.coerceAtLeast(1)} mật khẩu gần nhất"
        if (problems.isNotEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_POLICY_VIOLATION", problems.joinToString("; "))
    }
}
