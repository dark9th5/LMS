package com.lmspilot.identity.service

import com.lmspilot.identity.domain.UserAccountEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.Base64

@Service
class TokenService(
    private val jwtEncoder: JwtEncoder,
    private val authorization: AuthorizationService,
    @Value("\${identity.access-token-ttl:PT15M}") private val accessTtl: Duration,
    @Value("\${identity.refresh-token-ttl:P7D}") val refreshTtl: Duration,
) {
    private val random = SecureRandom()

    fun issueAccessToken(user: UserAccountEntity, sessionId: UUID): Pair<String, Long> {
        val now = Instant.now()
        val expiry = now.plus(accessTtl)
        val roles = user.roles.map { it.code }.toSet()
        val permissions = authorization.permissionsForToken(user)
        val globalPermissions = authorization.globalPermissionsForToken(user)
        val claims = JwtClaimsSet.builder()
            .issuer("lmspilot-identity")
            .issuedAt(now)
            .expiresAt(expiry)
            .subject(user.id.toString())
            .claim("username", user.username)
            .claim("fullName", user.fullName)
            .claim("accountType", user.accountType.name)
            .claim("sid", sessionId.toString())
            .claim("mustChangePassword", user.mustChangePassword)
            .claim("roles", roles)
            .claim("permissions", permissions)
            .claim("globalPermissions", globalPermissions)
            .build()
        val token = jwtEncoder.encode(
            JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims),
        ).tokenValue
        return token to accessTtl.seconds
    }

    fun newRefreshToken(): String {
        val bytes = ByteArray(48)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hashRefreshToken(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }

}
