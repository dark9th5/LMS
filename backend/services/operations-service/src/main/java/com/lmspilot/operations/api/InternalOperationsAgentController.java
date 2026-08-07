package com.lmspilot.operations.api;

import com.lmspilot.support.security.InternalTokenAuthorizer;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import java.util.*;
@RestController
@RequestMapping("/internal/v1/operations/jobs")
public class InternalOperationsAgentController{
    private final OperationsService s;
    private final InternalTokenAuthorizer internal;
    public InternalOperationsAgentController(OperationsService s,InternalTokenAuthorizer i){
        this.s=s;
        internal=i;
    }
    @PostMapping("/claim")
    public ResponseEntity<AgentClaimResponse>claim(@Valid
    @RequestBody AgentClaimRequest i,@RequestHeader(value="X-Service-Token",required=false)String token){
        internal.require(token);
        var j=s.claim(i.agentId());
        return j==null?ResponseEntity.noContent().build():ResponseEntity.ok(j);
    }
    @PostMapping("/{id}/heartbeat")
    public Map<String,Instant>heartbeat(@PathVariable UUID id,@Valid
    @RequestBody AgentHeartbeatRequest i,@RequestHeader(value="X-Service-Token",required=false)String token){
        internal.require(token);
        return Map.of("leaseUntil",s.heartbeat(id,i.claimToken()));
    }
    @PostMapping("/{id}/complete")
    public OperationJobResponse complete(@PathVariable UUID id,@Valid
    @RequestBody AgentCompleteRequest i,@RequestHeader(value="X-Service-Token",required=false)String token){
        internal.require(token);
        return s.complete(id,i);
    }

}
