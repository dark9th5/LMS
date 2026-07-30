package com.lmspilot.support.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableMethodSecurity
class StandardSecurityConfiguration {
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain::class)
    fun standardSecurityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt, org.springframework.security.authentication.AbstractAuthenticationToken>,
    ): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/actuator/health/**", "/actuator/info", "/error", "/internal/v1/**", "/public/v1/**").permitAll()
            it.anyRequest().authenticated()
        }
        .oauth2ResourceServer { resource -> resource.jwt { it.jwtAuthenticationConverter(jwtAuthenticationConverter) } }
        .build()
}
