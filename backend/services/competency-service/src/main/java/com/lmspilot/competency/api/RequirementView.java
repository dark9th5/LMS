package com.lmspilot.competency.api; import java.util.UUID; public record RequirementView(UUID competencyId,String competencyCode,String competencyName,int requiredLevel,double weight){}
