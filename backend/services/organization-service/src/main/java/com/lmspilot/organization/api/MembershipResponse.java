package com.lmspilot.organization.api;

import com.lmspilot.organization.domain.MembershipType;

import java.time.Instant;

import java.util.UUID;
public record MembershipResponse(UUID id,UUID userId,UUID unitId,MembershipType membershipType,boolean primaryMembership,Instant validFrom,Instant validUntil,boolean active,Instant createdAt){
}
