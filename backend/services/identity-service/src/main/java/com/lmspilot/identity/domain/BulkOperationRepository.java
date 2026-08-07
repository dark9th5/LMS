package com.lmspilot.identity.domain;

import java.time.Instant;

import java.util.*;

import org.springframework.data.domain.*;

import org.springframework.data.jpa.repository.*;

import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
public interface BulkOperationRepository extends JpaRepository<BulkOperationEntity,String> {
}
