package com.lmspilot.enrollment.domain;

import jakarta.persistence.*;

import java.time.*;

import java.util.*;
@Entity
@Table(name="enrollments",uniqueConstraints={
    @UniqueConstraint(name="uq_enrollment_class_user",columnNames={
        "class_id","user_id"
    }
    ),@UniqueConstraint(name="uq_enrollment_idempotency",columnNames="idempotency_key")
}
)
public class EnrollmentEntity {
    @Id
    public UUID id=UUID.randomUUID();
    @Column(name="class_id",nullable=false)
    public UUID classId;
    @Column(name="course_id",nullable=false)
    public UUID courseId;
    @Column(name="user_id",nullable=false)
    public UUID userId;
    public Instant dueAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    public EnrollmentStatus status=EnrollmentStatus.ENROLLED;
    @Column(name="idempotency_key",nullable=false,length=120)
    public String idempotencyKey=UUID.randomUUID().toString();
    @Column(nullable=false)
    public Instant enrolledAt=Instant.now();
    @Column(nullable=false)
    public Instant updatedAt=Instant.now();
    @Version
    public long version;
    public EnrollmentEntity(){
    }

}
