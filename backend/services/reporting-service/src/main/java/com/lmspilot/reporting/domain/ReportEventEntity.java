package com.lmspilot.reporting.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="report_events",uniqueConstraints=@UniqueConstraint(name="uq_report_event",columnNames="event_id"))public class ReportEventEntity{
    @Id
    private UUID id=UUID.randomUUID();
    @Column(name="event_id",nullable=false)
    private UUID eventId;
    @Column(nullable=false,length=120)
    private String eventType;
    @Column(nullable=false,length=120)
    private String aggregateId;
    @Column(nullable=false)
    private Instant occurredAt;
    @Column(nullable=false,columnDefinition="text")
    private String payloadJson;
    public ReportEventEntity(){
    }
    public ReportEventEntity(UUID e,String t,String a,Instant o,String p){
        eventId=e;
        eventType=t;
        aggregateId=a;
        occurredAt=o;
        payloadJson=p;
    }
    public UUID getEventId(){
        return eventId;
    }

}
