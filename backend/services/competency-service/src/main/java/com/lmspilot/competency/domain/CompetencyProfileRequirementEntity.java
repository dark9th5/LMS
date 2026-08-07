package com.lmspilot.competency.domain;

import jakarta.persistence.*;

import java.util.UUID;
@Entity
@Table(name="competency_profile_requirements", uniqueConstraints=@UniqueConstraint(name="uq_profile_competency", columnNames={
    "profile_id","competency_id"
}
))
public class CompetencyProfileRequirementEntity {
    @Id
    private UUID id=UUID.randomUUID();
    @ManyToOne(fetch=FetchType.EAGER,optional=false)
    @JoinColumn(name="profile_id",nullable=false)
    private CompetencyProfileEntity profile;
    @ManyToOne(fetch=FetchType.EAGER,optional=false)
    @JoinColumn(name="competency_id",nullable=false)
    private CompetencyEntity competency;
    @Column(nullable=false)
    private int requiredLevel=1;
    @Column(nullable=false)
    private double weight=1.0;
    public CompetencyProfileRequirementEntity(){
    }
    public CompetencyProfileRequirementEntity(CompetencyProfileEntity profile,CompetencyEntity competency,int requiredLevel,double weight){
        this.profile=profile;
        this.competency=competency;
        this.requiredLevel=requiredLevel;
        this.weight=weight;
    }
    public UUID getId(){
        return id;
    }
    public CompetencyProfileEntity getProfile(){
        return profile;
    }
    public CompetencyEntity getCompetency(){
        return competency;
    }
    public int getRequiredLevel(){
        return requiredLevel;
    }
    public double getWeight(){
        return weight;
    }

}
