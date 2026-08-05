package com.lmspilot.course.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="course_categories")
public class CourseCategoryEntity {
 @Id public UUID id=UUID.randomUUID();
 @Column(nullable=false,unique=true,length=80) public String code="";
 @Column(nullable=false,length=180) public String name="";
 public UUID parentId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) public RecordStatus status=RecordStatus.ACTIVE;
 @Column(nullable=false) public int sortOrder=0;
 @Column(nullable=false) public Instant createdAt=Instant.now();
 @Column(nullable=false) public Instant updatedAt=Instant.now();
 @Version public long version=0;
 public CourseCategoryEntity() {}
}
