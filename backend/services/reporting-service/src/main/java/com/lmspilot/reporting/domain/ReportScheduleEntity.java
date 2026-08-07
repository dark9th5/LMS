package com.lmspilot.reporting.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="report_schedules",indexes=@Index(name="idx_report_schedule_due",columnList="enabled,next_run_at"))public class ReportScheduleEntity{
    @Id
    private UUID id=UUID.randomUUID();
    @Column(name="owner_id",nullable=false)
    private UUID ownerId;
    @Column(nullable=false,length=180)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20)
    private ReportScope scope=ReportScope.SELF;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20)
    private ReportFrequency frequency=ReportFrequency.DAILY;
    @Column(name="day_of_week")
    private Integer dayOfWeek;
    @Column(name="hour_utc",nullable=false)
    private int hourUtc;
    @Column(nullable=false)
    private boolean enabled=true;
    @Column(name="next_run_at",nullable=false)
    private Instant nextRunAt=Instant.now();
    @Column(name="created_at",nullable=false)
    private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false)
    private Instant updatedAt=Instant.now();
    @Version
    private long version;
    public ReportScheduleEntity(){
    }
    public ReportScheduleEntity(UUID o,String n,ReportScope s,ReportFrequency f,Integer d,int h,boolean e,Instant next){
        ownerId=o;
        name=n;
        scope=s;
        frequency=f;
        dayOfWeek=d;
        hourUtc=h;
        enabled=e;
        nextRunAt=next;
    }
    public UUID getId(){
        return id;
    }
    public UUID getOwnerId(){
        return ownerId;
    }
    public String getName(){
        return name;
    }
    public void setName(String v){
        name=v;
    }
    public ReportScope getScope(){
        return scope;
    }
    public void setScope(ReportScope v){
        scope=v;
    }
    public ReportFrequency getFrequency(){
        return frequency;
    }
    public void setFrequency(ReportFrequency v){
        frequency=v;
    }
    public Integer getDayOfWeek(){
        return dayOfWeek;
    }
    public void setDayOfWeek(Integer v){
        dayOfWeek=v;
    }
    public int getHourUtc(){
        return hourUtc;
    }
    public void setHourUtc(int v){
        hourUtc=v;
    }
    public boolean isEnabled(){
        return enabled;
    }
    public void setEnabled(boolean v){
        enabled=v;
    }
    public Instant getNextRunAt(){
        return nextRunAt;
    }
    public void setNextRunAt(Instant v){
        nextRunAt=v;
    }
    public Instant getUpdatedAt(){
        return updatedAt;
    }
    public void setUpdatedAt(Instant v){
        updatedAt=v;
    }

}
