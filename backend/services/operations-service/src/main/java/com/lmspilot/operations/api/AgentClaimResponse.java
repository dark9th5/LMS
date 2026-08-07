package com.lmspilot.operations.api;

import com.lmspilot.operations.domain.OperationType;

import java.time.Instant;

import java.util.*;
public record AgentClaimResponse(UUID id,OperationType type,Map<String,String> parameters,String claimToken,Instant leaseUntil,int attempt){
}
