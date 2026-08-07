package com.lmspilot.filestorage.domain;

import jakarta.persistence.*;

import java.time.*;

import java.util.*;
@Entity
@Table(name="file_access_grants",uniqueConstraints=@UniqueConstraint(name="uq_file_access_grant",columnNames={
    "file_id","user_id"
}
)) public class FileAccessGrantEntity {
    @Id
    public UUID id=UUID.randomUUID();
    @Column(name="file_id",nullable=false)
    public UUID fileId;
    @Column(name="user_id",nullable=false)
    public UUID userId;
    @Column(nullable=false,length=80)
    public String source="INTERNAL";
    @Column(name="expires_at",nullable=false)
    public Instant expiresAt;
    @Column(name="created_at",nullable=false)
    public Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false)
    public Instant updatedAt=Instant.now();
    public FileAccessGrantEntity(){
    }

}
