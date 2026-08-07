package com.lmspilot.grading.api;

import jakarta.validation.constraints.DecimalMin;
public record ManualGradeRequest(@DecimalMin("0.0")double score,String feedback,String reason){
    public ManualGradeRequest{
        if(reason==null)reason="Chấm thủ công";
    }

}
