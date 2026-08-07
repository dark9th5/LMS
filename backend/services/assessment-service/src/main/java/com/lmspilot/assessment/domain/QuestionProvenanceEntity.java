package com.lmspilot.assessment.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="question_provenance",uniqueConstraints=@UniqueConstraint(name="uq_question_provenance_question",columnNames="question_id")) public class QuestionProvenanceEntity {
    @Id
    public UUID id=UUID.randomUUID();
    @Column(name="question_id",nullable=false)
    public UUID questionId;
    @Column(name="course_id",nullable=false)
    public UUID courseId;
    @Column(nullable=false,length=240)
    public String externalId="";
    @Column(nullable=false,columnDefinition="text")
    public String citationsJson="[]";
    @Column(nullable=false,columnDefinition="text")
    public String sourceDocumentVersionsJson="[]";
    @Column(nullable=false,columnDefinition="text")
    public String generatorMetadataJson="{}";
    @Column(nullable=false)
    public UUID importedBy;
    @Column(nullable=false)
    public Instant importedAt=Instant.now();
    public QuestionProvenanceEntity(){
    }

}
