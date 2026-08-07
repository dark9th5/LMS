package com.lmspilot.integration.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
public interface IntegrationAdapterRepository extends JpaRepository<IntegrationAdapterEntity,UUID>{
    IntegrationAdapterEntity findByCode(String code);
}
