package com.lmspilot.reporting.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
public interface ReportEventRepository extends JpaRepository<ReportEventEntity,UUID>{
    boolean existsByEventId(UUID id);
}
