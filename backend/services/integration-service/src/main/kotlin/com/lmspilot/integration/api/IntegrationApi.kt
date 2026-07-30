package com.lmspilot.integration.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.Permissions
import com.lmspilot.integration.domain.*
import com.lmspilot.support.api.ApiException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.Instant
import java.util.UUID

data class AdapterRequest(@field:NotBlank val code: String, @field:NotBlank val name: String, val type: AdapterType, @field:NotBlank val endpoint: String, val mapping: Map<String,String> = emptyMap(), val secretReference: String = "", val status: AdapterStatus = AdapterStatus.DRAFT)
data class AdapterResponse(val id: UUID, val code: String, val name: String, val type: AdapterType, val endpoint: String, val mapping: Map<String,String>, val secretConfigured: Boolean, val status: AdapterStatus, val lastTestedAt: Instant?, val lastTestResult: String?)

@Service
class IntegrationAdapterService(private val repository: IntegrationAdapterRepository, private val mapper: ObjectMapper) {
    @Transactional(readOnly = true) fun list() = repository.findAll().map { it.response(mapper) }
    @Transactional fun save(id: UUID?, input: AdapterRequest): AdapterResponse {
        validateEndpoint(input.endpoint)
        val entity = id?.let { repository.findById(it).orElseThrow { notFound() } } ?: IntegrationAdapterEntity()
        repository.findByCode(input.code)?.takeIf { it.id != entity.id }?.let { throw ApiException(HttpStatus.CONFLICT, "ADAPTER_CODE_EXISTS", "Mã adapter đã tồn tại") }
        entity.code=input.code.trim(); entity.name=input.name.trim(); entity.type=input.type; entity.endpoint=input.endpoint.trim(); entity.mappingJson=mapper.writeValueAsString(input.mapping); entity.secretReference=input.secretReference; entity.status=input.status; entity.updatedAt=Instant.now()
        return repository.save(entity).response(mapper)
    }
    @Transactional fun test(id: UUID): AdapterResponse {
        val entity = repository.findById(id).orElseThrow { notFound() }
        val result = runCatching { RestClient.create().get().uri(entity.endpoint).retrieve().toBodilessEntity().statusCode.toString() }.fold({ "Kết nối thành công: $it" }, { "Kết nối thất bại: ${it.message}" })
        entity.lastTestedAt=Instant.now(); entity.lastTestResult=result; if(result.startsWith("Kết nối thất bại")) entity.status=AdapterStatus.ERROR
        return entity.response(mapper)
    }
    private fun validateEndpoint(value: String) { val uri=runCatching{URI(value)}.getOrElse{throw ApiException(HttpStatus.BAD_REQUEST,"INVALID_ENDPOINT","Endpoint không hợp lệ")}; if(uri.scheme !in setOf("http","https","ldap","ldaps","smtp","smtps","file")) throw ApiException(HttpStatus.BAD_REQUEST,"UNSUPPORTED_ENDPOINT","Giao thức endpoint chưa được hỗ trợ") }
    private fun notFound() = ApiException(HttpStatus.NOT_FOUND,"ADAPTER_NOT_FOUND","Không tìm thấy adapter")
}
private fun IntegrationAdapterEntity.response(mapper:ObjectMapper)=AdapterResponse(id,code,name,type,endpoint,mapper.readValue(mappingJson,object:TypeReference<Map<String,String>>(){}),secretReference.isNotBlank(),status,lastTestedAt,lastTestResult)

@RestController
@RequestMapping("/api/v1/integrations")
@PreAuthorize("hasAuthority('${Permissions.INTEGRATIONS_MANAGE}')")
class IntegrationController(private val service: IntegrationAdapterService) {
    @GetMapping fun list()=service.list()
    @PostMapping fun create(@Valid @RequestBody input:AdapterRequest)=service.save(null,input)
    @PutMapping("/{id}") fun update(@PathVariable id:UUID,@Valid @RequestBody input:AdapterRequest)=service.save(id,input)
    @PostMapping("/{id}/test") fun test(@PathVariable id:UUID)=service.test(id)
}
