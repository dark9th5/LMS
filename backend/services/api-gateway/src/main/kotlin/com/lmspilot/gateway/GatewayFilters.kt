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
