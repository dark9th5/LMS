package com.lmspilot.organization.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class MembershipType { MEMBER, MANAGER, INSTRUCTOR, LEARNER }

@Entity
@Table(
    name = "organization_memberships_v2",
    indexes = [
        Index(name = "idx_org_membership_user", columnList = "user_id"),
        Index(name = "idx_org_membership_unit", columnList = "unit_id"),
    ],
)
class OrganizationMembershipEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(name = "unit_id", nullable = false) var unitId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false, length = 30)
    var membershipType: MembershipType = MembershipType.MEMBER,
    @Column(name = "primary_membership", nullable = false)
    var primaryMembership: Boolean = false,
    @Column(name = "valid_from") var validFrom: Instant? = null,
    @Column(name = "valid_until") var validUntil: Instant? = null,
    @Column(name = "created_by", nullable = false) var createdBy: UUID = UUID.randomUUID(),
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
)

interface OrganizationMembershipRepository : org.springframework.data.jpa.repository.JpaRepository<OrganizationMembershipEntity, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<OrganizationMembershipEntity>
    fun findAllByUnitIdOrderByCreatedAtDesc(unitId: UUID): List<OrganizationMembershipEntity>
    fun findAllByUnitIdIn(unitIds: Collection<UUID>): List<OrganizationMembershipEntity>
    fun existsByUserIdAndUnitIdAndMembershipType(userId: UUID, unitId: UUID, membershipType: MembershipType): Boolean
    fun deleteAllByIdIn(ids: Collection<UUID>): Long
}
