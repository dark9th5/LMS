package com.lmspilot.license.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.Permissions
import com.lmspilot.license.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.InternalTokenAuthorizer
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

private const val MAX_CLOCK_SKEW_SECONDS = 86_400L

data class LicensePayload(
    val licenseId: String,
    val organization: String,
    val edition: String = "STANDARD",
    @field:Min(1) val maxUsers: Int = 100,
    val features: Set<String> = emptySet(),
    val issuedAt: Instant,
    val expiresAt: Instant? = null,
    @field:Min(0) @field:Max(3650) val gracePeriodDays: Int = 0,
    val machineFingerprint: String? = null,
)

data class ActivateLicenseRequest(@field:NotBlank val payload: String, @field:NotBlank val signature: String)

data class LicenseResponse(
    val id: UUID,
    val licenseId: String,
    val organization: String,
    val edition: String,
    val maxUsers: Int,
    val features: Set<String>,
    val issuedAt: Instant,
    val expiresAt: Instant?,
    val gracePeriodDays: Int,
    val graceEndsAt: Instant?,
    val status: LicenseStatus,
    val readOnly: Boolean,
    val activatedAt: Instant,
)

data class LicenseEntitlementsResponse(
    val licenseId: String,
    val edition: String,
    val maxUsers: Int,
    val features: Set<String>,
    val status: LicenseStatus,
    val readOnly: Boolean,
    val expiresAt: Instant?,
    val graceEndsAt: Instant?,
)

@Service
class LicenseManagementService(
    private val repository: LicenseRepository,
    private val mapper: ObjectMapper,
    @Value("\${license.public-key:}") private val publicKeyBase64: String,
    @Value("\${license.allow-development:false}") private val allowDevelopment: Boolean,
    @Value("\${license.machine-fingerprint:development}") private val machineFingerprint: String,
) {
    @Transactional(readOnly = true)
    fun current(): LicenseResponse = repository.findTopByOrderByActivatedAtDesc()?.response(mapper, Instant.now())
        ?: if (allowDevelopment) developmentLicense() else missingLicense()

    @Transactional(readOnly = true)
    fun entitlements(): LicenseEntitlementsResponse {
        val current = current()
        return LicenseEntitlementsResponse(
            current.licenseId,
            current.edition,
            current.maxUsers,
            current.features,
            current.status,
            current.readOnly,
            current.expiresAt,
            current.graceEndsAt,
        )
    }

    @Transactional
    fun activate(input: ActivateLicenseRequest): LicenseResponse {
        val payloadBytes = runCatching { Base64.getUrlDecoder().decode(input.payload) }
            .getOrElse { throw invalid("Payload license không hợp lệ") }
        val signatureBytes = runCatching { Base64.getUrlDecoder().decode(input.signature) }
            .getOrElse { throw invalid("Chữ ký license không hợp lệ") }
        if (!verify(payloadBytes, signatureBytes)) throw invalid("Không xác minh được chữ ký license")
        val payload = runCatching { mapper.readValue(payloadBytes, LicensePayload::class.java) }
            .getOrElse { throw invalid("Nội dung license không hợp lệ") }
        validatePayload(payload)

        val entity = repository.findByLicenseId(payload.licenseId) ?: LicenseEntity(licenseId = payload.licenseId)
        entity.organization = payload.organization.trim()
        entity.edition = payload.edition.trim().uppercase()
        entity.maxUsers = payload.maxUsers
        entity.featuresJson = mapper.writeValueAsString(payload.features.map(String::uppercase).toSortedSet())
        entity.issuedAt = payload.issuedAt
        entity.expiresAt = payload.expiresAt
        entity.gracePeriodDays = payload.gracePeriodDays
        entity.status = LicenseStatus.ACTIVE
        entity.sourcePayload = input.payload
        entity.activatedAt = Instant.now()
        return repository.save(entity).response(mapper, Instant.now())
    }

    private fun validatePayload(payload: LicensePayload) {
        if (payload.licenseId.isBlank() || payload.licenseId.length > 100) throw invalid("Mã license không hợp lệ")
        if (payload.organization.isBlank() || payload.organization.length > 220) throw invalid("Tên tổ chức không hợp lệ")
        if (payload.edition.isBlank() || payload.edition.length > 80) throw invalid("Gói license không hợp lệ")
        if (payload.maxUsers < 1) throw invalid("Giới hạn người dùng phải lớn hơn 0")
        if (payload.gracePeriodDays !in 0..3650) throw invalid("Grace period không hợp lệ")
        if (payload.issuedAt.isAfter(Instant.now().plusSeconds(MAX_CLOCK_SKEW_SECONDS))) throw invalid("Ngày phát hành license nằm trong tương lai")
        if (payload.expiresAt != null && !payload.expiresAt.isAfter(payload.issuedAt)) throw invalid("Ngày hết hạn phải sau ngày phát hành")
        if (payload.machineFingerprint != null && payload.machineFingerprint != machineFingerprint) {
            throw invalid("License không thuộc máy chủ này")
        }
        val graceEndsAt = payload.expiresAt?.plus(Duration.ofDays(payload.gracePeriodDays.toLong()))
        if (graceEndsAt?.isBefore(Instant.now()) == true) {
            throw ApiException(HttpStatus.CONFLICT, "LICENSE_EXPIRED", "License và thời gian gia hạn đã hết")
        }
    }

    private fun verify(payload: ByteArray, signatureBytes: ByteArray): Boolean {
        if (publicKeyBase64.isBlank()) {
            return allowDevelopment && runCatching {
                val value = mapper.readValue(payload, LicensePayload::class.java)
                value.licenseId == "development"
            }.getOrDefault(false)
        }
        return runCatching {
            val key = KeyFactory.getInstance("Ed25519")
                .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)))
            Signature.getInstance("Ed25519").run {
                initVerify(key)
                update(payload)
                verify(signatureBytes)
            }
        }.getOrDefault(false)
    }

    private fun missingLicense() = LicenseResponse(
        id = UUID(0, 0),
        licenseId = "missing",
        organization = "Unlicensed installation",
        edition = "NONE",
        maxUsers = 0,
        features = emptySet(),
        issuedAt = Instant.EPOCH,
        expiresAt = null,
        gracePeriodDays = 0,
        graceEndsAt = null,
        status = LicenseStatus.INVALID,
        readOnly = true,
        activatedAt = Instant.EPOCH,
    )

    private fun developmentLicense() = LicenseResponse(
        id = UUID(0, 1),
        licenseId = "development",
        organization = "LMSPilot Development",
        edition = "ENTERPRISE",
        maxUsers = 10000,
        features = setOf("AI", "LDAP", "REPORT_EXPORT", "CUSTOM_THEME", "INTEGRATIONS", "GAMIFICATION"),
        issuedAt = Instant.EPOCH,
        expiresAt = null,
        gracePeriodDays = 0,
        graceEndsAt = null,
        status = LicenseStatus.DEVELOPMENT,
        readOnly = false,
        activatedAt = Instant.EPOCH,
    )

    private fun invalid(message: String) = ApiException(HttpStatus.BAD_REQUEST, "INVALID_LICENSE", message)
}

private fun LicenseEntity.response(mapper: ObjectMapper, now: Instant): LicenseResponse {
    val graceEndsAt = expiresAt?.plus(Duration.ofDays(gracePeriodDays.toLong()))
    val effectiveStatus = when {
        status == LicenseStatus.INVALID -> LicenseStatus.INVALID
        expiresAt == null || !now.isAfter(expiresAt) -> LicenseStatus.ACTIVE
        graceEndsAt != null && !now.isAfter(graceEndsAt) -> LicenseStatus.GRACE_PERIOD
        else -> LicenseStatus.EXPIRED
    }
    return LicenseResponse(
        id,
        licenseId,
        organization,
        edition,
        maxUsers,
        mapper.readValue(featuresJson, mapper.typeFactory.constructCollectionType(Set::class.java, String::class.java)),
        issuedAt,
        expiresAt,
        gracePeriodDays,
        graceEndsAt,
        effectiveStatus,
        effectiveStatus in setOf(LicenseStatus.EXPIRED, LicenseStatus.INVALID),
        activatedAt,
    )
}

@RestController
@RequestMapping("/api/v1/license")
class LicenseController(private val service: LicenseManagementService) {
    @GetMapping
    @PreAuthorize("hasAuthority('${Permissions.LICENSE_MANAGE}')")
    fun current() = service.current()

    @PostMapping("/activate")
    @PreAuthorize("hasAuthority('${Permissions.LICENSE_MANAGE}')")
    fun activate(@Valid @RequestBody input: ActivateLicenseRequest) = service.activate(input)
}

@RestController
@RequestMapping("/internal/v1/license")
class InternalLicenseController(
    private val service: LicenseManagementService,
    private val internal: InternalTokenAuthorizer,
) {
    @GetMapping("/entitlements")
    fun entitlements(@RequestHeader("X-Service-Token", required = false) token: String?): LicenseEntitlementsResponse {
        internal.require(token)
        return service.entitlements()
    }
}
