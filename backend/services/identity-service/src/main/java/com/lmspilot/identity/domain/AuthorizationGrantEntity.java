package com.lmspilot.identity.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.*;
@Entity
@Table(name="authorization_grants",indexes={
    @Index(name="idx_auth_grant_principal",columnList="principal_type,principal_id"),@Index(name="idx_auth_grant_scope",columnList="scope_type,scope_id")
}
) public class AuthorizationGrantEntity {
    @Id
    public UUID id=UUID.randomUUID();
    @Enumerated(EnumType.STRING)
    @Column(name="principal_type",nullable=false,length=20)
    public PrincipalType principalType=PrincipalType.USER;
    @Column(name="principal_id",nullable=false)
    public UUID principalId;
    @Column(name="permission_code",nullable=false,length=120)
    public String permissionCode="";
    @Enumerated(EnumType.STRING)
    @Column(name="scope_type",nullable=false,length=30)
    public ScopeType scopeType=ScopeType.SYSTEM;
    @Column(name="scope_id")
    public UUID scopeId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=10)
    public GrantEffect effect=GrantEffect.ALLOW;
    public Instant validFrom;
    public Instant validUntil;
    @Column(nullable=false)
    public UUID createdBy;
    @Column(nullable=false)
    public Instant createdAt=Instant.now();
    public AuthorizationGrantEntity(){
    }

}
