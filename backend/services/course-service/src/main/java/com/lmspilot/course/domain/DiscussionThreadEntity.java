package com.lmspilot.course.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="discussion_threads",indexes=@Index(name="idx_discussion_thread_course",columnList="course_id,updated_at"))
public class DiscussionThreadEntity {
    @Id
    public UUID id=UUID.randomUUID();
    @Column(name="course_id",nullable=false)
    public UUID courseId;
    @Column(name="lesson_id")
    public UUID lessonId;
    @Column(nullable=false,length=240)
    public String title="";
    @Column(name="author_id",nullable=false)
    public UUID authorId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20)
    public DiscussionThreadStatus status=DiscussionThreadStatus.OPEN;
    @Column(nullable=false)
    public boolean pinned=false;
    @Column(name="post_count",nullable=false)
    public int postCount=0;
    @Column(name="created_at",nullable=false)
    public Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false)
    public Instant updatedAt=Instant.now();
    @Version
    public long version=0;
    public DiscussionThreadEntity() {
    }

}
