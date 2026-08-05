package com.lmspilot.notification.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.notification.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.UUID

data class NewsRequest(
    @field:NotBlank @field:Size(max = 300) val title: String,
    @field:Size(max = 1000) val summary: String? = null,
    @field:NotBlank @field:Size(max = 100000) val contentHtml: String,
    val audienceType: NewsAudienceType = NewsAudienceType.SYSTEM,
    val audienceId: UUID? = null,
    val pinned: Boolean = false,
    val priority: Int = 0,
    val acknowledgementRequired: Boolean = false,
    val publishFrom: Instant? = null,
    val publishUntil: Instant? = null,
    @field:Size(max = 20) val attachmentFileIds: List<UUID> = emptyList(),
) {
    @AssertTrue(message = "audienceId không hợp lệ")
    fun validAudience(): Boolean = (audienceType == NewsAudienceType.SYSTEM && audienceId == null) ||
        (audienceType != NewsAudienceType.SYSTEM && audienceId != null)

    @AssertTrue(message = "publishUntil phải sau publishFrom")
    fun validWindow(): Boolean = publishUntil == null || publishFrom == null || publishUntil.isAfter(publishFrom)
}

data class NewsResponse(
    val id: UUID,
    val title: String,
    val summary: String?,
    val contentHtml: String,
    val status: NewsStatus,
    val audienceType: NewsAudienceType,
    val audienceId: UUID?,
    val pinned: Boolean,
    val priority: Int,
    val acknowledgementRequired: Boolean,
    val publishFrom: Instant?,
    val publishUntil: Instant?,
    val attachmentFileIds: List<UUID>,
    val read: Boolean,
    val acknowledged: Boolean,
    val authorId: UUID,
    val publishedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Service
class OrganizationAudienceClient(
    builder: RestClient.Builder,
    @Value("\${organization-service.url:http://localhost:8082}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val token: String,
) {
    private val client = builder.baseUrl(baseUrl).build()
    fun unitIds(userId: UUID): Set<UUID> = runCatching {
        (client.get().uri("/internal/v1/organization/users/{id}/unit-ids", userId)
            .header("X-Service-Token", token).retrieve().body(Array<String>::class.java) ?: emptyArray())
            .map(UUID::fromString).toSet()
    }.getOrDefault(emptySet())
}

@Service
class NewsFileClient(
    builder: RestClient.Builder,
    @Value("\${file-storage-service.url:http://localhost:8089}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val token: String,
) {
    data class FileMetadata(val id: UUID, val ownerId: UUID, val purpose: String, val status: String)
    private val client = builder.baseUrl(baseUrl).build()

    fun requireAttachable(fileId: UUID, actorId: UUID, administrator: Boolean) {
        val file = client.get().uri("/internal/v1/files/{id}", fileId)
            .header("X-Service-Token", token).retrieve().body(FileMetadata::class.java)
            ?: throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "FILE_SERVICE_UNAVAILABLE", "Không nhận được metadata tệp đính kèm")
        if (!administrator && file.ownerId != actorId) {
            throw ApiException(HttpStatus.FORBIDDEN, "NEWS_FILE_OWNER_MISMATCH", "Không thể đính kèm tệp của người dùng khác")
        }
        if (file.status != "AVAILABLE" || file.purpose != "NEWS_ATTACHMENT") {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_NEWS_FILE", "Tệp phải khả dụng và được tải lên cho tin tức")
        }
    }

    fun grant(userId: UUID, fileIds: Set<UUID>) {
        if (fileIds.isEmpty()) return
        client.post().uri("/internal/v1/files/access-grants")
            .header("X-Service-Token", token)
            .body(mapOf("userId" to userId, "fileIds" to fileIds, "source" to "NEWS_FEED", "ttlSeconds" to 14_400))
            .retrieve().toBodilessEntity()
    }
}

@Service
class NewsService(
    private val articles: NewsArticleRepository,
    private val attachments: NewsAttachmentRepository,
    private val receipts: NewsReceiptRepository,
    private val audiences: OrganizationAudienceClient,
    private val files: NewsFileClient,
) {
    @Transactional(readOnly = true)
    fun feed(): List<NewsResponse> {
        val userId = CurrentUser.id()
        val unitIds = audiences.unitIds(userId)
        val receiptMap = receipts.findAllByUserId(userId).associateBy { it.newsId }
        val now = Instant.now()
        val result = articles.findAllByOrderByPinnedDescPriorityDescCreatedAtDesc()
            .filter { article ->
                article.status == NewsStatus.PUBLISHED &&
                    (article.publishFrom == null || !article.publishFrom!!.isAfter(now)) &&
                    (article.publishUntil == null || article.publishUntil!!.isAfter(now)) &&
                    (article.audienceType == NewsAudienceType.SYSTEM || article.audienceId in unitIds)
            }
            .map { it.response(attachments, receiptMap[it.id]) }
        files.grant(userId, result.flatMap { it.attachmentFileIds }.toSet())
        return result
    }

    @Transactional(readOnly = true)
    fun manage(): List<NewsResponse> {
        val result = articles.findAllByOrderByPinnedDescPriorityDescCreatedAtDesc().map { it.response(attachments, null) }
        files.grant(CurrentUser.id(), result.flatMap { it.attachmentFileIds }.toSet())
        return result
    }

    @Transactional
    fun create(input: NewsRequest): NewsResponse {
        validateAttachments(input.attachmentFileIds)
        val now = Instant.now()
        val entity = articles.save(
            NewsArticleEntity(
                title = input.title.trim(),
                summary = input.summary?.trim()?.takeIf { it.isNotBlank() },
                contentHtml = sanitizeNewsHtml(input.contentHtml),
                audienceType = input.audienceType,
                audienceId = input.audienceId,
                pinned = input.pinned,
                priority = input.priority,
                acknowledgementRequired = input.acknowledgementRequired,
                publishFrom = input.publishFrom,
                publishUntil = input.publishUntil,
                authorId = CurrentUser.id(),
                createdAt = now,
                updatedAt = now,
            )
        )
        saveAttachments(entity.id, input.attachmentFileIds)
        return entity.response(attachments, null)
    }

    @Transactional
    fun update(id: UUID, input: NewsRequest): NewsResponse {
        val entity = requireArticle(id)
        if (entity.status == NewsStatus.ARCHIVED) throw ApiException(HttpStatus.CONFLICT, "NEWS_ARCHIVED", "Không thể sửa tin đã lưu trữ")
        validateAttachments(input.attachmentFileIds)
        entity.title = input.title.trim()
        entity.summary = input.summary?.trim()?.takeIf { it.isNotBlank() }
        entity.contentHtml = sanitizeNewsHtml(input.contentHtml)
        entity.audienceType = input.audienceType
        entity.audienceId = input.audienceId
        entity.pinned = input.pinned
        entity.priority = input.priority
        entity.acknowledgementRequired = input.acknowledgementRequired
        entity.publishFrom = input.publishFrom
        entity.publishUntil = input.publishUntil
        entity.updatedAt = Instant.now()
        attachments.deleteAllByNewsId(id)
        saveAttachments(id, input.attachmentFileIds)
        return entity.response(attachments, null)
    }

    @Transactional
    fun publish(id: UUID): NewsResponse {
        val entity = requireArticle(id)
        entity.status = NewsStatus.PUBLISHED
        entity.publishedBy = CurrentUser.id()
        entity.publishedAt = Instant.now()
        if (entity.publishFrom == null) entity.publishFrom = entity.publishedAt
        entity.updatedAt = Instant.now()
        return entity.response(attachments, null)
    }

    @Transactional
    fun archive(id: UUID): NewsResponse {
        val entity = requireArticle(id)
        entity.status = NewsStatus.ARCHIVED
        entity.updatedAt = Instant.now()
        return entity.response(attachments, null)
    }

    @Transactional
    fun read(id: UUID, acknowledge: Boolean): NewsResponse {
        val article = requireVisible(id)
        val userId = CurrentUser.id()
        val receipt = receipts.findByNewsIdAndUserId(id, userId) ?: NewsReceiptEntity(newsId = id, userId = userId)
        if (acknowledge) receipt.acknowledgedAt = Instant.now()
        receipts.save(receipt)
        return article.response(attachments, receipt)
    }

    private fun requireVisible(id: UUID): NewsArticleEntity = feed().firstOrNull { it.id == id }?.let { requireArticle(id) }
        ?: throw ApiException(HttpStatus.NOT_FOUND, "NEWS_NOT_FOUND", "Không tìm thấy tin tức trong phạm vi của bạn")

    private fun requireArticle(id: UUID) = articles.findById(id).orElseThrow {
        ApiException(HttpStatus.NOT_FOUND, "NEWS_NOT_FOUND", "Không tìm thấy tin tức")
    }

    private fun saveAttachments(newsId: UUID, fileIds: List<UUID>) {
        fileIds.distinct().forEachIndexed { index, fileId ->
            attachments.save(NewsAttachmentEntity(newsId, fileId, index))
        }
    }

    private fun validateAttachments(fileIds: List<UUID>) {
        val administrator =
            Permissions.NEWS_MANAGE in CurrentUser.authorities() ||
            Permissions.NEWS_PUBLISH in CurrentUser.authorities()
        fileIds.distinct().forEach { files.requireAttachable(it, CurrentUser.id(), administrator) }
    }

}

/**
 * Escape everything first, then restore only a tiny set of attribute-free
 * formatting tags. This keeps malformed HTML, event handlers, URLs, styles,
 * SVG and legacy stored payloads inert without relying on browser parsing.
 */
private fun sanitizeNewsHtml(value: String): String {
    // Decode only the five entities produced by this sanitizer so applying it
    // again to stored content is idempotent; all other entities remain text.
    val canonical = value
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
    val escaped = canonical
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
    val safeTag = Regex("&lt;(/?)(p|br|strong|em|ul|ol|li|blockquote|h2|h3|h4)(/?)&gt;", RegexOption.IGNORE_CASE)
    return safeTag.replace(escaped) { match ->
        val closing = match.groupValues[1] == "/"
        val tag = match.groupValues[2].lowercase()
        when {
            tag == "br" -> "<br>"
            closing -> "</$tag>"
            else -> "<$tag>"
        }
    }.trim()
}

private fun NewsArticleEntity.response(attachments: NewsAttachmentRepository, receipt: NewsReceiptEntity?) = NewsResponse(
    id, title, summary, sanitizeNewsHtml(contentHtml), status, audienceType, audienceId, pinned, priority,
    acknowledgementRequired, publishFrom, publishUntil,
    attachments.findAllByNewsIdOrderBySortOrderAsc(id).map { it.fileId },
    receipt != null, receipt?.acknowledgedAt != null, authorId, publishedAt, createdAt, updatedAt,
)

@RestController
@RequestMapping("/api/v1/news")
class NewsController(private val service: NewsService) {
    @GetMapping("/feed")
    @PreAuthorize("hasAuthority('${Permissions.NEWS_READ}')")
    fun feed() = service.feed()

    @GetMapping
    @PreAuthorize("hasAuthority('${Permissions.NEWS_MANAGE}')")
    fun manage() = service.manage()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.NEWS_MANAGE}')")
    fun create(@Valid @RequestBody input: NewsRequest) = service.create(input)

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('${Permissions.NEWS_MANAGE}')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody input: NewsRequest) = service.update(id, input)

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyAuthority('${Permissions.NEWS_PUBLISH}','${Permissions.NEWS_MANAGE}')")
    fun publish(@PathVariable id: UUID) = service.publish(id)

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('${Permissions.NEWS_MANAGE}')")
    fun archive(@PathVariable id: UUID) = service.archive(id)

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('${Permissions.NEWS_READ}')")
    fun read(@PathVariable id: UUID) = service.read(id, false)

    @PutMapping("/{id}/acknowledge")
    @PreAuthorize("hasAuthority('${Permissions.NEWS_READ}')")
    fun acknowledge(@PathVariable id: UUID) = service.read(id, true)
}
