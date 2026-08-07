package com.lmspilot.competency.api;

import java.util.*;
public record ProfileView(UUID id,String code,String name,String description,UUID organizationUnitId,String roleCode,boolean active,List<RequirementView> requirements){
}
