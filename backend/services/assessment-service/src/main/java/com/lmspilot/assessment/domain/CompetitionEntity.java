package com.lmspilot.assessment.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="competitions")
public class CompetitionEntity {
    @Id
    @Column(name="assessment_id")
    public UUID assessmentId;
    @Column(name="registration_opens_at")
    public Instant registrationOpensAt;
    @Column(name="registration_closes_at")
    public Instant registrationClosesAt;
    @Enumerated(EnumType.STRING)
    @Column(name="leaderboard_visibility",nullable=false,length=30)
    public LeaderboardVisibility leaderboardVisibility=LeaderboardVisibility.AFTER_CLOSE;
    @Column(name="tie_break_rule",nullable=false,length=80)
    public String tieBreakRule="SCORE_DURATION_SUBMITTED_AT";
    @Enumerated(EnumType.STRING)
    @Column(name="result_status",nullable=false,length=20)
    public CompetitionResultStatus resultStatus=CompetitionResultStatus.PROVISIONAL;
    @Column(name="published_at")
    public Instant publishedAt;
    @Column(name="published_by")
    public UUID publishedBy;
    public CompetitionEntity(){
    }

}
