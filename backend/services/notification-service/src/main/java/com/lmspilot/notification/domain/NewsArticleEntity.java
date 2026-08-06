package com.lmspilot.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "news_articles")
public class NewsArticleEntity {
    @Id public UUID id = UUID.randomUUID();
    @Column(nullable = false, length = 300) public String title = "";
    @Column(length = 1000) public String summary;
    @Column(name = "content_html", nullable = false, columnDefinition = "text") public String contentHtml = "";
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public NewsStatus status = NewsStatus.DRAFT;
    @Enumerated(EnumType.STRING) @Column(name = "audience_type", nullable = false, length = 30) public NewsAudienceType audienceType = NewsAudienceType.SYSTEM;
    @Column(name = "audience_id") public UUID audienceId;
    @Column(nullable = false) public boolean pinned;
    @Column(nullable = false) public int priority;
    @Column(name = "acknowledgement_required", nullable = false) public boolean acknowledgementRequired;
    @Column(name = "publish_from") public Instant publishFrom;
    @Column(name = "publish_until") public Instant publishUntil;
    @Column(name = "author_id", nullable = false) public UUID authorId;
    @Column(name = "published_by") public UUID publishedBy;
    @Column(name = "published_at") public Instant publishedAt;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) public Instant updatedAt = Instant.now();
    @Version public long version;

    public NewsArticleEntity() {}
}
