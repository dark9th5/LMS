package com.lmspilot.license.api;

import com.lmspilot.license.domain.LicenseStatus;

import java.time.Instant;

import java.util.Set;
public record LicenseEntitlementsResponse(String licenseId,String edition,int maxUsers,Set<String> features,LicenseStatus status,boolean readOnly,Instant expiresAt,Instant graceEndsAt){
}
