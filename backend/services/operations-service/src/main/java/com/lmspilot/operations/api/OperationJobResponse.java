package com.lmspilot.operations.api;

import com.lmspilot.operations.domain.*;

import java.time.Instant;

import java.util.UUID;
public record OperationJobResponse(UUID id,OperationType type,OperationStatus status,UUID requestedBy,Instant requestedAt,Instant startedAt,Instant finishedAt,String resultJson,String errorMessage,String claimedBy,Instant heartbeatAt,int attemptCount){
}
