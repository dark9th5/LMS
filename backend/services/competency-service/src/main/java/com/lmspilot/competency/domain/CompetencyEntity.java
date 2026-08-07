package com.lmspilot.competency.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="competencies", uniqueConstraints=@UniqueConstraint(name="uq_competency_code", columnNames="code"))
public class CompetencyEntity {
    @Id
    private UUID id=UUID.randomUUID();
    @Column(nullable=false,length=80)
    private String code="";
    @Column(nullable=false,length=220)
    private String name="";
    @Column(columnDefinition="text")
    private String description;
    @Column(length=120)
    private String category;
    @Column(nullable=false)
    private int maxLevel=5;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20)
    private CompetencyStatus status=CompetencyStatus.ACTIVE;
    @Column(nullable=false)
    private Instant createdAt=Instant.now();
    @Column(nullable=false)
    private Instant updatedAt=Instant.now();
    @Version
    private long version;
    public CompetencyEntity(){
    }
    public CompetencyEntity(String code,String name,String description,String category,int maxLevel,CompetencyStatus status){
        this.code=code;
        this.name=name;
        this.description=description;
        this.category=category;
        this.maxLevel=maxLevel;
        this.status=status;
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
    public String getCategory(){
        return category;
    }
    public void setCategory(String v){
        category=v;
    }
    public int getMaxLevel(){
        return maxLevel;
    }
    public void setMaxLevel(int v){
        maxLevel=v;
    }
    public CompetencyStatus getStatus(){
        return status;
    }
    public void setStatus(CompetencyStatus v){
        status=v;
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
