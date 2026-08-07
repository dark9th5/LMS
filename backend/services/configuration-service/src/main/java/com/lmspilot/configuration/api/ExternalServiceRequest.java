package com.lmspilot.configuration.api;

import com.lmspilot.configuration.domain.ExternalServiceType;

import jakarta.validation.constraints.*;

import java.util.Map;
public record ExternalServiceRequest(ExternalServiceType serviceType,@NotBlank
@Size(max=80)String configKey,boolean enabled,Map<String,Object>config,String secret){
    public ExternalServiceRequest{
        if(configKey==null)configKey="default";
        config=config==null?Map.of():Map.copyOf(config);
    }

}
