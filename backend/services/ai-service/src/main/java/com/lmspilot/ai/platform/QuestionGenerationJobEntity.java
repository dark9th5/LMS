package com.lmspilot.ai.platform;

import jakarta.persistence.*;

import java.time.*;

import java.util.*;
@Entity
@Table(name="question_generation_jobs")
public class QuestionGenerationJobEntity{
    @Id
    public UUID id=UUID.randomUUID();
    @Column(name="course_id",nullable=false)
    public UUID courseId;
    @Column(name="requested_by",nullable=false)
    public UUID requestedBy;
    @Column(name="provider_config_id",nullable=false)
    public UUID providerConfigId;
    @Column(name="document_version_ids_json",nullable=false,columnDefinition="text")
    public String documentVersionIdsJson="[]";
    @Column(name="generation_options_json",nullable=false,columnDefinition="text")
    public String generationOptionsJson="{}";
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    public QuestionGenerationStatus status=QuestionGenerationStatus.QUEUED;
    @Column(name="question_set_json",columnDefinition="text")
    public String questionSetJson;
    @Column(name="validation_errors_json",columnDefinition="text")
    public String validationErrorsJson;
    @Column(name="error_message",length=2000)
    public String errorMessage;
    @Column(name="created_at",nullable=false)
    public Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false)
    public Instant updatedAt=Instant.now();
    @Column(name="completed_at")
    public Instant completedAt;
    public QuestionGenerationJobEntity(){
    }

}
