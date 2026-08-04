package com.lmspilot.integration.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.Permissions
import com.lmspilot.integration.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.LicenseGuard
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID
import javax.net.ssl.SSLSocketFactory


data class AdapterRequest(
    @field:NotBlank val code: String,
    @field:NotBlank val name: String,
    val type: AdapterType,
    @field:NotBlank val endpoint: String,
    val mapping: Map<String, String> = emptyMap(),
    val secretReference: String = "",
    val status: AdapterStatus = AdapterStatus.DRAFT,
)

data class AdapterResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val type: AdapterType,
    val endpoint: String,
    val mapping: Map<String, String>,
    val secretConfigured: Boolean,
    val status: AdapterStatus,
    val lastTestedAt: Instant?,
    val lastTestResult: String?,
)

@Service
class IntegrationAdapterService(
    private val repository: IntegrationAdapterRepository,
    private val mapper: ObjectMapper,
    private val license: LicenseGuard,
) {
    @Transactional(readOnly = true)
    fun list() = repository.findAll().map { it.response(mapper) }

    @Transactional
    fun save(id: UUID?, input: AdapterRequest): AdapterResponse {
        license.requireFeature(featureFor(input.type))
        validateEndpoint(input.endpoint)
        val entity = id?.let { repository.findById(it).orElseThrow { notFound() } } ?: IntegrationAdapterEntity()
        repository.findByCode(input.code.trim())
            ?.takeIf { it.id != entity.id }
            ?.let { throw ApiException(HttpStatus.CONFLICT, "ADAPTER_CODE_EXISTS", "Mã adapter đã tồn tại") }

        entity.code = input.code.trim()
        entity.name = input.name.trim()
        entity.type = input.type
        entity.endpoint = input.endpoint.trim()
        entity.mappingJson = mapper.writeValueAsString(input.mapping)
        entity.secretReference = input.secretReference.trim()
        entity.status = input.status
        entity.updatedAt = Instant.now()
        return repository.save(entity).response(mapper)
    }

    @Transactional
    fun test(id: UUID): AdapterResponse {
        val entity = repository.findById(id).orElseThrow { notFound() }
        license.requireFeature(featureFor(entity.type), write = false)
        val result = runCatching { testEndpoint(entity.endpoint) }
            .fold(
                onSuccess = { "Kết nối thành công: $it" },
                onFailure = { "Kết nối thất bại: ${(it.message ?: it.javaClass.simpleName).take(1000)}" },
            )
        entity.lastTestedAt = Instant.now()
        entity.lastTestResult = result
        if (result.startsWith("Kết nối thất bại")) entity.status = AdapterStatus.ERROR
        return entity.response(mapper)
    }

    /**
     * This is intentionally a transport-level probe. Credentials stay in the configured secret
     * store and are never returned to the browser. A real adapter may add an authenticated dry run,
     * but the generic registry can at least validate DNS, TCP/TLS and endpoint reachability.
     */
    private fun testEndpoint(value: String): String {
        val uri = validatedUri(value)
        return when (uri.scheme.lowercase()) {
            "http", "https" -> {
                val response = RestClient.create().get().uri(uri).retrieve().toBodilessEntity()
                "HTTP ${response.statusCode.value()}"
            }
            "ldap", "smtp" -> probeSocket(uri, secure = false)
            "ldaps", "smtps" -> probeSocket(uri, secure = true)
            "file" -> {
                val path = Paths.get(uri).normalize()
                require(path.isAbsolute) { "Đường dẫn file phải là đường dẫn tuyệt đối" }
                require(Files.exists(path)) { "Đường dẫn không tồn tại: $path" }
                if (Files.isDirectory(path)) "Thư mục có thể truy cập: $path" else "Tệp có thể truy cập: $path"
            }
            else -> error("Giao thức endpoint chưa được hỗ trợ")
        }
    }

    private fun probeSocket(uri: URI, secure: Boolean): String {
        val host = uri.host?.takeIf { it.isNotBlank() } ?: error("Endpoint thiếu host")
        val port = if (uri.port > 0) uri.port else when (uri.scheme.lowercase()) {
            "ldap" -> 389
            "ldaps" -> 636
            "smtp" -> 25
            "smtps" -> 465
            else -> error("Endpoint thiếu port")
        }
        val socket: Socket = if (secure) SSLSocketFactory.getDefault().createSocket() else Socket()
        socket.use {
            it.connect(InetSocketAddress(host, port), 3_000)
            it.soTimeout = 3_000
            if (secure && it is javax.net.ssl.SSLSocket) it.startHandshake()
        }
        return "${if (secure) "TLS" else "TCP"} $host:$port"
    }

    private fun validateEndpoint(value: String) {
        validatedUri(value)
    }

    private fun validatedUri(value: String): URI {
        val uri = runCatching { URI(value.trim()) }
            .getOrElse { throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_ENDPOINT", "Endpoint không hợp lệ") }
        val scheme = uri.scheme?.lowercase()
        if (scheme !in setOf("http", "https", "ldap", "ldaps", "smtp", "smtps", "file")) {
            throw ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_ENDPOINT", "Giao thức endpoint chưa được hỗ trợ")
        }
        if (scheme != "file" && uri.host.isNullOrBlank()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_ENDPOINT", "Endpoint phải có host")
        }
        if (!uri.userInfo.isNullOrBlank()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "CREDENTIALS_NOT_ALLOWED", "Không đặt thông tin xác thực trực tiếp trong endpoint")
        }
        return uri
    }

    private fun featureFor(type: AdapterType) = if (type in setOf(AdapterType.LDAP, AdapterType.ACTIVE_DIRECTORY)) "LDAP" else "INTEGRATIONS"
    private fun notFound() = ApiException(HttpStatus.NOT_FOUND, "ADAPTER_NOT_FOUND", "Không tìm thấy adapter")
}

private fun IntegrationAdapterEntity.response(mapper: ObjectMapper) = AdapterResponse(
    id = id,
    code = code,
    name = name,
    type = type,
    endpoint = endpoint,
    mapping = mapper.readValue(mappingJson, object : TypeReference<Map<String, String>>() {}),
    secretConfigured = secretReference.isNotBlank(),
    status = status,
    lastTestedAt = lastTestedAt,
    lastTestResult = lastTestResult,
)

@RestController
@RequestMapping("/api/v1/integrations")
@PreAuthorize("hasAuthority('${Permissions.INTEGRATIONS_MANAGE}')")
class IntegrationController(private val service: IntegrationAdapterService) {
    @GetMapping
    fun list() = service.list()

    @PostMapping
    fun create(@Valid @RequestBody input: AdapterRequest) = service.save(null, input)

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody input: AdapterRequest) = service.save(id, input)

    @PostMapping("/{id}/test")
    fun test(@PathVariable id: UUID) = service.test(id)
}
