package com.lmspilot.competency.api;

import jakarta.validation.constraints.*;
public record CompetencyRequest(@NotBlank String code,@NotBlank String name,String description,String category,@Min(1)
@Max(10) int maxLevel,boolean active){
    public CompetencyRequest{
        if(maxLevel==0)maxLevel=5;
    }

}
