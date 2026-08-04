package com.lmspilot.identity.security

import com.lmspilot.support.security.hmacKey
import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableMethodSecurity
class IdentitySecurityConfiguration {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun jwtEncoder(@Value("\${lmspilot.jwt.secret}") secret: String): JwtEncoder =
        NimbusJwtEncoder(ImmutableSecret<SecurityContext>(hmacKey(secret)))

    @Bean
    fun identitySecurityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt, org.springframework.security.authentication.AbstractAuthenticationToken>,
    ): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/actuator/health/**", "/actuator/info", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout", "/internal/v1/**", "/error").permitAll()
            it.anyRequest().authenticated()
        }
        .oauth2ResourceServer { resource -> resource.jwt { it.jwtAuthenticationConverter(jwtAuthenticationConverter) } }
        .build()
}
