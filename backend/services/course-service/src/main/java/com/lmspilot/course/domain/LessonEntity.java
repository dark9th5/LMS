package com.lmspilot.course.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="lessons",uniqueConstraints=@UniqueConstraint(name="uq_lesson_course_order",columnNames={"course_id","sort_order"}))
public class LessonEntity {
 @Id public UUID id=UUID.randomUUID();
 @Column(name="course_id",nullable=false) public UUID courseId;
 @Column(nullable=false,length=220) public String title="";
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) public LessonType type=LessonType.TEXT;
 @Column(columnDefinition="text") public String textContent;
 public UUID fileId;
 @Column(nullable=false) public boolean required=true;
 @Column(nullable=false) public int sortOrder=0;
 @Column(nullable=false) public int estimatedMinutes=0;
 @Column(nullable=false) public Instant createdAt=Instant.now();
 @Column(nullable=false) public Instant updatedAt=Instant.now();
 @Version public long version=0;
 public LessonEntity() {}
}
