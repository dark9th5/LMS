package com.lmspilot.organization.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.organization.domain.*
import com.lmspilot.support.api.ApiException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

data class OrganizationUnitRequest(
    @field:NotBlank @field:Size(max = 80) val code: String,
    @field:NotBlank @field:Size(max = 180) val name: String,
    val type: OrganizationUnitType,
    val parentId: UUID? = null,
    val status: OrganizationUnitStatus = OrganizationUnitStatus.ACTIVE,
    val sortOrder: Int = 0,
)

data class OrganizationUnitResponse(
    val id: UUID, val code: String, val name: String, val type: OrganizationUnitType,
    val parentId: UUID?, val status: OrganizationUnitStatus, val sortOrder: Int,
    val path: String, val children: List<OrganizationUnitResponse> = emptyList(),
)

@Service
class OrganizationService(private val repository: OrganizationUnitRepository) {
    @Transactional(readOnly = true)
    fun search(query: String?, status: OrganizationUnitStatus?) = repository.search(query?.takeIf { it.isNotBlank() }, status).map { it.response() }

    @Transactional(readOnly = true)
    fun tree(): List<OrganizationUnitResponse> {
        val all = repository.findAll().sortedWith(compareBy({ it.sortOrder }, { it.name }))
        val grouped = all.groupBy { it.parentId }
        fun build(parent: UUID?): List<OrganizationUnitResponse> = grouped[parent].orEmpty().map { node -> node.response(build(node.id)) }
        return build(null)
    }

    @Transactional
    fun create(input: OrganizationUnitRequest): OrganizationUnitResponse {
        if (repository.existsByCodeIgnoreCase(input.code.trim())) duplicate()
        val parent = input.parentId?.let { repository.findById(it).orElseThrow { parentNotFound() } }
        val entity = OrganizationUnitEntity(
            code = input.code.trim().uppercase(), name = input.name.trim(), type = input.type,
            parentId = parent?.id, status = input.status, sortOrder = input.sortOrder,
            materializedPath = parent?.let { "${it.materializedPath}${it.id}/" } ?: "/",
        )
        return repository.save(entity).response()
    }

    @Transactional
    fun update(id: UUID, input: OrganizationUnitRequest): OrganizationUnitResponse {
        val entity = repository.findById(id).orElseThrow { notFound() }
        val parent = input.parentId?.let { repository.findById(it).orElseThrow { parentNotFound() } }
        if (parent?.id == entity.id || parent?.materializedPath?.contains("/${entity.id}/") == true) {
            throw ApiException(HttpStatus.CONFLICT, "ORGANIZATION_CYCLE", "Không thể chuyển đơn vị vào chính nó hoặc đơn vị con")
        }
        entity.name = input.name.trim()
        entity.type = input.type
        entity.parentId = parent?.id
        entity.status = input.status
        entity.sortOrder = input.sortOrder
        entity.materializedPath = parent?.let { "${it.materializedPath}${it.id}/" } ?: "/"
        entity.updatedAt = Instant.now()
        return entity.response()
    }

    @Transactional
    fun deactivate(id: UUID): OrganizationUnitResponse {
        val entity = repository.findById(id).orElseThrow { notFound() }
        entity.status = OrganizationUnitStatus.INACTIVE
        entity.updatedAt = Instant.now()
        return entity.response()
    }

    private fun duplicate(): Nothing = throw ApiException(HttpStatus.CONFLICT, "DUPLICATE_ORGANIZATION_CODE", "Mã đơn vị đã tồn tại")
    private fun notFound() = ApiException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", "Không tìm thấy đơn vị")
    private fun parentNotFound() = ApiException(HttpStatus.BAD_REQUEST, "PARENT_NOT_FOUND", "Đơn vị cha không tồn tại")
}

private fun OrganizationUnitEntity.response(children: List<OrganizationUnitResponse> = emptyList()) =
    OrganizationUnitResponse(id, code, name, type, parentId, status, sortOrder, materializedPath, children)

@RestController
@RequestMapping("/api/v1/organization/units")
class OrganizationController(private val service: OrganizationService) {
    @GetMapping
    @PreAuthorize("hasAuthority('${Permissions.ORGANIZATION_READ}')")
    fun search(@RequestParam(required = false) query: String?, @RequestParam(required = false) status: OrganizationUnitStatus?) = service.search(query, status)

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('${Permissions.ORGANIZATION_READ}')")
    fun tree() = service.tree()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.ORGANIZATION_WRITE}')")
    fun create(@Valid @RequestBody input: OrganizationUnitRequest) = service.create(input)

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('${Permissions.ORGANIZATION_WRITE}')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody input: OrganizationUnitRequest) = service.update(id, input)

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('${Permissions.ORGANIZATION_WRITE}')")
    fun deactivate(@PathVariable id: UUID) = service.deactivate(id)
}
