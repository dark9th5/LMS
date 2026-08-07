package com.lmspilot.configuration.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="external_service_configs",uniqueConstraints=@UniqueConstraint(columnNames={
    "service_type","config_key"
}
))public class ExternalServiceConfigEntity{
    @Id
    private UUID id=UUID.randomUUID();
    @Enumerated(EnumType.STRING)
    @Column(name="service_type",nullable=false,length=40)
    private ExternalServiceType serviceType=ExternalServiceType.REDIS;
    @Column(name="config_key",nullable=false,length=80)
    private String configKey="default";
    @Column(nullable=false)
    private boolean enabled;
    @Column(name="config_json",nullable=false,columnDefinition="text")
    private String configJson="{}";
    @Column(name="encrypted_secret")
    private byte[] encryptedSecret;
    @Column(name="secret_key_version")
    private Integer secretKeyVersion;
    @Enumerated(EnumType.STRING)
    @Column(name="health_status",nullable=false,length=20)
    private ExternalServiceHealth healthStatus=ExternalServiceHealth.UNKNOWN;
    @Column(name="last_checked_at")
    private Instant lastCheckedAt;
    @Column(name="last_error",length=1000)
    private String lastError;
    @Column(name="updated_by",nullable=false)
    private UUID updatedBy=new UUID(0,1);
    @Column(name="updated_at",nullable=false)
    private Instant updatedAt=Instant.now();
    @Version
    private long version;
    public ExternalServiceConfigEntity(){
    }
    public ExternalServiceConfigEntity(ExternalServiceType t,String k){
        serviceType=t;
        configKey=k;
    }
    public UUID getId(){
        return id;
    }
    public ExternalServiceType getServiceType(){
        return serviceType;
    }
    public void setServiceType(ExternalServiceType v){
        serviceType=v;
    }
    public String getConfigKey(){
        return configKey;
    }
    public void setConfigKey(String v){
        configKey=v;
    }
    public boolean isEnabled(){
        return enabled;
    }
    public void setEnabled(boolean v){
        enabled=v;
    }
    public String getConfigJson(){
        return configJson;
    }
    public void setConfigJson(String v){
        configJson=v;
    }
    public byte[] getEncryptedSecret(){
        return encryptedSecret;
    }
    public void setEncryptedSecret(byte[] v){
        encryptedSecret=v;
    }
    public Integer getSecretKeyVersion(){
        return secretKeyVersion;
    }
    public void setSecretKeyVersion(Integer v){
        secretKeyVersion=v;
    }
    public ExternalServiceHealth getHealthStatus(){
        return healthStatus;
    }
    public void setHealthStatus(ExternalServiceHealth v){
        healthStatus=v;
    }
    public Instant getLastCheckedAt(){
        return lastCheckedAt;
    }
    public void setLastCheckedAt(Instant v){
        lastCheckedAt=v;
    }
    public String getLastError(){
        return lastError;
    }
    public void setLastError(String v){
        lastError=v;
    }
    public void setUpdatedBy(UUID v){
        updatedBy=v;
    }
    public Instant getUpdatedAt(){
        return updatedAt;
    }
    public void setUpdatedAt(Instant v){
        updatedAt=v;
    }

}
