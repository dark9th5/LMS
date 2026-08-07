package com.lmspilot.ai.platform;

import jakarta.persistence.*;

import java.time.*;

import java.util.*;
@Entity
@Table(name="ai_provider_configs")
public class AiProviderConfigEntity{
    @Id
    public UUID id=UUID.randomUUID();
    @Column(nullable=false,unique=true,length=80)
    public String code="";
    @Enumerated(EnumType.STRING)
    @Column(name="provider_type",nullable=false,length=40)
    public AiProviderType providerType=AiProviderType.LOCAL_OPENAI_COMPATIBLE;
    @Column(name="base_url",nullable=false,length=1000)
    public String baseUrl="";
    @Column(nullable=false,length=240)
    public String model="";
    @Column(nullable=false)
    public boolean enabled;
    @Column(name="encrypted_api_key")
    public byte[] encryptedApiKey;
    @Column(name="secret_key_version")
    public Integer secretKeyVersion;
    @Column(name="request_timeout_seconds",nullable=false)
    public int requestTimeoutSeconds=120;
    @Column(name="max_output_tokens")
    public Integer maxOutputTokens;
    @Column(name="config_json",nullable=false,columnDefinition="text")
    public String configJson="{}";
    @Column(name="updated_by",nullable=false)
    public UUID updatedBy;
    @Column(name="updated_at",nullable=false)
    public Instant updatedAt=Instant.now();
    public AiProviderConfigEntity(){
    }

}
