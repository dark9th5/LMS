package com.lmspilot.audit.api;

import com.lmspilot.contracts.DomainEventEnvelope;

import com.lmspilot.support.security.InternalTokenAuthorizer;

import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/internal/v1/audit")
public class InternalAuditController{
    private final AuditService service;
    private final InternalTokenAuthorizer internal;
    public InternalAuditController(AuditService s,InternalTokenAuthorizer i){
        service=s;
        internal=i;
    }
    @PostMapping
    public void record(@RequestHeader(value="X-Service-Token",required=false)String token,@RequestBody DomainEventEnvelope event){
        internal.require(token);
        service.record(event);
    }

}
