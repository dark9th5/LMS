package com.lmspilot.audit.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEntryRepository extends JpaRepository<AuditEntryEntity, UUID>, JpaSpecificationExecutor<AuditEntryEntity> {
    boolean existsByEventId(UUID eventId);
}
