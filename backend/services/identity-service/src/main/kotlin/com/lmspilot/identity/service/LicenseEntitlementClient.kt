package com.lmspilot.identity.service

import com.lmspilot.support.api.ApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Instant

data class LicenseEntitlements(
    val licenseId: String = "unknown",
    val edition: String = "UNKNOWN",
    val maxUsers: Int = 0,
    val features: Set<String> = emptySet(),
    val status: String = "INVALID",
    val readOnly: Boolean = true,
    val expiresAt: Instant? = null,
    val graceEndsAt: Instant? = null,
)

@Service
class LicenseEntitlementClient(
    builder: RestClient.Builder,
    @Value("\${license-service.url:http://localhost:8090}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
    @Value("\${identity.license-cache-ttl:PT1M}") private val cacheTtl: Duration,
    @Value("\${identity.license-stale-tolerance:PT15M}") private val staleTolerance: Duration,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = builder.baseUrl(baseUrl).build()

    @Volatile private var cached: CachedEntitlements? = null

    fun requireWritable(): LicenseEntitlements {
        val entitlements = current()
        if (entitlements.readOnly || entitlements.status in setOf("EXPIRED", "INVALID")) {
            throw ApiException(HttpStatus.LOCKED, "LICENSE_READ_ONLY", "License không cho phép thay đổi dữ liệu")
        }
        return entitlements
    }

    fun requireUserCapacity(currentUsers: Long, additionalUsers: Int) {
        if (additionalUsers <= 0) return
        val entitlements = requireWritable()
        if (currentUsers + additionalUsers > entitlements.maxUsers.toLong()) {
            throw ApiException(
                HttpStatus.CONFLICT,
                "LICENSE_USER_LIMIT",
                "License cho phép tối đa ${entitlements.maxUsers} tài khoản đang sử dụng",
            )
        }
    }

    fun requireFeature(feature: String, write: Boolean = true) {
        val entitlements = if (write) requireWritable() else current()
        if (feature.uppercase() !in entitlements.features.map(String::uppercase).toSet()) {
            throw ApiException(HttpStatus.FORBIDDEN, "FEATURE_NOT_LICENSED", "Tính năng $feature không có trong license hiện tại")
        }
    }

    fun current(): LicenseEntitlements {
        val now = Instant.now()
        cached?.takeIf { Duration.between(it.loadedAt, now) <= cacheTtl }?.let { return it.value }
        return runCatching {
            client.get()
                .uri("/internal/v1/license/entitlements")
                .header("X-Service-Token", serviceToken)
                .retrieve()
                .body(LicenseEntitlements::class.java)
                ?: throw IllegalStateException("License service trả dữ liệu trống")
        }.onSuccess { cached = CachedEntitlements(it, now) }
            .getOrElse { cause ->
                val fallback = cached?.takeIf { Duration.between(it.loadedAt, now) <= staleTolerance }
                if (fallback != null) {
                    log.warn("License service unavailable; using recently cached entitlements", cause)
                    fallback.value
                } else {
                    log.error("License service unavailable and no acceptable cache exists", cause)
                    throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "LICENSE_SERVICE_UNAVAILABLE", "Không thể xác nhận trạng thái license")
                }
            }
    }

    private data class CachedEntitlements(val value: LicenseEntitlements, val loadedAt: Instant)
}
