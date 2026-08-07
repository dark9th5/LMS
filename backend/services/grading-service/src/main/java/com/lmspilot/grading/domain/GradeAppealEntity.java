package com.lmspilot.grading.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="grade_appeals",uniqueConstraints=@UniqueConstraint(name="uq_open_grade_appeal",columnNames={
    "grade_id","user_id","active_key"
}
),indexes=@Index(name="idx_grade_appeal_status",columnList="status,created_at"))public class GradeAppealEntity{
    @Id
    private UUID id=UUID.randomUUID();
    @Column(name="grade_id",nullable=false)
    private UUID gradeId;
    @Column(name="user_id",nullable=false)
    private UUID userId;
    @Column(nullable=false,columnDefinition="text")
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    private GradeAppealStatus status=GradeAppealStatus.OPEN;
    @Column(name="active_key",nullable=false,length=80)
    private String activeKey="ACTIVE";
    @Column(columnDefinition="text")
    private String resolution;
    @Column(name="resolved_by")
    private UUID resolvedBy;
    @Column(name="created_at",nullable=false)
    private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false)
    private Instant updatedAt=Instant.now();
    @Column(name="resolved_at")
    private Instant resolvedAt;
    @Version
    private long rowVersion;
    public GradeAppealEntity(){
    }
    public GradeAppealEntity(UUID g,UUID u,String r){
        gradeId=g;
        userId=u;
        reason=r;
    }
    public UUID getId(){
        return id;
    }
    public UUID getGradeId(){
        return gradeId;
    }
    public UUID getUserId(){
        return userId;
    }
    public String getReason(){
        return reason;
    }
    public GradeAppealStatus getStatus(){
        return status;
    }
    public void setStatus(GradeAppealStatus v){
        status=v;
    }
    public String getActiveKey(){
        return activeKey;
    }
    public void setActiveKey(String v){
        activeKey=v;
    }
    public String getResolution(){
        return resolution;
    }
    public void setResolution(String v){
        resolution=v;
    }
    public UUID getResolvedBy(){
        return resolvedBy;
    }
    public void setResolvedBy(UUID v){
        resolvedBy=v;
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
    public Instant getResolvedAt(){
        return resolvedAt;
    }
    public void setResolvedAt(Instant v){
        resolvedAt=v;
    }

}
