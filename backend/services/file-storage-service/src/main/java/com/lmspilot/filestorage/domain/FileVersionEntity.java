package com.lmspilot.filestorage.domain;

import jakarta.persistence.*;

import java.time.*;

import java.util.*;
@Entity
@Table(name="file_versions_v2",uniqueConstraints=@UniqueConstraint(name="uq_file_version_number",columnNames={
    "file_id","version_number"
}
)) public class FileVersionEntity {
    @Id
    public UUID id=UUID.randomUUID();
    @Column(name="file_id",nullable=false)
    public UUID fileId;
    @Column(nullable=false)
    public int versionNumber=1;
    @Column(nullable=false,length=1000)
    public String storageKey="";
    @Column(nullable=false,length=255)
    public String mediaType="application/octet-stream";
    @Column(nullable=false)
    public long sizeBytes;
    @Column(nullable=false,length=64)
    public String sha256="";
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    public FileVersionSource sourceType=FileVersionSource.UPLOAD;
    public UUID parentVersionId;
    @Column(length=1000)
    public String changeSummary;
    @Column(nullable=false)
    public UUID createdBy;
    @Column(nullable=false)
    public Instant createdAt=Instant.now();
    public FileVersionEntity(){
    }

}
