package com.lmspilot.support.security

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.nio.charset.StandardCharsets
import java.util.UUID

class JwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val authorities = mutableSetOf<GrantedAuthority>()
        jwt.getClaimAsStringList("roles")?.forEach { authorities += SimpleGrantedAuthority("ROLE_$it") }
        jwt.getClaimAsStringList("permissions")?.forEach { authorities += SimpleGrantedAuthority(it) }
        return JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("username") ?: jwt.subject)
    }
}

@Configuration
class JwtDecoderConfiguration {
    @Bean
    fun jwtDecoder(@Value("\${lmspilot.jwt.secret}") secret: String): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(hmacKey(secret)).build()

    @Bean
    fun jwtAuthenticationConverter(): Converter<Jwt, AbstractAuthenticationToken> = JwtAuthenticationConverter()
}

fun hmacKey(secret: String): SecretKey {
    require(secret.toByteArray(StandardCharsets.UTF_8).size >= 32) { "LMSPILOT_JWT_SECRET must contain at least 32 bytes" }
    return SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
}

object CurrentUser {
    fun jwt(): Jwt = org.springframework.security.core.context.SecurityContextHolder.getContext()
        .authentication.let { auth ->
            (auth as? JwtAuthenticationToken)?.token
                ?: error("Authenticated JWT is required")
        }

    fun id(): UUID = UUID.fromString(jwt().subject)
    fun username(): String = jwt().getClaimAsString("username") ?: jwt().subject
    fun roles(): Set<String> = jwt().getClaimAsStringList("roles")?.toSet() ?: emptySet()
    fun accountType(): String = jwt().getClaimAsString("accountType") ?: "USER"
    fun isSystemAdmin(): Boolean = accountType() == "SYSTEM_ADMIN"
    fun authorities(): Set<String> = (jwt().getClaimAsStringList("permissions") ?: emptyList()).toSet()
}
