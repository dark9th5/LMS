package com.lmspilot.configuration.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.configuration.domain.*
import com.lmspilot.contracts.Permissions
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val DEFAULT_PROFILE = "default"
private const val COLOR_PATTERN = "^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$"
private const val THEME_PATTERN = "^(soft-spectrum|executive-midnight|heritage-academy|bright-school|civic-trust|creative-pop|nature-learning|editorial-burgundy|minimal-calm|digital-grid)$"

data class BrandingRequest(
    @field:NotBlank @field:Size(max = 240) val systemName: String,
    @field:Size(max = 10000) val introduction: String? = null,
    val logoFileId: UUID? = null,
    val faviconFileId: UUID? = null,
    val backgroundFileId: UUID? = null,
    @field:Pattern(regexp = THEME_PATTERN) val themeKey: String = "soft-spectrum",
    @field:Pattern(regexp = COLOR_PATTERN) val primaryColor: String,
    @field:Pattern(regexp = COLOR_PATTERN) val secondaryColor: String,
    @field:Pattern(regexp = COLOR_PATTERN) val backgroundColor: String,
    @field:Pattern(regexp = COLOR_PATTERN) val textColor: String,
    @field:Size(max = 253) val customDomain: String? = null,
)

data class BrandingResponse(
    val profileKey: String,
    val systemName: String,
    val introduction: String?,
    val logoFileId: UUID?,
    val faviconFileId: UUID?,
    val backgroundFileId: UUID?,
    val logoUrl: String?,
    val faviconUrl: String?,
    val backgroundUrl: String?,
    val themeKey: String,
    val primaryColor: String,
    val secondaryColor: String,
    val backgroundColor: String,
    val textColor: String,
    val customDomain: String?,
    val updatedAt: Instant,
)

data class ExternalServiceRequest(
    val serviceType: ExternalServiceType,
    @field:NotBlank @field:Size(max = 80) val configKey: String = "default",
    val enabled: Boolean = false,
    val config: Map<String, Any?> = emptyMap(),
    /** Null keeps the previous secret; empty string removes it. */
    val secret: String? = null,
)

data class ExternalServiceResponse(
    val id: UUID,
    val serviceType: ExternalServiceType,
    val configKey: String,
    val enabled: Boolean,
    val config: Map<String, Any?>,
    val secretConfigured: Boolean,
    val healthStatus: ExternalServiceHealth,
    val lastCheckedAt: Instant?,
    val lastError: String?,
    val updatedAt: Instant,
)

@Service
class ConfigurationSecretCipher(@Value("\${configuration.secret-key}") secret: String) {
    private val random = SecureRandom()
    private val key = SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret.toByteArray()), "AES")

    fun encrypt(value: String): ByteArray {
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return ByteBuffer.allocate(nonce.size + encrypted.size).put(nonce).put(encrypted).array()
    }

    fun decrypt(value: ByteArray): String {
        require(value.size > 12) { "Encrypted secret is invalid" }
        val nonce = value.copyOfRange(0, 12)
        val encrypted = value.copyOfRange(12, value.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }
}

@Service
class BrandingService(private val repository: BrandingProfileRepository) {
    @Transactional(readOnly = true)
    fun publicBranding(): BrandingResponse = getEntity().response()

    @Transactional
    fun update(input: BrandingRequest): BrandingResponse {
        val entity = getEntity()
        entity.systemName = input.systemName.trim()
        entity.introduction = input.introduction?.trim()?.takeIf { it.isNotBlank() }
        entity.logoFileId = input.logoFileId
        entity.faviconFileId = input.faviconFileId
        entity.backgroundFileId = input.backgroundFileId
        entity.themeKey = input.themeKey
        entity.primaryColor = input.primaryColor.uppercase()
        entity.secondaryColor = input.secondaryColor.uppercase()
        entity.backgroundColor = input.backgroundColor.uppercase()
        entity.textColor = input.textColor.uppercase()
        entity.customDomain = input.customDomain?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        entity.updatedBy = CurrentUser.id()
        entity.updatedAt = Instant.now()
        return repository.save(entity).response()
    }

    private fun getEntity(): BrandingProfileEntity = repository.findByProfileKey(DEFAULT_PROFILE)
        ?: BrandingProfileEntity(profileKey = DEFAULT_PROFILE)
}

private fun BrandingProfileEntity.response() = BrandingResponse(
    profileKey = profileKey,
    systemName = systemName,
    introduction = introduction,
    logoFileId = logoFileId,
    faviconFileId = faviconFileId,
    backgroundFileId = backgroundFileId,
    logoUrl = logoFileId?.let { "/public/v1/branding/assets/logo" },
    faviconUrl = faviconFileId?.let { "/public/v1/branding/assets/favicon" },
    backgroundUrl = backgroundFileId?.let { "/public/v1/branding/assets/background" },
    themeKey = themeKey,
    primaryColor = primaryColor,
    secondaryColor = secondaryColor,
    backgroundColor = backgroundColor,
    textColor = textColor,
    customDomain = customDomain,
    updatedAt = updatedAt,
)

@Service
class BrandingAssetService(
    private val repository: BrandingProfileRepository,
    restClientBuilder: RestClient.Builder,
    @Value("\${file-storage-service.url:http://localhost:8089}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client = restClientBuilder.baseUrl(baseUrl).build()

    fun content(kind: String): ResponseEntity<ByteArray> {
        val branding = repository.findByProfileKey(DEFAULT_PROFILE)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "BRANDING_NOT_FOUND", "Chưa cấu hình thương hiệu")
        val fileId = when (kind.lowercase()) {
            "logo" -> branding.logoFileId
            "favicon" -> branding.faviconFileId
            "background" -> branding.backgroundFileId
            else -> null
        } ?: throw ApiException(HttpStatus.NOT_FOUND, "BRANDING_ASSET_NOT_FOUND", "Không tìm thấy tài nguyên thương hiệu")
        val upstream = client.get()
            .uri("/internal/v1/files/{id}/content", fileId)
            .header("X-Service-Token", serviceToken)
            .retrieve()
            .toEntity(ByteArray::class.java)
        val contentType = upstream.headers.contentType ?: MediaType.APPLICATION_OCTET_STREAM
        return ResponseEntity.ok()
            .contentType(contentType)
            .cacheControl(CacheControl.noCache())
            .header("X-Content-Type-Options", "nosniff")
            .body(upstream.body ?: ByteArray(0))
    }
}

@Service
class ExternalServiceConfigurationService(
    private val repository: ExternalServiceConfigRepository,
    private val mapper: ObjectMapper,
    private val cipher: ConfigurationSecretCipher,
) {
    @Transactional(readOnly = true)
    fun list(): List<ExternalServiceResponse> = repository.findAllByOrderByServiceTypeAscConfigKeyAsc().map { it.response(mapper) }

    @Transactional
    fun save(id: UUID?, input: ExternalServiceRequest): ExternalServiceResponse {
        val normalizedKey = input.configKey.trim().lowercase()
        val entity = when {
            id != null -> repository.findById(id).orElseThrow { notFound() }
            else -> repository.findByServiceTypeAndConfigKey(input.serviceType, normalizedKey)
                ?: ExternalServiceConfigEntity(serviceType = input.serviceType, configKey = normalizedKey)
        }
        entity.serviceType = input.serviceType
        entity.configKey = normalizedKey
        entity.enabled = input.enabled
        entity.configJson = mapper.writeValueAsString(input.config)
        if (input.secret != null) {
            entity.encryptedSecret = input.secret.takeIf { it.isNotEmpty() }?.let(cipher::encrypt)
            entity.secretKeyVersion = if (entity.encryptedSecret == null) null else 1
        }
        entity.healthStatus = ExternalServiceHealth.UNKNOWN
        entity.lastError = null
        entity.lastCheckedAt = null
        entity.updatedBy = CurrentUser.id()
        entity.updatedAt = Instant.now()
        return repository.save(entity).response(mapper)
    }

    @Transactional
    fun test(id: UUID): ExternalServiceResponse {
        val entity = repository.findById(id).orElseThrow { notFound() }
        val config = mapper.readValue(entity.configJson, object : TypeReference<Map<String, Any?>>() {})
        val secret = entity.encryptedSecret?.let(cipher::decrypt)
        val result = runCatching { performHealthCheck(entity.serviceType, config, secret) }
        entity.lastCheckedAt = Instant.now()
        entity.healthStatus = result.getOrElse { ExternalServiceHealth.UNREACHABLE }
        entity.lastError = result.exceptionOrNull()?.message?.take(1000)
        entity.updatedAt = Instant.now()
        return entity.response(mapper)
    }

    private fun performHealthCheck(type: ExternalServiceType, config: Map<String, Any?>, secret: String?): ExternalServiceHealth = when (type) {
        ExternalServiceType.REDIS -> {
            val host = config["host"]?.toString()?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Thiếu host Redis")
            val port = config["port"]?.toString()?.toIntOrNull() ?: 6379
            Socket().use { it.connect(InetSocketAddress(host, port), 2500) }
            ExternalServiceHealth.HEALTHY
        }
        else -> {
            val endpoint = config["endpoint"]?.toString() ?: config["baseUrl"]?.toString()
                ?: throw IllegalArgumentException("Thiếu endpoint")
            val connection = URI(endpoint).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "GET"
            if (!secret.isNullOrBlank()) connection.setRequestProperty("Authorization", "Bearer $secret")
            val status = connection.responseCode
            if (status in 200..499) ExternalServiceHealth.HEALTHY else ExternalServiceHealth.DEGRADED
        }
    }

    private fun notFound() = ApiException(HttpStatus.NOT_FOUND, "EXTERNAL_SERVICE_NOT_FOUND", "Không tìm thấy cấu hình dịch vụ")
}

private fun ExternalServiceConfigEntity.response(mapper: ObjectMapper): ExternalServiceResponse = ExternalServiceResponse(
    id = id,
    serviceType = serviceType,
    configKey = configKey,
    enabled = enabled,
    config = mapper.readValue(configJson, object : TypeReference<Map<String, Any?>>() {}),
    secretConfigured = encryptedSecret != null,
    healthStatus = healthStatus,
    lastCheckedAt = lastCheckedAt,
    lastError = lastError,
    updatedAt = updatedAt,
)

@RestController
class CustomizationController(
    private val branding: BrandingService,
    private val brandingAssets: BrandingAssetService,
    private val externalServices: ExternalServiceConfigurationService,
) {
    @GetMapping("/public/v1/branding")
    fun publicBranding() = branding.publicBranding()

    @GetMapping("/public/v1/branding/assets/{kind}")
    fun publicBrandingAsset(@PathVariable kind: String) = brandingAssets.content(kind)

    @GetMapping("/api/v1/branding")
    @PreAuthorize("hasAuthority('${Permissions.BRANDING_MANAGE}')")
    fun branding() = branding.publicBranding()

    @PutMapping("/api/v1/branding")
    @PreAuthorize("hasAuthority('${Permissions.BRANDING_MANAGE}')")
    fun updateBranding(@Valid @RequestBody input: BrandingRequest) = branding.update(input)

    @GetMapping("/api/v1/external-services")
    @PreAuthorize("hasAnyAuthority('${Permissions.CONFIGURATION_MANAGE}','${Permissions.INTEGRATIONS_MANAGE}')")
    fun externalServices() = externalServices.list()

    @PostMapping("/api/v1/external-services")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('${Permissions.CONFIGURATION_MANAGE}','${Permissions.INTEGRATIONS_MANAGE}')")
    fun createExternalService(@Valid @RequestBody input: ExternalServiceRequest) = externalServices.save(null, input)

    @PutMapping("/api/v1/external-services/{id}")
    @PreAuthorize("hasAnyAuthority('${Permissions.CONFIGURATION_MANAGE}','${Permissions.INTEGRATIONS_MANAGE}')")
    fun updateExternalService(@PathVariable id: UUID, @Valid @RequestBody input: ExternalServiceRequest) = externalServices.save(id, input)

    @PostMapping("/api/v1/external-services/{id}/test")
    @PreAuthorize("hasAnyAuthority('${Permissions.CONFIGURATION_MANAGE}','${Permissions.INTEGRATIONS_MANAGE}')")
    fun testExternalService(@PathVariable id: UUID) = externalServices.test(id)
}
