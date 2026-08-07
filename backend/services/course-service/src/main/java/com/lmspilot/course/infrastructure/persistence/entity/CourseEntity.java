package com.lmspilot.course.infrastructure.persistence.entity;

import com.lmspilot.course.domain.enums.CourseStatus;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="courses")
public class CourseEntity {
 @Id public UUID id=UUID.randomUUID();
 @Column(nullable=false,unique=true,length=80) public String code="";
 @Column(nullable=false,length=240) public String name="";
 @Column(columnDefinition="text") public String description;
 @Column(columnDefinition="text") public String objectives;
 @Column(length=500) public String targetAudience;
 public Integer durationMinutes;
 @Column(nullable=false) public double passingScore=70.0;
 @Column(nullable=false,columnDefinition="text") public String completionPolicyJson="{\"requiredLessonPercent\":100}";
 public UUID categoryId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) public CourseStatus status=CourseStatus.DRAFT;
 @Column(nullable=false) public int contentVersion=1;
 @Column(nullable=false) public int publishedVersion=0;
 public Instant publishedAt; public UUID publishedBy;
 @Column(nullable=false) public UUID ownerId=UUID.randomUUID();
 @Column(nullable=false) public Instant createdAt=Instant.now();
 @Column(nullable=false) public Instant updatedAt=Instant.now();
 @Version public long rowVersion=0;
 public CourseEntity() {}
}
