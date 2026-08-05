package com.lmspilot.operations.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OperationJobRepository extends JpaRepository<OperationJobEntity, UUID> {
    @Query(value = "SELECT * FROM operation_jobs WHERE status = 'REQUESTED' OR " +
        "(status = 'RUNNING' AND lease_until IS NOT NULL AND lease_until < now()) " +
        "ORDER BY requested_at LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    OperationJobEntity lockNextClaimable();

    List<OperationJobEntity> findAllByOrderByRequestedAtDesc();
}
