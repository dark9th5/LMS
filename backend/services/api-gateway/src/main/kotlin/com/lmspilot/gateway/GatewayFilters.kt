package com.lmspilot.gateway

import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationAndAccessLogFilter : GlobalFilter {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val started = System.nanoTime()
        val existing = exchange.request.headers.getFirst(CORRELATION_HEADER)
        val correlationId = existing?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val request: ServerHttpRequest = exchange.request.mutate().header(CORRELATION_HEADER, correlationId).build()
        exchange.response.headers.set(CORRELATION_HEADER, correlationId)
        return chain.filter(exchange.mutate().request(request).build())
            .doFinally {
                val durationMs = (System.nanoTime() - started) / 1_000_000
                log.info(
                    "access method={} path={} status={} durationMs={} correlationId={}",
                    request.method, request.uri.path, exchange.response.statusCode?.value(), durationMs, correlationId,
                )
            }
    }

    companion object { const val CORRELATION_HEADER = "X-Correlation-Id" }
}

@org.springframework.context.annotation.Configuration
class RateLimitConfiguration {
    @org.springframework.context.annotation.Bean
    fun userOrIpKeyResolver(): KeyResolver = KeyResolver { exchange ->
        ReactiveSecurityContextHolder.getContext()
            .map { it.authentication?.name }
            .filter { !it.isNullOrBlank() }
            .switchIfEmpty(Mono.just(exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"))
    }
}

/**
 * Forces a temporary-password user to replace the password before entering the
 * rest of the platform. The check is at the gateway so hiding UI controls is
 * never the only enforcement layer.
 */
@Component
@Order(-50)
class MustChangePasswordFilter : GlobalFilter {
    private val allowed = setOf(
        "/api/v1/auth/me",
        "/api/v1/auth/change-password",
        "/api/v1/auth/logout",
        "/api/v1/auth/sessions",
    )

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val path = exchange.request.uri.path
        if (path in allowed || path.startsWith("/actuator/")) return chain.filter(exchange)
        return ReactiveSecurityContextHolder.getContext()
            .mapNotNull { it.authentication as? org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken }
            .flatMap { authentication ->
                if (authentication.token.getClaimAsBoolean("mustChangePassword") != true) return@flatMap chain.filter(exchange)
                val response = exchange.response
                response.statusCode = org.springframework.http.HttpStatus.PRECONDITION_REQUIRED
                response.headers.contentType = org.springframework.http.MediaType.APPLICATION_JSON
                val body = """{"code":"PASSWORD_CHANGE_REQUIRED","message":"Bạn phải đổi mật khẩu tạm thời trước khi tiếp tục"}"""
                response.writeWith(Mono.just(response.bufferFactory().wrap(body.toByteArray(java.nio.charset.StandardCharsets.UTF_8))))
            }
            .switchIfEmpty(chain.filter(exchange))
    }
}
