package com.lmspilot.organization.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.organization.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.InternalTokenAuthorizer
import com.lmspilot.support.security.ScopedAuthorizationClient
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

data class MembershipInput(
    val userId: UUID,
    val unitId: UUID,
    val membershipType: MembershipType = MembershipType.MEMBER,
    val primaryMembership: Boolean = false,
    val validFrom: Instant? = null,
    val validUntil: Instant? = null,
) {
    @AssertTrue(message = "validUntil phải sau validFrom")
    fun validWindow(): Boolean = validUntil == null || validFrom == null || validUntil.isAfter(validFrom)
}

data class BulkMembershipRequest(
    @field:Valid @field:Size(min = 1, max = 5000)
    val memberships: List<MembershipInput>,
)

data class RevokeMembershipRequest(@field:Size(min = 1, max = 5000) val membershipIds: Set<UUID>)

data class MembershipResponse(
    val id: UUID,
    val userId: UUID,
    val unitId: UUID,
    val membershipType: MembershipType,
    val primaryMembership: Boolean,
    val validFrom: Instant?,
    val validUntil: Instant?,
    val active: Boolean,
    val createdAt: Instant,
)

@Service
class OrganizationMembershipService(
    private val memberships: OrganizationMembershipRepository,
    private val units: OrganizationUnitRepository,
    private val scopedAuthorization: ScopedAuthorizationClient,
) {
    @Transactional(readOnly = true)
    fun byUser(userId: UUID): List<MembershipResponse> = memberships.findAllByUserIdOrderByCreatedAtDesc(userId).map { it.response() }

    @Transactional(readOnly = true)
    fun byUnit(unitId: UUID): List<MembershipResponse> {
        val unit = requireUnit(unitId)
        requireUnitPermission(unit, Permissions.ORGANIZATION_READ)
        return memberships.findAllByUnitIdOrderByCreatedAtDesc(unitId).map { it.response() }
    }

    @Transactional(readOnly = true)
    fun byUserForRequester(userId: UUID): List<MembershipResponse> {
        val rows = memberships.findAllByUserIdOrderByCreatedAtDesc(userId)
        if (isGlobal(Permissions.ORGANIZATION_READ)) return rows.map { it.response() }
        val unitById = units.findAllById(rows.map { it.unitId }.toSet()).associateBy { it.id }
        return rows.filter { row -> unitById[row.unitId]?.let { canAccessUnit(it, Permissions.ORGANIZATION_READ) } == true }
            .map { it.response() }
    }

    @Transactional
    fun grant(input: BulkMembershipRequest): List<MembershipResponse> {
        val unitIds = input.memberships.map { it.unitId }.toSet()
        val foundUnits = units.findAllById(unitIds).associateBy { it.id }
        if (foundUnits.size != unitIds.size) throw ApiException(HttpStatus.BAD_REQUEST, "UNIT_NOT_FOUND", "Một hoặc nhiều đơn vị không tồn tại")
        foundUnits.values.forEach { requireUnitPermission(it, Permissions.ORGANIZATION_MEMBERSHIP_MANAGE) }

        val created = mutableListOf<OrganizationMembershipEntity>()
        for (item in input.memberships) {
            if (memberships.existsByUserIdAndUnitIdAndMembershipType(item.userId, item.unitId, item.membershipType)) continue
            if (item.primaryMembership) {
                memberships.findAllByUserIdOrderByCreatedAtDesc(item.userId)
                    .filter { it.primaryMembership }
                    .forEach { it.primaryMembership = false }
            }
            created += memberships.save(
                OrganizationMembershipEntity(
                    userId = item.userId,
                    unitId = item.unitId,
                    membershipType = item.membershipType,
                    primaryMembership = item.primaryMembership,
                    validFrom = item.validFrom,
                    validUntil = item.validUntil,
                    createdBy = CurrentUser.id(),
                )
            )
        }
        return created.map { it.response() }
    }

    @Transactional
    fun revoke(input: RevokeMembershipRequest): Map<String, Long> {
        val rows = memberships.findAllById(input.membershipIds)
        if (rows.size != input.membershipIds.size) {
            throw ApiException(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "Một hoặc nhiều liên kết thành viên không tồn tại")
        }
        val unitById = units.findAllById(rows.map { it.unitId }.toSet()).associateBy { it.id }
        rows.forEach { row ->
            val unit = unitById[row.unitId] ?: throw ApiException(HttpStatus.NOT_FOUND, "UNIT_NOT_FOUND", "Đơn vị không tồn tại")
            requireUnitPermission(unit, Permissions.ORGANIZATION_MEMBERSHIP_MANAGE)
        }
        return mapOf("deleted" to memberships.deleteAllByIdIn(input.membershipIds))
    }

    @Transactional(readOnly = true)
    fun descendants(unitId: UUID): Set<UUID> {
        val root = requireUnit(unitId)
        val marker = "/${root.id}/"
        return units.findAll().filter { it.id == unitId || it.materializedPath.contains(marker) }.map { it.id }.toSet()
    }

    @Transactional(readOnly = true)
    fun ancestors(unitId: UUID): Set<UUID> {
        val unit = requireUnit(unitId)
        val ids = unit.materializedPath.trim('/').split('/').filter { it.isNotBlank() }.map(UUID::fromString).toMutableSet()
        ids += unit.id
        return ids
    }

    @Transactional(readOnly = true)
    fun usersInScope(unitId: UUID): Set<UUID> {
        val unitIds = descendants(unitId)
        val now = Instant.now()
        return memberships.findAllByUnitIdIn(unitIds)
            .filter { (it.validFrom == null || !it.validFrom!!.isAfter(now)) && (it.validUntil == null || it.validUntil!!.isAfter(now)) }
            .map { it.userId }.toSet()
    }

    private fun requireUnit(unitId: UUID): OrganizationUnitEntity = units.findById(unitId).orElseThrow {
        ApiException(HttpStatus.NOT_FOUND, "UNIT_NOT_FOUND", "Không tìm thấy đơn vị")
    }

    private fun requireUnitPermission(unit: OrganizationUnitEntity, permission: String) {
        if (!canAccessUnit(unit, permission)) {
            throw ApiException(HttpStatus.FORBIDDEN, "ORGANIZATION_MEMBERSHIP_OUT_OF_SCOPE", "Đơn vị ngoài phạm vi quản lý thành viên")
        }
    }

    private fun canAccessUnit(unit: OrganizationUnitEntity, permission: String): Boolean {
        if (isGlobal(permission)) return true
        val candidates = when (unit.type) {
            OrganizationUnitType.BRANCH -> listOf("BRANCH")
            OrganizationUnitType.DEPARTMENT, OrganizationUnitType.DIVISION -> listOf("DEPARTMENT", "BRANCH")
            else -> listOf("GROUP", "DEPARTMENT", "BRANCH")
        }
        return candidates.any { scopedAuthorization.allowed(permission, it, unit.id) } ||
            candidates.any { scopedAuthorization.allowed(Permissions.ORGANIZATION_MANAGE, it, unit.id) }
    }

    private fun isGlobal(permission: String): Boolean = CurrentUser.isSystemAdmin() ||
        permission in CurrentUser.authorities() ||
        Permissions.ORGANIZATION_MANAGE in CurrentUser.authorities() ||
        scopedAuthorization.allowed(permission, "SYSTEM", null) ||
        scopedAuthorization.allowed(Permissions.ORGANIZATION_MANAGE, "SYSTEM", null)
}

private fun OrganizationMembershipEntity.response(): MembershipResponse {
    val now = Instant.now()
    val active = (validFrom == null || !validFrom!!.isAfter(now)) && (validUntil == null || validUntil!!.isAfter(now))
    return MembershipResponse(id, userId, unitId, membershipType, primaryMembership, validFrom, validUntil, active, createdAt)
}

@RestController
@RequestMapping("/api/v1/organization/memberships")
class OrganizationMembershipController(private val service: OrganizationMembershipService) {
    @GetMapping
    @PreAuthorize("hasAuthority('${Permissions.ORGANIZATION_READ}')")
    fun list(@RequestParam(required = false) userId: UUID?, @RequestParam(required = false) unitId: UUID?): List<MembershipResponse> = when {
        userId != null -> service.byUserForRequester(userId)
        unitId != null -> service.byUnit(unitId)
        else -> throw ApiException(HttpStatus.BAD_REQUEST, "FILTER_REQUIRED", "Cần userId hoặc unitId")
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('${Permissions.ORGANIZATION_MEMBERSHIP_MANAGE}','${Permissions.ORGANIZATION_MANAGE}')")
    fun grant(@Valid @RequestBody input: BulkMembershipRequest) = service.grant(input)

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyAuthority('${Permissions.ORGANIZATION_MEMBERSHIP_MANAGE}','${Permissions.ORGANIZATION_MANAGE}')")
    fun revoke(@Valid @RequestBody input: RevokeMembershipRequest) = service.revoke(input)
}

@RestController
@RequestMapping("/internal/v1/organization")
class InternalOrganizationScopeController(
    private val service: OrganizationMembershipService,
    private val internal: InternalTokenAuthorizer,
) {
    @GetMapping("/units/{id}/descendants")
    fun descendants(@PathVariable id: UUID, @RequestHeader("X-Service-Token", required = false) token: String?): Set<UUID> {
        internal.require(token)
        return service.descendants(id)
    }


    @GetMapping("/users/{userId}/unit-ids")
    fun userUnitIds(@PathVariable userId: UUID, @RequestHeader("X-Service-Token", required = false) token: String?): Set<UUID> {
        internal.require(token)
        return service.byUser(userId).filter { it.active }.map { it.unitId }.toSet()
    }

    @GetMapping("/units/{id}/users")
    fun users(@PathVariable id: UUID, @RequestHeader("X-Service-Token", required = false) token: String?): Set<UUID> {
        internal.require(token)
        return service.usersInScope(id)
    }

    @GetMapping("/units/{id}/ancestors")
    fun ancestors(@PathVariable id: UUID, @RequestHeader("X-Service-Token", required = false) token: String?): Set<UUID> {
        internal.require(token)
        return service.ancestors(id)
    }
}
