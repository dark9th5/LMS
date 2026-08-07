package com.lmspilot.configuration.api;

import com.lmspilot.contracts.Permissions;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;
@RestController
public class ProductConfigurationController{
    private final ProductConfigurationService s;
    public ProductConfigurationController(ProductConfigurationService s){
        this.s=s;
    }
    @GetMapping("/public/v1/configuration")
    public ProductConfigurationResponse publicConfig(){
        return s.get();
    }
    @GetMapping("/api/v1/configuration")
    @PreAuthorize("hasAuthority('"+Permissions.CONFIGURATION_MANAGE+"')")
    public ProductConfigurationResponse get(){
        return s.get();
    }
    @PutMapping("/api/v1/configuration")
    @PreAuthorize("hasAuthority('"+Permissions.CONFIGURATION_MANAGE+"')")
    public ProductConfigurationResponse update(@Valid
    @RequestBody ProductConfigurationRequest i){
        return s.update(i);
    }

}
