package com.lmspilot.organization.domain;
import java.util.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnitEntity,UUID>{
 boolean existsByCodeIgnoreCase(String code); List<OrganizationUnitEntity> findAllByParentIdOrderBySortOrderAscNameAsc(UUID parentId); long countByParentId(UUID parentId);
 @Query("select o from OrganizationUnitEntity o where (:query is null or lower(o.code) like lower(concat('%', cast(:query as string), '%')) or lower(o.name) like lower(concat('%', cast(:query as string), '%'))) and (:status is null or o.status = :status) order by o.materializedPath, o.sortOrder, o.name")
 List<OrganizationUnitEntity> search(@Param("query")String query,@Param("status")OrganizationUnitStatus status);
}
