package com.lmspilot.competency.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="user_competency_assessments", indexes=@Index(name="idx_user_competency_assessment", columnList="user_id,competency_id,assessed_at"))
public class UserCompetencyAssessmentEntity {
    @Id
    private UUID id=UUID.randomUUID();
    @Column(nullable=false)
    private UUID userId;
    @ManyToOne(fetch=FetchType.EAGER,optional=false)
    @JoinColumn(name="competency_id",nullable=false)
    private CompetencyEntity competency;
    @Column(nullable=false)
    private int level;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20)
    private AssessmentSource source=AssessmentSource.SELF;
    @Column(nullable=false)
    private UUID assessedBy;
    @Column(columnDefinition="text")
    private String evidenceJson="{}";
    @Column(nullable=false)
    private Instant assessedAt=Instant.now();
    private Instant validUntil;
    public UserCompetencyAssessmentEntity(){
    }
    public UserCompetencyAssessmentEntity(UUID userId,CompetencyEntity competency,int level,AssessmentSource source,UUID assessedBy,String evidenceJson,Instant validUntil){
        this.userId=userId;
        this.competency=competency;
        this.level=level;
        this.source=source;
        this.assessedBy=assessedBy;
        this.evidenceJson=evidenceJson;
        this.validUntil=validUntil;
    }
    public UUID getId(){
        return id;
    }
    public UUID getUserId(){
        return userId;
    }
    public CompetencyEntity getCompetency(){
        return competency;
    }
    public int getLevel(){
        return level;
    }
    public AssessmentSource getSource(){
        return source;
    }
    public UUID getAssessedBy(){
        return assessedBy;
    }
    public String getEvidenceJson(){
        return evidenceJson;
    }
    public Instant getAssessedAt(){
        return assessedAt;
    }
    public Instant getValidUntil(){
        return validUntil;
    }

}
