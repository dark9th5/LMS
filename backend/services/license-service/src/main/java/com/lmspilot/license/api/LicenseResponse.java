package com.lmspilot.license.api;

import com.lmspilot.license.domain.LicenseStatus;

import java.time.Instant;

import java.util.*;
public record LicenseResponse(UUID id,String licenseId,String organization,String edition,int maxUsers,Set<String> features,Instant issuedAt,Instant expiresAt,int gracePeriodDays,Instant graceEndsAt,LicenseStatus status,boolean readOnly,Instant activatedAt){
    public LicenseResponse{
        features=features==null?Set.of():Set.copyOf(features);
    }

}
