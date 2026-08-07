package com.lmspilot.organization.api;

import com.lmspilot.organization.domain.MembershipType;

import jakarta.validation.constraints.AssertTrue;

import java.time.Instant;

import java.util.UUID;
public record MembershipInput(UUID userId,UUID unitId,MembershipType membershipType,boolean primaryMembership,Instant validFrom,Instant validUntil){
    public MembershipInput{
        if(membershipType==null)membershipType=MembershipType.MEMBER;
    }
    @AssertTrue(message="validUntil phải sau validFrom")
    public boolean validWindow(){
        return validUntil==null||validFrom==null||validUntil.isAfter(validFrom);
    }

}
