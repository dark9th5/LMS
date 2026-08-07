package com.lmspilot.configuration.api;

import com.lmspilot.configuration.domain.*;

import java.time.Instant;

import java.util.*;
public record ExternalServiceResponse(UUID id,ExternalServiceType serviceType,String configKey,boolean enabled,Map<String,Object>config,boolean secretConfigured,ExternalServiceHealth healthStatus,Instant lastCheckedAt,String lastError,Instant updatedAt){
}
