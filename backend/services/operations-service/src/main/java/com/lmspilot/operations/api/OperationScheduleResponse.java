package com.lmspilot.operations.api;

import com.lmspilot.operations.domain.*;

import java.time.Instant;

import java.util.*;
public record OperationScheduleResponse(UUID id,String name,OperationType operationType,OperationScheduleFrequency frequency,Integer dayOfWeek,int hourUtc,Map<String,String> parameters,boolean enabled,Instant nextRunAt,UUID createdBy,Instant updatedAt){
}
