package com.lmspilot.competency.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="competency_profiles", uniqueConstraints=@UniqueConstraint(name="uq_competency_profile_code", columnNames="code"))
public class CompetencyProfileEntity {
    @Id
    private UUID id=UUID.randomUUID();
    @Column(nullable=false,length=80)
    private String code="";
    @Column(nullable=false,length=220)
    private String name="";
    @Column(columnDefinition="text")
    private String description;
    private UUID organizationUnitId;
    @Column(length=80)
    private String roleCode;
    @Column(nullable=false)
    private boolean active=true;
    @Column(nullable=false)
    private Instant createdAt=Instant.now();
    @Column(nullable=false)
    private Instant updatedAt=Instant.now();
    public CompetencyProfileEntity(){
    }
    public CompetencyProfileEntity(String code,String name,String description,UUID organizationUnitId,String roleCode,boolean active){
        this.code=code;
        this.name=name;
        this.description=description;
        this.organizationUnitId=organizationUnitId;
        this.roleCode=roleCode;
        this.active=active;
    }
    public UUID getId(){
        return id;
    }
    public String getCode(){
        return code;
    }
    public void setCode(String v){
        code=v;
    }
    public String getName(){
        return name;
    }
    public void setName(String v){
        name=v;
    }
    public String getDescription(){
        return description;
    }
    public void setDescription(String v){
        description=v;
    }
    public UUID getOrganizationUnitId(){
        return organizationUnitId;
    }
    public void setOrganizationUnitId(UUID v){
        organizationUnitId=v;
    }
    public String getRoleCode(){
        return roleCode;
    }
    public void setRoleCode(String v){
        roleCode=v;
    }
    public boolean isActive(){
        return active;
    }
    public void setActive(boolean v){
        active=v;
    }
    public Instant getCreatedAt(){
        return createdAt;
    }
    public Instant getUpdatedAt(){
        return updatedAt;
    }
    public void setUpdatedAt(Instant v){
        updatedAt=v;
    }

}
