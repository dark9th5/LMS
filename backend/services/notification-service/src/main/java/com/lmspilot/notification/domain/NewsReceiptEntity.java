package com.lmspilot.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "news_receipts")
@IdClass(NewsReceiptId.class)
public class NewsReceiptEntity {
    @Id @Column(name = "news_id") public UUID newsId;
    @Id @Column(name = "user_id") public UUID userId;
    @Column(name = "read_at", nullable = false) public Instant readAt = Instant.now();
    @Column(name = "acknowledged_at") public Instant acknowledgedAt;
    public NewsReceiptEntity() {}
}

class NewsReceiptId implements Serializable {
    public UUID newsId;
    public UUID userId;
    public NewsReceiptId() {}
    @Override public boolean equals(Object value) {
        return value instanceof NewsReceiptId other
            && Objects.equals(newsId, other.newsId)
            && Objects.equals(userId, other.userId);
    }
    @Override public int hashCode() { return Objects.hash(newsId, userId); }
}
