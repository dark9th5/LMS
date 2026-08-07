package com.lmspilot.organization.api;

import com.lmspilot.organization.domain.*;

import java.util.*;
public record OrganizationUnitResponse(UUID id,String code,String name,OrganizationUnitType type,UUID parentId,OrganizationUnitStatus status,int sortOrder,String path,List<OrganizationUnitResponse> children){
    public OrganizationUnitResponse{
        children=children==null?List.of():List.copyOf(children);
    }

}
