package com.lmspilot.configuration.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="product_configuration")
public class ProductConfigurationEntity{
    @Id
    private UUID id=new UUID(0,1);
    @Column(nullable=false,length=160)
    private String productName="LMSPilot";
    @Column(nullable=false,length=500)
    private String logoUrl="";
    @Column(nullable=false,length=20)
    private String primaryColor="#1457D9";
    @Column(nullable=false,length=20)
    private String accentColor="#15A37B";
    @Column(nullable=false,length=20)
    private String defaultLocale="vi";
    @Column(nullable=false,columnDefinition="text")
    private String featureFlagsJson="{}";
    @Column(nullable=false,columnDefinition="text")
    private String terminologyJson="{}";
    @Column(nullable=false)
    private Instant updatedAt=Instant.now();
    private UUID updatedBy;
    @Version
    private long rowVersion;
    public ProductConfigurationEntity(){
    }
    public UUID getId(){
        return id;
    }
    public String getProductName(){
        return productName;
    }
    public void setProductName(String v){
        productName=v;
    }
    public String getLogoUrl(){
        return logoUrl;
    }
    public void setLogoUrl(String v){
        logoUrl=v;
    }
    public String getPrimaryColor(){
        return primaryColor;
    }
    public void setPrimaryColor(String v){
        primaryColor=v;
    }
    public String getAccentColor(){
        return accentColor;
    }
    public void setAccentColor(String v){
        accentColor=v;
    }
    public String getDefaultLocale(){
        return defaultLocale;
    }
    public void setDefaultLocale(String v){
        defaultLocale=v;
    }
    public String getFeatureFlagsJson(){
        return featureFlagsJson;
    }
    public void setFeatureFlagsJson(String v){
        featureFlagsJson=v;
    }
    public String getTerminologyJson(){
        return terminologyJson;
    }
    public void setTerminologyJson(String v){
        terminologyJson=v;
    }
    public Instant getUpdatedAt(){
        return updatedAt;
    }
    public void setUpdatedAt(Instant v){
        updatedAt=v;
    }
    public void setUpdatedBy(UUID v){
        updatedBy=v;
    }

}
