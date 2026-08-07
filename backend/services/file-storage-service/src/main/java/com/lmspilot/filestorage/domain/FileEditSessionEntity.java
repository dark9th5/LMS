package com.lmspilot.filestorage.domain;

import jakarta.persistence.*;

import java.time.*;

import java.util.*;
@Entity
@Table(name="file_edit_sessions")
public class FileEditSessionEntity {
    @Id
    public UUID id=UUID.randomUUID();
    @Column(nullable=false)
    public UUID fileId;
    @Column(nullable=false)
    public UUID baseVersionId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    public FileEditorType editorType=FileEditorType.ONLYOFFICE;
    @Column(nullable=false)
    public UUID userId;
    @Column(nullable=false,length=64)
    public String lockTokenHash="";
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20)
    public FileEditSessionStatus status=FileEditSessionStatus.OPEN;
    @Column(nullable=false)
    public Instant expiresAt;
    @Column(nullable=false)
    public Instant createdAt=Instant.now();
    public Instant closedAt;
    public FileEditSessionEntity(){
    }

}
