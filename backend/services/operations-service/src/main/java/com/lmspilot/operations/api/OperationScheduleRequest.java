package com.lmspilot.operations.api;

import com.lmspilot.operations.domain.*;

import jakarta.validation.constraints.*;

import java.util.Map;
public record OperationScheduleRequest(@NotBlank
@Size(max=180)String name,OperationType operationType,OperationScheduleFrequency frequency,@Min(1)
@Max(7)Integer dayOfWeek,@Min(0)
@Max(23)int hourUtc,Map<String,String> parameters,boolean enabled){
    public OperationScheduleRequest{
        if(operationType==null)operationType=OperationType.BACKUP;
        if(frequency==null)frequency=OperationScheduleFrequency.DAILY;
        parameters=parameters==null?Map.of():Map.copyOf(parameters);
    }

}
