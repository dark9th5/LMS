package com.lmspilot.organization.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="organization_units",indexes=@Index(name="idx_org_parent",columnList="parent_id"))
public class OrganizationUnitEntity{
    @Id
    private UUID id=UUID.randomUUID();
    @Column(nullable=false,unique=true,length=80)
    private String code="";
    @Column(nullable=false,length=180)
    private String name="";
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=40)
    private OrganizationUnitType type=OrganizationUnitType.DEPARTMENT;
    @Column(name="parent_id")
    private UUID parentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    private OrganizationUnitStatus status=OrganizationUnitStatus.ACTIVE;
    @Column(nullable=false)
    private int sortOrder;
    @Column(nullable=false,length=1200)
    private String materializedPath="/";
    @Column(nullable=false)
    private Instant createdAt=Instant.now();
    @Column(nullable=false)
    private Instant updatedAt=Instant.now();
    @Version
    private long version;
    protected OrganizationUnitEntity(){
    }
    public OrganizationUnitEntity(String code,String name,OrganizationUnitType type,UUID parentId,OrganizationUnitStatus status,int sortOrder,String path){
        this.code=code;
        this.name=name;
        this.type=type;
        this.parentId=parentId;
        this.status=status;
        this.sortOrder=sortOrder;
        this.materializedPath=path;
    }
    public UUID getId(){
        return id;
    }
    public String getCode(){
        return code;
    }
    public String getName(){
        return name;
    }
    public void setName(String v){
        name=v;
    }
    public OrganizationUnitType getType(){
        return type;
    }
    public void setType(OrganizationUnitType v){
        type=v;
    }
    public UUID getParentId(){
        return parentId;
    }
    public void setParentId(UUID v){
        parentId=v;
    }
    public OrganizationUnitStatus getStatus(){
        return status;
    }
    public void setStatus(OrganizationUnitStatus v){
        status=v;
    }
    public int getSortOrder(){
        return sortOrder;
    }
    public void setSortOrder(int v){
        sortOrder=v;
    }
    public String getMaterializedPath(){
        return materializedPath;
    }
    public void setMaterializedPath(String v){
        materializedPath=v;
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
    public long getVersion(){
        return version;
    }

}
