package com.lmspilot.ai.platform;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface AiProviderConfigRepository extends JpaRepository<AiProviderConfigEntity,UUID>{
    Optional<AiProviderConfigEntity> findByCodeIgnoreCase(String code);
    List<AiProviderConfigEntity> findAllByOrderByCodeAsc();
}
