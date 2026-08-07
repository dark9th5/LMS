package com.lmspilot.competency.api;

import jakarta.validation.constraints.NotBlank;

import java.util.*;
public record ProfileRequest(@NotBlank String code,@NotBlank String name,String description,UUID organizationUnitId,String roleCode,boolean active,List<RequirementRequest> requirements){
    public ProfileRequest{
        requirements=requirements==null?List.of():List.copyOf(requirements);
    }

}
