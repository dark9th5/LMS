package com.lmspilot.reporting.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="learner_course_read_model",uniqueConstraints=@UniqueConstraint(name="uq_report_enrollment",columnNames="enrollment_id"))public class LearnerCourseReadModel{
    @Id
    private UUID id=UUID.randomUUID();
    @Column(name="enrollment_id",nullable=false)
    private UUID enrollmentId;
    @Column(nullable=false)
    private UUID classId;
    @Column(nullable=false)
    private UUID courseId;
    @Column(nullable=false)
    private UUID userId;
    private Instant dueAt;
    @Column(nullable=false)
    private int progressPercent;
    @Column(nullable=false)
    private boolean completed;
    private Instant completedAt;
    private Instant lastActivityAt;
    private Double lastScore;
    private Boolean passed;
    @Column(nullable=false)
    private Instant updatedAt=Instant.now();
    public LearnerCourseReadModel(){
    }
    public LearnerCourseReadModel(UUID e,UUID cl,UUID c,UUID u,Instant due,Instant updated){
        enrollmentId=e;
        classId=cl;
        courseId=c;
        userId=u;
        dueAt=due;
        updatedAt=updated;
    }
    public UUID getEnrollmentId(){
        return enrollmentId;
    }
    public UUID getClassId(){
        return classId;
    }
    public UUID getCourseId(){
        return courseId;
    }
    public UUID getUserId(){
        return userId;
    }
    public Instant getDueAt(){
        return dueAt;
    }
    public int getProgressPercent(){
        return progressPercent;
    }
    public void setProgressPercent(int v){
        progressPercent=v;
    }
    public boolean isCompleted(){
        return completed;
    }
    public void setCompleted(boolean v){
        completed=v;
    }
    public Instant getCompletedAt(){
        return completedAt;
    }
    public void setCompletedAt(Instant v){
        completedAt=v;
    }
    public Instant getLastActivityAt(){
        return lastActivityAt;
    }
    public void setLastActivityAt(Instant v){
        lastActivityAt=v;
    }
    public Double getLastScore(){
        return lastScore;
    }
    public void setLastScore(Double v){
        lastScore=v;
    }
    public Boolean getPassed(){
        return passed;
    }
    public void setPassed(Boolean v){
        passed=v;
    }
    public Instant getUpdatedAt(){
        return updatedAt;
    }
    public void setUpdatedAt(Instant v){
        updatedAt=v;
    }

}
