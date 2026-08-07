package com.lmspilot.license.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.lmspilot.license.domain.*;

import com.lmspilot.support.api.ApiException;

import java.security.*;

import java.security.spec.X509EncodedKeySpec;

import java.time.*;

import java.util.*;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
@Service
public class LicenseManagementService{
    private static final long MAX_CLOCK_SKEW_SECONDS=86400;
    private final LicenseRepository repo;
    private final ObjectMapper mapper;
    private final String publicKeyBase64;
    private final boolean allowDevelopment;
    private final String machineFingerprint;
    public LicenseManagementService(LicenseRepository r,ObjectMapper m,@Value("${license.public-key:}")String k,@Value("${license.allow-development:false}")boolean d,@Value("${license.machine-fingerprint:development}")String f){
        repo=r;
        mapper=m;
        publicKeyBase64=k;
        allowDevelopment=d;
        machineFingerprint=f;
    }
    @Transactional(readOnly=true)
    public LicenseResponse current(){
        LicenseEntity e=repo.findTopByOrderByActivatedAtDesc();
        if(e!=null)return response(e,Instant.now());
        return allowDevelopment?developmentLicense():missingLicense();
    }
    @Transactional(readOnly=true)
    public LicenseEntitlementsResponse entitlements(){
        var c=current();
        return new LicenseEntitlementsResponse(c.licenseId(),c.edition(),c.maxUsers(),c.features(),c.status(),c.readOnly(),c.expiresAt(),c.graceEndsAt());
    }
    @Transactional
    public LicenseResponse activate(ActivateLicenseRequest in){
        byte[] payload,signature;
        try{
            payload=Base64.getUrlDecoder().decode(in.payload());
        }
        catch(Exception e){
            throw invalid("Payload license không hợp lệ");
        }
        try{
            signature=Base64.getUrlDecoder().decode(in.signature());
        }
        catch(Exception e){
            throw invalid("Chữ ký license không hợp lệ");
        }
        if(!verify(payload,signature))throw invalid("Không xác minh được chữ ký license");
        LicensePayload p;
        try{
            p=mapper.readValue(payload,LicensePayload.class);
        }
        catch(Exception e){
            throw invalid("Nội dung license không hợp lệ");
        }
        validatePayload(p);
        LicenseEntity e=repo.findByLicenseId(p.licenseId());
        if(e==null)e=new LicenseEntity(p.licenseId());
        e.setOrganization(p.organization().trim());
        e.setEdition(p.edition().trim().toUpperCase());
        e.setMaxUsers(p.maxUsers());
        try{
            e.setFeaturesJson(mapper.writeValueAsString(p.features().stream().map(String::toUpperCase).collect(Collectors.toCollection(TreeSet::new))));
        }
        catch(Exception x){
            throw new IllegalStateException(x);
        }
        e.setIssuedAt(p.issuedAt());
        e.setExpiresAt(p.expiresAt());
        e.setGracePeriodDays(p.gracePeriodDays());
        e.setStatus(LicenseStatus.ACTIVE);
        e.setSourcePayload(in.payload());
        e.setActivatedAt(Instant.now());
        return response(repo.save(e),Instant.now());
    }
    private void validatePayload(LicensePayload p){
        if(p.licenseId()==null||p.licenseId().isBlank()||p.licenseId().length()>100)throw invalid("Mã license không hợp lệ");
        if(p.organization()==null||p.organization().isBlank()||p.organization().length()>220)throw invalid("Tên tổ chức không hợp lệ");
        if(p.edition()==null||p.edition().isBlank()||p.edition().length()>80)throw invalid("Gói license không hợp lệ");
        if(p.maxUsers()<1)throw invalid("Giới hạn người dùng phải lớn hơn 0");
        if(p.gracePeriodDays()<0||p.gracePeriodDays()>3650)throw invalid("Grace period không hợp lệ");
        if(p.issuedAt().isAfter(Instant.now().plusSeconds(MAX_CLOCK_SKEW_SECONDS)))throw invalid("Ngày phát hành license nằm trong tương lai");
        if(p.expiresAt()!=null&&!p.expiresAt().isAfter(p.issuedAt()))throw invalid("Ngày hết hạn phải sau ngày phát hành");
        if(p.machineFingerprint()!=null&&!p.machineFingerprint().equals(machineFingerprint))throw invalid("License không thuộc máy chủ này");
        Instant grace=p.expiresAt()==null?null:p.expiresAt().plus(Duration.ofDays(p.gracePeriodDays()));
        if(grace!=null&&grace.isBefore(Instant.now()))throw new ApiException(HttpStatus.CONFLICT,"LICENSE_EXPIRED","License và thời gian gia hạn đã hết");
    }
    private boolean verify(byte[] payload,byte[] signature){
        if(publicKeyBase64.isBlank()){
            if(!allowDevelopment)return false;
            try{
                return mapper.readValue(payload,LicensePayload.class).licenseId().equals("development");
            }
            catch(Exception e){
                return false;
            }

        }
        try{
            var key=KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
            var sig=Signature.getInstance("Ed25519");
            sig.initVerify(key);
            sig.update(payload);
            return sig.verify(signature);
        }
        catch(Exception e){
            return false;
        }

    }
    private LicenseResponse response(LicenseEntity e,Instant now){
        Instant grace=e.getExpiresAt()==null?null:e.getExpiresAt().plus(Duration.ofDays(e.getGracePeriodDays()));
        LicenseStatus s;
        if(e.getStatus()==LicenseStatus.INVALID)s=LicenseStatus.INVALID;
        else if(e.getExpiresAt()==null||!now.isAfter(e.getExpiresAt()))s=LicenseStatus.ACTIVE;
        else if(grace!=null&&!now.isAfter(grace))s=LicenseStatus.GRACE_PERIOD;
        else s=LicenseStatus.EXPIRED;
        Set<String> features;
        try{
            features=mapper.readValue(e.getFeaturesJson(),mapper.getTypeFactory().constructCollectionType(Set.class,String.class));
        }
        catch(Exception x){
            throw new IllegalStateException(x);
        }
        return new LicenseResponse(e.getId(),e.getLicenseId(),e.getOrganization(),e.getEdition(),e.getMaxUsers(),features,e.getIssuedAt(),e.getExpiresAt(),e.getGracePeriodDays(),grace,s,s==LicenseStatus.EXPIRED||s==LicenseStatus.INVALID,e.getActivatedAt());
    }
    private LicenseResponse missingLicense(){
        return new LicenseResponse(new UUID(0,0),"missing","Unlicensed installation","NONE",0,Set.of(),Instant.EPOCH,null,0,null,LicenseStatus.INVALID,true,Instant.EPOCH);
    }
    private LicenseResponse developmentLicense(){
        return new LicenseResponse(new UUID(0,1),"development","LMSPilot Development","ENTERPRISE",10000,Set.of("AI","LDAP","REPORT_EXPORT","CUSTOM_THEME","INTEGRATIONS","GAMIFICATION"),Instant.EPOCH,null,0,null,LicenseStatus.DEVELOPMENT,false,Instant.EPOCH);
    }
    private ApiException invalid(String m){
        return new ApiException(HttpStatus.BAD_REQUEST,"INVALID_LICENSE",m);
    }

}
