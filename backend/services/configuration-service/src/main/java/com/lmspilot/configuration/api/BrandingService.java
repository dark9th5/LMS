package com.lmspilot.configuration.api;

import com.lmspilot.configuration.domain.*;

import com.lmspilot.support.api.ApiException;

import com.lmspilot.support.security.CurrentUser;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.*;
@Service
public class BrandingService{
    private final BrandingProfileRepository repo;
    private final BrandingFileClient files;
    public BrandingService(BrandingProfileRepository r,BrandingFileClient f){
        repo=r;
        files=f;
    }
    @Transactional(readOnly=true)
    public BrandingResponse publicBranding(){
        return response(entity());
    }
    @Transactional
    public BrandingResponse update(BrandingRequest i){
        var e=entity();
        validate(i.logoFileId(),e.getLogoFileId(),"BRANDING_LOGO",Set.of("image/png","image/jpeg"),"logo");
        validate(i.faviconFileId(),e.getFaviconFileId(),"BRANDING_LOGO",Set.of("image/png","image/jpeg"),"favicon");
        validate(i.backgroundFileId(),e.getBackgroundFileId(),"BRANDING_BACKGROUND",Set.of("image/png","image/jpeg","image/webp"),"ảnh nền đăng nhập");
        e.setSystemName(i.systemName().trim());
        e.setIntroduction(nonblank(i.introduction()));
        e.setLogoFileId(i.logoFileId());
        e.setFaviconFileId(i.faviconFileId());
        e.setBackgroundFileId(i.backgroundFileId());
        e.setThemeKey(i.themeKey());
        e.setPrimaryColor(i.primaryColor().toUpperCase());
        e.setSecondaryColor(i.secondaryColor().toUpperCase());
        e.setBackgroundColor(i.backgroundColor().toUpperCase());
        e.setTextColor(i.textColor().toUpperCase());
        e.setCustomDomain(i.customDomain()==null?null:nonblank(i.customDomain().trim().toLowerCase()));
        e.setUpdatedBy(CurrentUser.id());
        e.setUpdatedAt(Instant.now());
        return response(repo.save(e));
    }
    private void validate(UUID requested,UUID current,String purpose,Set<String>types,String label){
        if(requested==null||requested.equals(current))return;
        var m=files.metadata(requested);
        if(!m.ownerId().equals(CurrentUser.id()))throw new ApiException(HttpStatus.FORBIDDEN,"BRANDING_FILE_OWNER_MISMATCH","Tệp "+label+" không thuộc quản trị viên hiện tại");
        if(!"AVAILABLE".equals(m.status())||!purpose.equals(m.purpose())||!types.contains(m.contentType()))throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_BRANDING_FILE","Tệp "+label+" không đúng định dạng hoặc mục đích sử dụng");
    }
    private BrandingProfileEntity entity(){
        var e=repo.findByProfileKey("default");
        return e==null?new BrandingProfileEntity("default"):e;
    }
    private BrandingResponse response(BrandingProfileEntity e){
        return new BrandingResponse(e.getProfileKey(),e.getSystemName(),e.getIntroduction(),e.getLogoFileId(),e.getFaviconFileId(),e.getBackgroundFileId(),e.getLogoFileId()==null?null:"/public/v1/branding/assets/logo",e.getFaviconFileId()==null?null:"/public/v1/branding/assets/favicon",e.getBackgroundFileId()==null?null:"/public/v1/branding/assets/background",e.getThemeKey(),e.getPrimaryColor(),e.getSecondaryColor(),e.getBackgroundColor(),e.getTextColor(),e.getCustomDomain(),e.getUpdatedAt());
    }
    private String nonblank(String s){
        return s==null||s.isBlank()?null:s.trim();
    }

}
