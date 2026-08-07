package com.lmspilot.assessment.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="questions")
public class QuestionEntity {
    @Id
    public UUID id=UUID.randomUUID();
    @Column(nullable=false)
    public UUID ownerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    public QuestionType type=QuestionType.SINGLE_CHOICE;
    @Column(nullable=false,columnDefinition="text")
    public String prompt="";
    @Column(nullable=false,columnDefinition="text")
    public String optionsJson="[]";
    @Column(nullable=false,columnDefinition="text")
    public String correctAnswersJson="[]";
    @Column(columnDefinition="text")
    public String explanation;
    @Column(nullable=false)
    public int difficulty=1;
    @Column(nullable=false,length=500)
    public String tagsCsv="";
    @Column(nullable=false)
    public double defaultPoints=1;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20)
    public QuestionStatus status=QuestionStatus.ACTIVE;
    @Column(nullable=false)
    public int questionVersion=1;
    @Column(nullable=false)
    public Instant createdAt=Instant.now();
    @Column(nullable=false)
    public Instant updatedAt=Instant.now();
    @Version
    public long rowVersion;
    public QuestionEntity(){
    }

}
