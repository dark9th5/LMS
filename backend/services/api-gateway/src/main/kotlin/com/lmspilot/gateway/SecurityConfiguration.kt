package com.lmspilot.gateway

import java.nio.charset.StandardCharsets
import javax.crypto.spec.SecretKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration {
    @Bean
    fun reactiveJwtDecoder(@Value("\${lmspilot.jwt.secret}") secret: String): ReactiveJwtDecoder {
        require(secret.toByteArray(StandardCharsets.UTF_8).size >= 32) { "JWT secret must be at least 32 bytes" }
        return NimbusReactiveJwtDecoder.withSecretKey(SecretKeySpec(secret.toByteArray(), "HmacSHA256")).build()
    }

    @Bean
    fun gatewaySecurity(
        http: ServerHttpSecurity,
        decoder: ReactiveJwtDecoder,
    ): SecurityWebFilterChain {
        val converter = Converter<Jwt, AbstractAuthenticationToken> { jwt ->
            val authorities = mutableSetOf<SimpleGrantedAuthority>()
            jwt.getClaimAsStringList("roles")?.forEach { authorities += SimpleGrantedAuthority("ROLE_$it") }
            jwt.getClaimAsStringList("permissions")?.forEach { authorities += SimpleGrantedAuthority(it) }
            JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("username") ?: jwt.subject)
        }
        return http
            .csrf { it.disable() }
            .authorizeExchange {
                it.pathMatchers(
                    "/actuator/health/**", "/actuator/info",
                    "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout",
                    "/public/v1/configuration", "/public/v1/branding", "/public/v1/branding/**", "/public/v1/certificates/**", "/public/v1/file-edit/**",
                ).permitAll()
                it.anyExchange().authenticated()
            }
            .oauth2ResourceServer {
                it.jwt { jwt ->
                    jwt.jwtDecoder(decoder)
                    jwt.jwtAuthenticationConverter(ReactiveJwtAuthenticationConverterAdapter(converter))
                }
            }
            .build()
    }
}
