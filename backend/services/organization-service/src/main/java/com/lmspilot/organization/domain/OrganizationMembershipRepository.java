package com.lmspilot.organization.domain;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembershipEntity,UUID>{
 List<OrganizationMembershipEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId); List<OrganizationMembershipEntity> findAllByUnitIdOrderByCreatedAtDesc(UUID unitId); List<OrganizationMembershipEntity> findAllByUnitIdIn(Collection<UUID> unitIds); boolean existsByUserIdAndUnitIdAndMembershipType(UUID userId,UUID unitId,MembershipType membershipType); long deleteAllByIdIn(Collection<UUID> ids);
}
