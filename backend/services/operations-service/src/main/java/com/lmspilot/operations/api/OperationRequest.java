package com.lmspilot.operations.api;

import java.util.Map;
public record OperationRequest(Map<String,String> parameters){
    public OperationRequest{
        parameters=parameters==null?Map.of():Map.copyOf(parameters);
    }

}
