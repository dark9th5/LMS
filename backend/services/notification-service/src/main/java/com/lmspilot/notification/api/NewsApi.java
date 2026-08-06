package com.lmspilot.notification.api;

import com.lmspilot.notification.domain.NewsArticleEntity;
import com.lmspilot.notification.domain.NewsArticleRepository;
import com.lmspilot.notification.domain.NewsAttachmentEntity;
import com.lmspilot.notification.domain.NewsAttachmentRepository;
import com.lmspilot.notification.domain.NewsAudienceType;
import com.lmspilot.notification.domain.NewsReceiptEntity;
import com.lmspilot.notification.domain.NewsReceiptRepository;
import com.lmspilot.notification.domain.NewsStatus;
import com.lmspilot.notification.platform.NewsPolicy.NewsAudience;
import com.lmspilot.notification.platform.NewsPolicy.NewsPublicationWindow;
import com.lmspilot.support.api.ApiException;
import com.lmspilot.support.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

record NewsRequest(
    @NotBlank @Size(max = 300) String title,
    @Size(max = 1000) String summary,
    @NotBlank String content,
    NewsStatus status,
    NewsAudienceType audienceType,
    UUID audienceId,
    Boolean pinned,
    Integer priority,
    Boolean acknowledgementRequired,
    Instant publishFrom,
    Instant publishUntil,
    List<UUID> attachmentFileIds
) {}

record NewsResponse(
    UUID id,
    String title,
    String summary,
    String content,
    NewsStatus status,
    NewsAudienceType audienceType,
    UUID audienceId,
    boolean pinned,
    int priority,
    boolean acknowledgementRequired,
    Instant publishFrom,
    Instant publishUntil,
    List<UUID> attachmentFileIds,
    boolean read,
    boolean acknowledged,
    Instant createdAt,
    Instant updatedAt
) {}

@Service
@Transactional
class NewsService {
    private final NewsArticleRepository articles;
    private final NewsAttachmentRepository attachments;
    private final NewsReceiptRepository receipts;

    NewsService(NewsArticleRepository articles, NewsAttachmentRepository attachments, NewsReceiptRepository receipts) {
        this.articles = articles;
        this.attachments = attachments;
        this.receipts = receipts;
    }

    @Transactional(readOnly = true)
    List<NewsResponse> feed() {
        Instant now = Instant.now();
        List<NewsArticleEntity> rows = articles
            .findAllByStatusOrderByPinnedDescPriorityDescPublishFromDesc(NewsStatus.PUBLISHED)
            .stream()
            .filter(article -> (article.publishFrom == null || !article.publishFrom.isAfter(now)))
            .filter(article -> article.publishUntil == null || article.publishUntil.isAfter(now))
            .toList();
        return views(rows, user());
    }

    @Transactional(readOnly = true)
    List<NewsResponse> manage() {
        List<NewsArticleEntity> rows = new ArrayList<>(articles.findAll());
        rows.sort(Comparator.comparing((NewsArticleEntity value) -> value.updatedAt).reversed());
        return views(rows, user());
    }

    @Transactional(readOnly = true)
    NewsResponse get(UUID id) {
        return views(List.of(article(id)), user()).getFirst();
    }

    NewsResponse create(NewsRequest input) {
        NewsArticleEntity entity = new NewsArticleEntity();
        UUID current = user();
        entity.authorId = current;
        apply(entity, input);
        entity = articles.save(entity);
        replaceAttachments(entity.id, input.attachmentFileIds());
        return views(List.of(entity), current).getFirst();
    }

    NewsResponse update(UUID id, NewsRequest input) {
        NewsArticleEntity entity = article(id);
        apply(entity, input);
        entity.updatedAt = Instant.now();
        entity = articles.save(entity);
        replaceAttachments(entity.id, input.attachmentFileIds());
        return views(List.of(entity), user()).getFirst();
    }

    NewsResponse publish(UUID id) {
        NewsArticleEntity entity = article(id);
        entity.status = NewsStatus.PUBLISHED;
        if (entity.publishFrom == null) entity.publishFrom = Instant.now();
        entity.publishedAt = Instant.now();
        entity.publishedBy = user();
        entity.updatedAt = entity.publishedAt;
        entity = articles.save(entity);
        return views(List.of(entity), user()).getFirst();
    }

    NewsResponse archive(UUID id) {
        NewsArticleEntity entity = article(id);
        entity.status = NewsStatus.ARCHIVED;
        entity.updatedAt = Instant.now();
        entity = articles.save(entity);
        return views(List.of(entity), user()).getFirst();
    }

    NewsResponse markRead(UUID id) {
        NewsArticleEntity entity = article(id);
        UUID current = user();
        NewsReceiptEntity receipt = receipts.findByNewsIdAndUserId(id, current).orElseGet(NewsReceiptEntity::new);
        receipt.newsId = id;
        receipt.userId = current;
        receipt.readAt = Instant.now();
        receipts.save(receipt);
        return views(List.of(entity), current).getFirst();
    }

    NewsResponse acknowledge(UUID id) {
        NewsArticleEntity entity = article(id);
        UUID current = user();
        NewsReceiptEntity receipt = receipts.findByNewsIdAndUserId(id, current).orElseGet(NewsReceiptEntity::new);
        receipt.newsId = id;
        receipt.userId = current;
        if (receipt.readAt == null) receipt.readAt = Instant.now();
        receipt.acknowledgedAt = Instant.now();
        receipts.save(receipt);
        return views(List.of(entity), current).getFirst();
    }

    private void apply(NewsArticleEntity entity, NewsRequest input) {
        NewsAudienceType audienceType = input.audienceType() == null ? NewsAudienceType.SYSTEM : input.audienceType();
        new NewsAudience(audienceType, input.audienceId());
        new NewsPublicationWindow(input.publishFrom(), input.publishUntil());
        entity.title = input.title().trim();
        entity.summary = normalize(input.summary());
        entity.contentHtml = input.content().trim();
        entity.status = input.status() == null ? NewsStatus.DRAFT : input.status();
        entity.audienceType = audienceType;
        entity.audienceId = input.audienceId();
        entity.pinned = Boolean.TRUE.equals(input.pinned());
        entity.priority = input.priority() == null ? 0 : input.priority();
        entity.acknowledgementRequired = Boolean.TRUE.equals(input.acknowledgementRequired());
        entity.publishFrom = input.publishFrom();
        entity.publishUntil = input.publishUntil();
    }

    private void replaceAttachments(UUID newsId, List<UUID> fileIds) {
        attachments.deleteAllByNewsId(newsId);
        if (fileIds == null || fileIds.isEmpty()) return;
        int order = 0;
        for (UUID fileId : new LinkedHashSet<>(fileIds)) {
            NewsAttachmentEntity item = new NewsAttachmentEntity();
            item.newsId = newsId;
            item.fileId = fileId;
            item.sortOrder = order++;
            attachments.save(item);
        }
    }

    private List<NewsResponse> views(List<NewsArticleEntity> rows, UUID current) {
        if (rows.isEmpty()) return List.of();
        Set<UUID> ids = rows.stream().map(item -> item.id).collect(Collectors.toSet());
        Map<UUID, List<UUID>> files = attachments.findAllByNewsIdInOrderByNewsIdAscSortOrderAsc(ids)
            .stream()
            .collect(Collectors.groupingBy(
                item -> item.newsId,
                LinkedHashMap::new,
                Collectors.mapping(item -> item.fileId, Collectors.toList())
            ));
        Map<UUID, NewsReceiptEntity> receiptMap = receipts.findAllByNewsIdInAndUserId(ids, current)
            .stream()
            .collect(Collectors.toMap(item -> item.newsId, item -> item));
        return rows.stream().map(entity -> {
            NewsReceiptEntity receipt = receiptMap.get(entity.id);
            return new NewsResponse(
                entity.id,
                entity.title,
                entity.summary,
                entity.contentHtml,
                entity.status,
                entity.audienceType,
                entity.audienceId,
                entity.pinned,
                entity.priority,
                entity.acknowledgementRequired,
                entity.publishFrom,
                entity.publishUntil,
                files.getOrDefault(entity.id, List.of()),
                receipt != null,
                receipt != null && receipt.acknowledgedAt != null,
                entity.createdAt,
                entity.updatedAt
            );
        }).toList();
    }

    private NewsArticleEntity article(UUID id) {
        return articles.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NEWS_NOT_FOUND", "Không tìm thấy tin tức"));
    }

    private String normalize(String value) {
        if (value == null) return null;
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }

    private UUID user() {
        try { return CurrentUser.id(); }
        catch (Exception ignored) { return new UUID(0, 1); }
    }
}

@RestController
@RequestMapping("/api/v1/news")
public class NewsApi {
    private final NewsService service;
    public NewsApi(NewsService service) { this.service = service; }

    @GetMapping public List<NewsResponse> feed() { return service.feed(); }
    @GetMapping("/manage") public List<NewsResponse> manage() { return service.manage(); }
    @GetMapping("/{id}") public NewsResponse get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public NewsResponse create(@Valid @RequestBody NewsRequest input) { return service.create(input); }
    @PutMapping("/{id}") public NewsResponse update(@PathVariable UUID id, @Valid @RequestBody NewsRequest input) { return service.update(id, input); }
    @PostMapping("/{id}/publish") public NewsResponse publish(@PathVariable UUID id) { return service.publish(id); }
    @PostMapping("/{id}/archive") public NewsResponse archive(@PathVariable UUID id) { return service.archive(id); }
    @PostMapping("/{id}/read") public NewsResponse read(@PathVariable UUID id) { return service.markRead(id); }
    @PostMapping("/{id}/acknowledge") public NewsResponse acknowledge(@PathVariable UUID id) { return service.acknowledge(id); }
}
