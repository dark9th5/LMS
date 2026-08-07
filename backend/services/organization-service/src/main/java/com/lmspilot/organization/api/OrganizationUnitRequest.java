package com.lmspilot.organization.api;

import com.lmspilot.organization.domain.*;

import jakarta.validation.constraints.*;

import java.util.UUID;
public record OrganizationUnitRequest(@NotBlank
@Size(max=80)String code,@NotBlank
@Size(max=180)String name,OrganizationUnitType type,UUID parentId,OrganizationUnitStatus status,int sortOrder){
    public OrganizationUnitRequest{
        if(status==null)status=OrganizationUnitStatus.ACTIVE;
    }

}
