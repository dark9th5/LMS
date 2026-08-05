package com.lmspilot.grading.api;import java.util.*;public record GradingQuestionPayload(UUID questionId,String type,String prompt,List<String> correctAnswers,double points){}
