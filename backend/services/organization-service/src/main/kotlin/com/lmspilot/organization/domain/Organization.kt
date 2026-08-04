package com.lmspilot.organization.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class OrganizationUnitType { ORGANIZATION, BRANCH, DIVISION, DEPARTMENT, TEAM, CLASS, GROUP }
enum class OrganizationUnitStatus { ACTIVE, INACTIVE }

@Entity
@Table(name = "organization_units", indexes = [Index(name = "idx_org_parent", columnList = "parent_id")])
class OrganizationUnitEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 80) var code: String = "",
    @Column(nullable = false, length = 180) var name: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) var type: OrganizationUnitType = OrganizationUnitType.DEPARTMENT,
    @Column(name = "parent_id") var parentId: UUID? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: OrganizationUnitStatus = OrganizationUnitStatus.ACTIVE,
    @Column(nullable = false) var sortOrder: Int = 0,
    @Column(nullable = false, length = 1200) var materializedPath: String = "/",
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

interface OrganizationUnitRepository : org.springframework.data.jpa.repository.JpaRepository<OrganizationUnitEntity, UUID> {
    fun existsByCodeIgnoreCase(code: String): Boolean
    fun findAllByParentIdOrderBySortOrderAscNameAsc(parentId: UUID?): List<OrganizationUnitEntity>
    fun countByParentId(parentId: UUID): Long

    @org.springframework.data.jpa.repository.Query("""
        select o from OrganizationUnitEntity o
        where (:query is null or lower(o.code) like lower(concat('%', cast(:query as string), '%')) or lower(o.name) like lower(concat('%', cast(:query as string), '%')))
          and (:status is null or o.status = :status)
        order by o.materializedPath, o.sortOrder, o.name
    """)
    fun search(query: String?, status: OrganizationUnitStatus?): List<OrganizationUnitEntity>
}
