package com.lmspilot.notification.domain

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.UUID

enum class NewsStatus { DRAFT, PUBLISHED, ARCHIVED }
enum class NewsAudienceType { SYSTEM, BRANCH, DEPARTMENT, GROUP }

@Entity
@Table(name = "news_articles")
class NewsArticleEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 300) var title: String = "",
    @Column(length = 1000) var summary: String? = null,
    @Column(name = "content_html", nullable = false, columnDefinition = "text") var contentHtml: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: NewsStatus = NewsStatus.DRAFT,
    @Enumerated(EnumType.STRING) @Column(name = "audience_type", nullable = false, length = 30)
    var audienceType: NewsAudienceType = NewsAudienceType.SYSTEM,
    @Column(name = "audience_id") var audienceId: UUID? = null,
    @Column(nullable = false) var pinned: Boolean = false,
    @Column(nullable = false) var priority: Int = 0,
    @Column(name = "acknowledgement_required", nullable = false) var acknowledgementRequired: Boolean = false,
    @Column(name = "publish_from") var publishFrom: Instant? = null,
    @Column(name = "publish_until") var publishUntil: Instant? = null,
    @Column(name = "author_id", nullable = false) var authorId: UUID = UUID.randomUUID(),
    @Column(name = "published_by") var publishedBy: UUID? = null,
    @Column(name = "published_at") var publishedAt: Instant? = null,
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

@Entity
@Table(name = "news_attachments")
@IdClass(NewsAttachmentId::class)
class NewsAttachmentEntity(
    @Id @Column(name = "news_id") var newsId: UUID = UUID.randomUUID(),
    @Id @Column(name = "file_id") var fileId: UUID = UUID.randomUUID(),
    @Column(name = "sort_order", nullable = false) var sortOrder: Int = 0,
)

data class NewsAttachmentId(var newsId: UUID? = null, var fileId: UUID? = null) : Serializable

@Entity
@Table(name = "news_receipts")
@IdClass(NewsReceiptId::class)
class NewsReceiptEntity(
    @Id @Column(name = "news_id") var newsId: UUID = UUID.randomUUID(),
    @Id @Column(name = "user_id") var userId: UUID = UUID.randomUUID(),
    @Column(name = "read_at", nullable = false) var readAt: Instant = Instant.now(),
    @Column(name = "acknowledged_at") var acknowledgedAt: Instant? = null,
)

data class NewsReceiptId(var newsId: UUID? = null, var userId: UUID? = null) : Serializable

interface NewsArticleRepository : org.springframework.data.jpa.repository.JpaRepository<NewsArticleEntity, UUID> {
    fun findAllByOrderByPinnedDescPriorityDescCreatedAtDesc(): List<NewsArticleEntity>
}
interface NewsAttachmentRepository : org.springframework.data.jpa.repository.JpaRepository<NewsAttachmentEntity, NewsAttachmentId> {
    fun findAllByNewsIdOrderBySortOrderAsc(newsId: UUID): List<NewsAttachmentEntity>
    fun deleteAllByNewsId(newsId: UUID)
}
interface NewsReceiptRepository : org.springframework.data.jpa.repository.JpaRepository<NewsReceiptEntity, NewsReceiptId> {
    fun findByNewsIdAndUserId(newsId: UUID, userId: UUID): NewsReceiptEntity?
    fun findAllByUserId(userId: UUID): List<NewsReceiptEntity>
}
