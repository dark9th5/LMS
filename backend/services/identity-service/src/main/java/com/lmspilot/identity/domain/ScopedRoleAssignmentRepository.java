package com.lmspilot.identity.domain;

import java.time.Instant;

import java.util.*;

import org.springframework.data.domain.*;

import org.springframework.data.jpa.repository.*;

import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
public interface ScopedRoleAssignmentRepository extends JpaRepository<ScopedRoleAssignmentEntity,UUID> {
    List<ScopedRoleAssignmentEntity> findAllByUserIdOrderByCreatedAtDesc(UUID id);
    List<ScopedRoleAssignmentEntity> findAllByUserId(UUID id);
    long deleteAllByIdIn(Collection<UUID> ids);
}
