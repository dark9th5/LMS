package com.lmspilot.organization.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.organization.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.InternalTokenAuthorizer
import com.lmspilot.support.security.ScopedAuthorizationClient
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
class OrganizationService(
    private val repository: OrganizationUnitRepository,
    private val scopedAuthorization: ScopedAuthorizationClient,
) {
    @Transactional(readOnly = true)
    fun search(query: String?, status: OrganizationUnitStatus?): List<OrganizationUnitResponse> {
        val visible = visibleUnitIds(Permissions.ORGANIZATION_READ)
        return repository.search(query?.takeIf { it.isNotBlank() }, status)
            .filter { visible == null || it.id in visible }
            .map { it.response() }
    }

    @Transactional(readOnly = true)
    fun tree(): List<OrganizationUnitResponse> {
        val visible = visibleUnitIds(Permissions.ORGANIZATION_READ)
        val all = repository.findAll()
            .filter { visible == null || it.id in visible }
            .sortedWith(compareBy({ it.sortOrder }, { it.name }))
        val grouped = all.groupBy { it.parentId }
        fun build(parent: UUID?): List<OrganizationUnitResponse> = grouped[parent].orEmpty().map { node -> node.response(build(node.id)) }
        return build(null)
    }

    @Transactional
    fun create(input: OrganizationUnitRequest): OrganizationUnitResponse {
        if (repository.existsByCodeIgnoreCase(input.code.trim())) duplicate()
        val parent = input.parentId?.let { repository.findById(it).orElseThrow { parentNotFound() } }
        if (parent == null) requireGlobal(Permissions.ORGANIZATION_MANAGE)
        else requireUnitPermission(parent, Permissions.ORGANIZATION_MANAGE)
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
        requireUnitPermission(entity, Permissions.ORGANIZATION_MANAGE)
        val parent = input.parentId?.let { repository.findById(it).orElseThrow { parentNotFound() } }
        parent?.let { requireUnitPermission(it, Permissions.ORGANIZATION_MANAGE) }
        if (parent?.id == entity.id || parent?.materializedPath?.contains("/${entity.id}/") == true) {
            throw ApiException(HttpStatus.CONFLICT, "ORGANIZATION_CYCLE", "Không thể chuyển đơn vị vào chính nó hoặc đơn vị con")
        }
        val oldPrefix = "${entity.materializedPath}${entity.id}/"
        val newPath = parent?.let { "${it.materializedPath}${it.id}/" } ?: "/"
        entity.name = input.name.trim()
        entity.type = input.type
        entity.parentId = parent?.id
        entity.status = input.status
        entity.sortOrder = input.sortOrder
        entity.materializedPath = newPath
        entity.updatedAt = Instant.now()
        val newPrefix = "$newPath${entity.id}/"
        repository.findAll().filter { it.id != entity.id && it.materializedPath.startsWith(oldPrefix) }.forEach { descendant ->
            descendant.materializedPath = newPrefix + descendant.materializedPath.removePrefix(oldPrefix)
            descendant.updatedAt = Instant.now()
        }
        return entity.response()
    }

    @Transactional
    fun deactivate(id: UUID): OrganizationUnitResponse {
        val entity = repository.findById(id).orElseThrow { notFound() }
        requireUnitPermission(entity, Permissions.ORGANIZATION_MANAGE)
        entity.status = OrganizationUnitStatus.INACTIVE
        entity.updatedAt = Instant.now()
        return entity.response()
    }

    private fun visibleUnitIds(permission: String): Set<UUID>? {
        if (isGlobal(permission)) return null
        val roots = scopedAuthorization.scopeIds(permission, "BRANCH") +
            scopedAuthorization.scopeIds(permission, "DEPARTMENT") +
            scopedAuthorization.scopeIds(permission, "GROUP") +
            scopedAuthorization.scopeIds(Permissions.ORGANIZATION_MANAGE, "BRANCH") +
            scopedAuthorization.scopeIds(Permissions.ORGANIZATION_MANAGE, "DEPARTMENT") +
            scopedAuthorization.scopeIds(Permissions.ORGANIZATION_MANAGE, "GROUP")
        if (roots.isEmpty()) return emptySet()
        val all = repository.findAll()
        return all.filter { unit -> roots.any { root -> unit.id == root || unit.materializedPath.contains("/$root/") } }
            .map { it.id }.toSet()
    }

    private fun requireGlobal(permission: String) {
        if (!isGlobal(permission)) throw ApiException(HttpStatus.FORBIDDEN, "ORGANIZATION_OUT_OF_SCOPE", "Cần quyền toàn hệ thống cho thao tác này")
    }

    private fun requireUnitPermission(unit: OrganizationUnitEntity, permission: String) {
        if (isGlobal(permission)) return
        val candidates = when (unit.type) {
            OrganizationUnitType.BRANCH -> listOf("BRANCH")
            OrganizationUnitType.DEPARTMENT, OrganizationUnitType.DIVISION -> listOf("DEPARTMENT", "BRANCH")
            else -> listOf("GROUP", "DEPARTMENT", "BRANCH")
        }
        val allowed = candidates.any { scopedAuthorization.allowed(permission, it, unit.id) } ||
            candidates.any { scopedAuthorization.allowed(Permissions.ORGANIZATION_WRITE, it, unit.id) }
        if (!allowed) throw ApiException(HttpStatus.FORBIDDEN, "ORGANIZATION_OUT_OF_SCOPE", "Đơn vị ngoài phạm vi được cấp")
    }

    private fun isGlobal(permission: String): Boolean = CurrentUser.isSystemAdmin() || "ADMIN" in CurrentUser.roles() ||
        scopedAuthorization.allowed(permission, "SYSTEM", null) ||
        scopedAuthorization.allowed(Permissions.ORGANIZATION_WRITE, "SYSTEM", null)

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
    @PreAuthorize("hasAnyAuthority('${Permissions.ORGANIZATION_MANAGE}','${Permissions.ORGANIZATION_WRITE}')")
    fun create(@Valid @RequestBody input: OrganizationUnitRequest) = service.create(input)

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('${Permissions.ORGANIZATION_MANAGE}','${Permissions.ORGANIZATION_WRITE}')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody input: OrganizationUnitRequest) = service.update(id, input)

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('${Permissions.ORGANIZATION_MANAGE}','${Permissions.ORGANIZATION_WRITE}')")
    fun deactivate(@PathVariable id: UUID) = service.deactivate(id)
}

data class ValidateOrganizationUnitsRequest(val ids: Set<UUID>)

@RestController
@RequestMapping("/internal/v1/organization/units")
class InternalOrganizationUnitController(
    private val repository: OrganizationUnitRepository,
    private val internal: InternalTokenAuthorizer,
) {
    @PostMapping("/validate-active")
    fun validateActive(
        @RequestBody input: ValidateOrganizationUnitsRequest,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ): Set<UUID> {
        internal.require(token)
        if (input.ids.isEmpty()) return emptySet()
        return repository.findAllById(input.ids)
            .filter { it.status == OrganizationUnitStatus.ACTIVE }
            .map { it.id }
            .toSet()
    }
}
