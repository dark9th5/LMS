package com.lmspilot.identity.config;

import com.lmspilot.contracts.*;

import com.lmspilot.identity.domain.*;

import java.time.Instant;

import java.util.LinkedHashSet;

import org.springframework.boot.*;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
@Component
public class SystemAccessProfileBootstrap implements ApplicationRunner {
    private final RoleRepository roles;
    public SystemAccessProfileBootstrap(RoleRepository roles){
        this.roles=roles;
    }
    @Override
    @Transactional
    public void run(ApplicationArguments args){
        for(AccessProfileDefinition p:DefaultAccessProfiles.PROFILES){
            RoleEntity r=roles.findByCodeIgnoreCase(p.code()).orElseGet(RoleEntity::new);
            r.code=p.code();
            r.name=p.name();
            r.permissions=new LinkedHashSet<>(p.permissions());
            r.systemRole=true;
            r.updatedAt=Instant.now();
            roles.save(r);
        }

    }

}
