package com.lmspilot.competency.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="course_competency_maps", uniqueConstraints=@UniqueConstraint(name="uq_course_competency", columnNames={
    "course_id","competency_id"
}
))
public class CourseCompetencyMapEntity {
    @Id
    private UUID id=UUID.randomUUID();
    @Column(nullable=false)
    private UUID courseId;
    @ManyToOne(fetch=FetchType.EAGER,optional=false)
    @JoinColumn(name="competency_id",nullable=false)
    private CompetencyEntity competency;
    @Column(nullable=false)
    private int targetLevel=1;
    @Column(nullable=false)
    private Instant createdAt=Instant.now();
    public CourseCompetencyMapEntity(){
    }
    public CourseCompetencyMapEntity(UUID courseId,CompetencyEntity competency){
        this.courseId=courseId;
        this.competency=competency;
    }
    public UUID getId(){
        return id;
    }
    public UUID getCourseId(){
        return courseId;
    }
    public CompetencyEntity getCompetency(){
        return competency;
    }
    public int getTargetLevel(){
        return targetLevel;
    }
    public void setTargetLevel(int v){
        targetLevel=v;
    }
    public Instant getCreatedAt(){
        return createdAt;
    }

}
