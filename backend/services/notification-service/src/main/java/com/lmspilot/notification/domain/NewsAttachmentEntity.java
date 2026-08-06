package com.lmspilot.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "news_attachments")
@IdClass(NewsAttachmentId.class)
public class NewsAttachmentEntity {
    @Id @Column(name = "news_id") public UUID newsId;
    @Id @Column(name = "file_id") public UUID fileId;
    @Column(name = "sort_order", nullable = false) public int sortOrder;
    public NewsAttachmentEntity() {}
}

class NewsAttachmentId implements Serializable {
    public UUID newsId;
    public UUID fileId;
    public NewsAttachmentId() {}
    @Override public boolean equals(Object value) {
        return value instanceof NewsAttachmentId other
            && Objects.equals(newsId, other.newsId)
            && Objects.equals(fileId, other.fileId);
    }
    @Override public int hashCode() { return Objects.hash(newsId, fileId); }
}
