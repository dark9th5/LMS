package com.lmspilot.assessment.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;

import com.lmspilot.assessment.platform.AssessmentContextType;
@Entity
@Table(name="assessment_contexts")
public class AssessmentContextEntity {
    @Id
    @Column(name="assessment_id")
    public UUID assessmentId;
    @Enumerated(EnumType.STRING)
    @Column(name="context_type",nullable=false,length=30)
    public AssessmentContextType contextType=AssessmentContextType.COURSE_QUIZ;
    @Column(name="course_id")
    public UUID courseId;
    @Column(name="cohort_id")
    public UUID cohortId;
    @Column(name="opens_at")
    public Instant opensAt;
    @Column(name="closes_at")
    public Instant closesAt;
    @Column(name="max_attempts",nullable=false)
    public int maxAttempts=1;
    @Column(name="auto_grade",nullable=false)
    public boolean autoGrade=true;
    public AssessmentContextEntity(){
    }

}
