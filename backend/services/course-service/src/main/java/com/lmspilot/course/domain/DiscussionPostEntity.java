package com.lmspilot.course.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="discussion_posts",indexes=@Index(name="idx_discussion_post_thread",columnList="thread_id,created_at"))
public class DiscussionPostEntity {
 @Id public UUID id=UUID.randomUUID(); @Column(name="thread_id",nullable=false) public UUID threadId; @Column(name="author_id",nullable=false) public UUID authorId;
 @Column(name="parent_post_id") public UUID parentPostId; @Column(nullable=false,columnDefinition="text") public String content="";
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) public DiscussionPostStatus status=DiscussionPostStatus.VISIBLE;
 @Column(name="created_at",nullable=false) public Instant createdAt=Instant.now(); @Column(name="updated_at",nullable=false) public Instant updatedAt=Instant.now(); @Version public long version=0;
 public DiscussionPostEntity() {}
}
