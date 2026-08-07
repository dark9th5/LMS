package com.lmspilot.organization.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="organization_memberships_v2",indexes={
    @Index(name="idx_org_membership_user",columnList="user_id"),@Index(name="idx_org_membership_unit",columnList="unit_id")
}
)
public class OrganizationMembershipEntity{
    @Id
    private UUID id=UUID.randomUUID();
    @Column(name="user_id",nullable=false)
    private UUID userId;
    @Column(name="unit_id",nullable=false)
    private UUID unitId;
    @Enumerated(EnumType.STRING)
    @Column(name="membership_type",nullable=false,length=30)
    private MembershipType membershipType=MembershipType.MEMBER;
    @Column(name="primary_membership",nullable=false)
    private boolean primaryMembership;
    @Column(name="valid_from")
    private Instant validFrom;
    @Column(name="valid_until")
    private Instant validUntil;
    @Column(name="created_by",nullable=false)
    private UUID createdBy;
    @Column(name="created_at",nullable=false)
    private Instant createdAt=Instant.now();
    protected OrganizationMembershipEntity(){
    }
    public OrganizationMembershipEntity(UUID userId,UUID unitId,MembershipType membershipType,boolean primaryMembership,Instant validFrom,Instant validUntil,UUID createdBy){
        this.userId=userId;
        this.unitId=unitId;
        this.membershipType=membershipType;
        this.primaryMembership=primaryMembership;
        this.validFrom=validFrom;
        this.validUntil=validUntil;
        this.createdBy=createdBy;
    }
    public UUID getId(){
        return id;
    }
    public UUID getUserId(){
        return userId;
    }
    public UUID getUnitId(){
        return unitId;
    }
    public MembershipType getMembershipType(){
        return membershipType;
    }
    public boolean isPrimaryMembership(){
        return primaryMembership;
    }
    public void setPrimaryMembership(boolean v){
        primaryMembership=v;
    }
    public Instant getValidFrom(){
        return validFrom;
    }
    public Instant getValidUntil(){
        return validUntil;
    }
    public UUID getCreatedBy(){
        return createdBy;
    }
    public Instant getCreatedAt(){
        return createdAt;
    }

}
