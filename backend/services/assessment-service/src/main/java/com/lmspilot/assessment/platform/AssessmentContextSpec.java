package com.lmspilot.assessment.platform;

import java.time.Instant;

import java.util.UUID;
public record AssessmentContextSpec(AssessmentContextType type, UUID courseId, UUID cohortId, Instant opensAt, Instant closesAt, int maxAttempts, boolean autoGrade) {
    public AssessmentContextSpec {
        if(maxAttempts<=0) throw new IllegalArgumentException("maxAttempts must be positive");
        if(closesAt!=null&&opensAt!=null&&!closesAt.isAfter(opensAt)) throw new IllegalArgumentException("Invalid assessment window");
        if((type==AssessmentContextType.COURSE_QUIZ||type==AssessmentContextType.COURSE_ASSIGNMENT)&&courseId==null) throw new IllegalArgumentException(type+" requires courseId");
        if((type==AssessmentContextType.STANDALONE_EXAM||type==AssessmentContextType.COMPETITION)&&courseId!=null) throw new IllegalArgumentException(type+" must not reference a course");
    }

}
