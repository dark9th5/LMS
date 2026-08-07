package com.lmspilot.configuration.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;
public interface ExternalServiceConfigRepository extends JpaRepository<ExternalServiceConfigEntity,UUID>{
    ExternalServiceConfigEntity findByServiceTypeAndConfigKey(ExternalServiceType t,String k);
    List<ExternalServiceConfigEntity>findAllByOrderByServiceTypeAscConfigKeyAsc();
}
