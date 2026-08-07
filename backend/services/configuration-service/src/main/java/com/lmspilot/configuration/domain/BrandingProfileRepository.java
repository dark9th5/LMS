package com.lmspilot.configuration.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
public interface BrandingProfileRepository extends JpaRepository<BrandingProfileEntity,UUID>{
    BrandingProfileEntity findByProfileKey(String key);
}
