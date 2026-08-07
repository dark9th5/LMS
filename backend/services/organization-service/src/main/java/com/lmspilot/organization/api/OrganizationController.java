package com.lmspilot.organization.api;

import com.lmspilot.contracts.Permissions;

import com.lmspilot.organization.domain.OrganizationUnitStatus;

import jakarta.validation.Valid;

import java.util.*;

import org.springframework.http.HttpStatus;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/organization/units")
public class OrganizationController{
    private final OrganizationService service;
    public OrganizationController(OrganizationService s){
        service=s;
    }
    @GetMapping
    @PreAuthorize("hasAuthority('"+Permissions.ORGANIZATION_READ+"')")
    public List<OrganizationUnitResponse> search(@RequestParam(required=false)String query,@RequestParam(required=false)OrganizationUnitStatus status){
        return service.search(query,status);
    }
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('"+Permissions.ORGANIZATION_READ+"')")
    public List<OrganizationUnitResponse> tree(){
        return service.tree();
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('"+Permissions.ORGANIZATION_MANAGE+"','"+Permissions.ORGANIZATION_WRITE+"')")
    public OrganizationUnitResponse create(@Valid
    @RequestBody OrganizationUnitRequest in){
        return service.create(in);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('"+Permissions.ORGANIZATION_MANAGE+"','"+Permissions.ORGANIZATION_WRITE+"')")
    public OrganizationUnitResponse update(@PathVariable UUID id,@Valid
    @RequestBody OrganizationUnitRequest in){
        return service.update(id,in);
    }
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('"+Permissions.ORGANIZATION_MANAGE+"','"+Permissions.ORGANIZATION_WRITE+"')")
    public OrganizationUnitResponse deactivate(@PathVariable UUID id){
        return service.deactivate(id);
    }

}
