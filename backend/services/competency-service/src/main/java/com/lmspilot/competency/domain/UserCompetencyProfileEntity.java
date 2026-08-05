package com.lmspilot.competency.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="user_competency_profiles", uniqueConstraints=@UniqueConstraint(name="uq_user_competency_profile", columnNames={"user_id","profile_id"}))
public class UserCompetencyProfileEntity {
 @Id private UUID id=UUID.randomUUID(); @Column(nullable=false) private UUID userId; @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="profile_id",nullable=false) private CompetencyProfileEntity profile; @Column(nullable=false) private UUID assignedBy; @Column(nullable=false) private Instant assignedAt=Instant.now();
 public UserCompetencyProfileEntity(){} public UserCompetencyProfileEntity(UUID userId,CompetencyProfileEntity profile,UUID assignedBy){this.userId=userId;this.profile=profile;this.assignedBy=assignedBy;}
 public UUID getId(){return id;} public UUID getUserId(){return userId;} public CompetencyProfileEntity getProfile(){return profile;} public UUID getAssignedBy(){return assignedBy;} public Instant getAssignedAt(){return assignedAt;}
}
