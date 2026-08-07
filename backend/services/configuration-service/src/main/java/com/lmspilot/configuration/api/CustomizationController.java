package com.lmspilot.configuration.api;

import com.lmspilot.contracts.Permissions;

import jakarta.validation.Valid;

import org.springframework.http.*;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
public class CustomizationController{
    private final BrandingService branding;
    private final BrandingAssetService assets;
    private final ExternalServiceConfigurationService external;
    public CustomizationController(BrandingService b,BrandingAssetService a,ExternalServiceConfigurationService e){
        branding=b;
        assets=a;
        external=e;
    }
    @GetMapping("/public/v1/branding")
    public BrandingResponse publicBranding(){
        return branding.publicBranding();
    }
    @GetMapping("/public/v1/branding/assets/{kind}")
    public ResponseEntity<byte[]>asset(@PathVariable String kind){
        return assets.content(kind);
    }
    @GetMapping("/api/v1/branding")
    @PreAuthorize("hasAuthority('"+Permissions.BRANDING_MANAGE+"')")
    public BrandingResponse branding(){
        return branding.publicBranding();
    }
    @PutMapping("/api/v1/branding")
    @PreAuthorize("hasAuthority('"+Permissions.BRANDING_MANAGE+"')")
    public BrandingResponse update(@Valid
    @RequestBody BrandingRequest i){
        return branding.update(i);
    }
    @GetMapping("/api/v1/external-services")
    @PreAuthorize("hasAnyAuthority('"+Permissions.CONFIGURATION_MANAGE+"','"+Permissions.INTEGRATIONS_MANAGE+"')")
    public List<ExternalServiceResponse>external(){
        return external.list();
    }
    @PostMapping("/api/v1/external-services")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('"+Permissions.CONFIGURATION_MANAGE+"','"+Permissions.INTEGRATIONS_MANAGE+"')")
    public ExternalServiceResponse create(@Valid
    @RequestBody ExternalServiceRequest i){
        return external.save(null,i);
    }
    @PutMapping("/api/v1/external-services/{id}")
    @PreAuthorize("hasAnyAuthority('"+Permissions.CONFIGURATION_MANAGE+"','"+Permissions.INTEGRATIONS_MANAGE+"')")
    public ExternalServiceResponse update(@PathVariable UUID id,@Valid
    @RequestBody ExternalServiceRequest i){
        return external.save(id,i);
    }
    @PostMapping("/api/v1/external-services/{id}/test")
    @PreAuthorize("hasAnyAuthority('"+Permissions.CONFIGURATION_MANAGE+"','"+Permissions.INTEGRATIONS_MANAGE+"')")
    public ExternalServiceResponse test(@PathVariable UUID id){
        return external.test(id);
    }

}
