package com.lmspilot.support.security

import com.lmspilot.support.api.ApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Instant

/**
 * Central client used by business services to enforce the active offline license.
 * A short cache avoids coupling each write request to license-service while a bounded
 * stale window keeps already-running on-premise installations usable during a brief
 * service restart. Once the stale window expires, writes fail closed.
 */
data class LicenseEntitlementsView(
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
class LicenseGuard(
    builder: RestClient.Builder,
    @Value("\${license-service.url:http://localhost:8090}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
    @Value("\${lmspilot.license-cache-ttl:PT1M}") private val cacheTtl: Duration,
    @Value("\${lmspilot.license-stale-tolerance:PT15M}") private val staleTolerance: Duration,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = builder.baseUrl(baseUrl).build()

    @Volatile
    private var cached: CacheEntry? = null

    fun current(): LicenseEntitlementsView {
        val now = Instant.now()
        cached?.takeIf { Duration.between(it.loadedAt, now) <= cacheTtl }?.let { return it.value }
        return runCatching {
            client.get()
                .uri("/internal/v1/license/entitlements")
                .header("X-Service-Token", serviceToken)
                .retrieve()
                .body(LicenseEntitlementsView::class.java)
                ?: error("License service returned an empty body")
        }.onSuccess { cached = CacheEntry(it, now) }
            .getOrElse { cause ->
                cached?.takeIf { Duration.between(it.loadedAt, now) <= staleTolerance }?.let {
                    log.warn("license-service unavailable; using a recently cached entitlement snapshot", cause)
                    return it.value
                }
                log.error("license-service unavailable and no acceptable cached entitlement exists", cause)
                throw ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "LICENSE_SERVICE_UNAVAILABLE",
                    "Không thể xác nhận trạng thái license",
                )
            }
    }

    fun requireWritable(): LicenseEntitlementsView {
        val value = current()
        if (value.readOnly || value.status in setOf("EXPIRED", "INVALID")) {
            throw ApiException(HttpStatus.LOCKED, "LICENSE_READ_ONLY", "License hiện chỉ cho phép đọc dữ liệu")
        }
        return value
    }

    fun requireFeature(feature: String, write: Boolean = true): LicenseEntitlementsView {
        val value = if (write) requireWritable() else current()
        val normalized = feature.trim().uppercase()
        if (normalized !in value.features.map(String::uppercase).toSet()) {
            throw ApiException(
                HttpStatus.FORBIDDEN,
                "FEATURE_NOT_LICENSED",
                "Tính năng $normalized không có trong license hiện tại",
            )
        }
        return value
    }

    fun validateEnabledFeatures(enabledFeatureKeys: Set<String>) {
        val value = requireWritable()
        val licensed = value.features.map(String::uppercase).toSet()
        val unsupported = enabledFeatureKeys.map(String::uppercase).toSet() - licensed
        if (unsupported.isNotEmpty()) {
            throw ApiException(
                HttpStatus.CONFLICT,
                "FEATURE_FLAG_EXCEEDS_LICENSE",
                "Không thể bật tính năng chưa có trong license: ${unsupported.sorted().joinToString()}",
            )
        }
    }

    private data class CacheEntry(val value: LicenseEntitlementsView, val loadedAt: Instant)
}
