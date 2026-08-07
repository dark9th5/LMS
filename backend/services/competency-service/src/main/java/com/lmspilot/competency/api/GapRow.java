package com.lmspilot.competency.api;

import java.util.*;
public record GapRow(UUID competencyId,String code,String name,String category,int currentLevel,int requiredLevel,int gap,double weight,List<UUID> recommendedCourseIds){
}
