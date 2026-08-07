package com.lmspilot.configuration.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="branding_profiles")
public class BrandingProfileEntity{
    @Id
    private UUID id=UUID.randomUUID();
    @Column(name="profile_key",nullable=false,unique=true,length=80)
    private String profileKey="default";
    @Column(name="system_name",nullable=false,length=240)
    private String systemName="LMSPilot";
    @Column(columnDefinition="text")
    private String introduction;
    @Column(name="logo_file_id")
    private UUID logoFileId;
    @Column(name="favicon_file_id")
    private UUID faviconFileId;
    @Column(name="background_file_id")
    private UUID backgroundFileId;
    @Column(name="theme_key",nullable=false,length=64)
    private String themeKey="unified-light";
    @Column(name="primary_color",nullable=false,length=9)
    private String primaryColor="#2563EB";
    @Column(name="secondary_color",nullable=false,length=9)
    private String secondaryColor="#475569";
    @Column(name="background_color",nullable=false,length=9)
    private String backgroundColor="#F6F7F9";
    @Column(name="text_color",nullable=false,length=9)
    private String textColor="#172033";
    @Column(name="custom_domain",length=253)
    private String customDomain;
    @Column(name="updated_by",nullable=false)
    private UUID updatedBy=new UUID(0,1);
    @Column(name="updated_at",nullable=false)
    private Instant updatedAt=Instant.now();
    @Version
    private long version;
    public BrandingProfileEntity(){
    }
    public BrandingProfileEntity(String key){
        profileKey=key;
    }
    public UUID getId(){
        return id;
    }
    public String getProfileKey(){
        return profileKey;
    }
    public String getSystemName(){
        return systemName;
    }
    public void setSystemName(String v){
        systemName=v;
    }
    public String getIntroduction(){
        return introduction;
    }
    public void setIntroduction(String v){
        introduction=v;
    }
    public UUID getLogoFileId(){
        return logoFileId;
    }
    public void setLogoFileId(UUID v){
        logoFileId=v;
    }
    public UUID getFaviconFileId(){
        return faviconFileId;
    }
    public void setFaviconFileId(UUID v){
        faviconFileId=v;
    }
    public UUID getBackgroundFileId(){
        return backgroundFileId;
    }
    public void setBackgroundFileId(UUID v){
        backgroundFileId=v;
    }
    public String getThemeKey(){
        return themeKey;
    }
    public void setThemeKey(String v){
        themeKey=v;
    }
    public String getPrimaryColor(){
        return primaryColor;
    }
    public void setPrimaryColor(String v){
        primaryColor=v;
    }
    public String getSecondaryColor(){
        return secondaryColor;
    }
    public void setSecondaryColor(String v){
        secondaryColor=v;
    }
    public String getBackgroundColor(){
        return backgroundColor;
    }
    public void setBackgroundColor(String v){
        backgroundColor=v;
    }
    public String getTextColor(){
        return textColor;
    }
    public void setTextColor(String v){
        textColor=v;
    }
    public String getCustomDomain(){
        return customDomain;
    }
    public void setCustomDomain(String v){
        customDomain=v;
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
