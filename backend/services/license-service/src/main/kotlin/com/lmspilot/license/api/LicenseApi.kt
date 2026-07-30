package com.lmspilot.license.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.Permissions
import com.lmspilot.license.domain.*
import com.lmspilot.support.api.ApiException
import jakarta.validation.Valid
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
import java.time.Instant
import java.util.Base64
import java.util.UUID

data class LicensePayload(val licenseId: String, val organization: String, val edition: String = "STANDARD", val maxUsers: Int = 100, val features: Set<String> = emptySet(), val issuedAt: Instant, val expiresAt: Instant? = null, val machineFingerprint: String? = null)
data class ActivateLicenseRequest(@field:NotBlank val payload: String, @field:NotBlank val signature: String)
data class LicenseResponse(val id: UUID, val licenseId: String, val organization: String, val edition: String, val maxUsers: Int, val features: Set<String>, val issuedAt: Instant, val expiresAt: Instant?, val status: LicenseStatus, val activatedAt: Instant)

@Service
class LicenseManagementService(
    private val repository: LicenseRepository,
    private val mapper: ObjectMapper,
    @Value("\${license.public-key:}") private val publicKeyBase64: String,
    @Value("\${license.allow-development:true}") private val allowDevelopment: Boolean,
    @Value("\${license.machine-fingerprint:development}") private val machineFingerprint: String,
) {
    @Transactional(readOnly = true)
    fun current(): LicenseResponse {
        val latest = repository.findTopByOrderByActivatedAtDesc() ?: return developmentLicense()
        if (latest.expiresAt?.isBefore(Instant.now()) == true && latest.status == LicenseStatus.ACTIVE) latest.status = LicenseStatus.EXPIRED
        return latest.response(mapper)
    }

    @Transactional
    fun activate(input: ActivateLicenseRequest): LicenseResponse {
        val payloadBytes = runCatching { Base64.getUrlDecoder().decode(input.payload) }.getOrElse { throw invalid("Payload license không hợp lệ") }
        val signatureBytes = runCatching { Base64.getUrlDecoder().decode(input.signature) }.getOrElse { throw invalid("Chữ ký license không hợp lệ") }
        if (!verify(payloadBytes, signatureBytes)) throw invalid("Không xác minh được chữ ký license")
        val payload = runCatching { mapper.readValue(payloadBytes, LicensePayload::class.java) }.getOrElse { throw invalid("Nội dung license không hợp lệ") }
        if (payload.machineFingerprint != null && payload.machineFingerprint != machineFingerprint) throw invalid("License không thuộc máy chủ này")
        if (payload.expiresAt?.isBefore(Instant.now()) == true) throw ApiException(HttpStatus.CONFLICT, "LICENSE_EXPIRED", "License đã hết hạn")
        repository.findByLicenseId(payload.licenseId)?.let { repository.delete(it) }
        return repository.save(LicenseEntity(licenseId = payload.licenseId, organization = payload.organization, edition = payload.edition, maxUsers = payload.maxUsers, featuresJson = mapper.writeValueAsString(payload.features), issuedAt = payload.issuedAt, expiresAt = payload.expiresAt, status = LicenseStatus.ACTIVE, sourcePayload = input.payload)).response(mapper)
    }

    private fun verify(payload: ByteArray, signatureBytes: ByteArray): Boolean {
        if (publicKeyBase64.isBlank()) return allowDevelopment && String(payload).contains("\"licenseId\":\"development")
        return runCatching {
            val key = KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)))
            Signature.getInstance("Ed25519").run { initVerify(key); update(payload); verify(signatureBytes) }
        }.getOrDefault(false)
    }

    private fun developmentLicense() = LicenseResponse(UUID(0, 1), "development", "LMSPilot Development", "ENTERPRISE", 10000, setOf("AI", "LDAP", "REPORT_EXPORT", "CUSTOM_THEME"), Instant.EPOCH, null, LicenseStatus.DEVELOPMENT, Instant.EPOCH)
    private fun invalid(message: String) = ApiException(HttpStatus.BAD_REQUEST, "INVALID_LICENSE", message)
}
private fun LicenseEntity.response(mapper: ObjectMapper): LicenseResponse = LicenseResponse(id, licenseId, organization, edition, maxUsers, mapper.readValue(featuresJson, mapper.typeFactory.constructCollectionType(Set::class.java, String::class.java)), issuedAt, expiresAt, status, activatedAt)

@RestController
@RequestMapping("/api/v1/license")
class LicenseController(private val service: LicenseManagementService) {
    @GetMapping @PreAuthorize("hasAuthority('${Permissions.LICENSE_MANAGE}')") fun current() = service.current()
    @PostMapping("/activate") @PreAuthorize("hasAuthority('${Permissions.LICENSE_MANAGE}')") fun activate(@Valid @RequestBody input: ActivateLicenseRequest) = service.activate(input)
}
