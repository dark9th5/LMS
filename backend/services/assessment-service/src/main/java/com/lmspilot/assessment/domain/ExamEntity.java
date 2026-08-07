package com.lmspilot.assessment.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="exams")
public class ExamEntity {
    @Id
    public UUID id=UUID.randomUUID();
    @Column(nullable=false,length=220)
    public String title="";
    public UUID courseId;
    public UUID lessonId;
    @Column(nullable=false)
    public int durationMinutes=30;
    public Instant opensAt;
    public Instant closesAt;
    @Column(nullable=false)
    public int maxAttempts=1;
    @Column(nullable=false)
    public int waitMinutesBetweenAttempts=0;
    @Column(nullable=false)
    public double passingScore=70;
    @Column(nullable=false)
    public boolean shuffleQuestions;
    @Column(nullable=false)
    public boolean shuffleAnswers;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20)
    public ScoreStrategy scoreStrategy=ScoreStrategy.HIGHEST;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20)
    public ExamStatus status=ExamStatus.DRAFT;
    @Column(nullable=false)
    public int examVersion=1;
    @Column(nullable=false)
    public UUID ownerId;
    @Column(nullable=false)
    public Instant createdAt=Instant.now();
    @Column(nullable=false)
    public Instant updatedAt=Instant.now();
    @Version
    public long rowVersion;
    public ExamEntity(){
    }

}
