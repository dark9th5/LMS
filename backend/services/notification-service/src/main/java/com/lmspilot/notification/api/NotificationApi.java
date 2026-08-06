package com.lmspilot.notification.api;

import com.lmspilot.notification.domain.NotificationChannel;
import com.lmspilot.notification.domain.NotificationEntity;
import com.lmspilot.notification.domain.NotificationRepository;
import com.lmspilot.support.api.ApiException;
import com.lmspilot.support.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record NotificationResponse(UUID id, String type, String title, String body, boolean read, Instant createdAt, Instant readAt) {}
record NotificationSummary(long unread, List<NotificationResponse> items) {}
record NotificationMessage(UUID sourceEventId, UUID userId, String type, String title, String body) {}

@Service
@Transactional
class NotificationService {
    private final NotificationRepository repository;

    NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    NotificationSummary mine() {
        UUID current = user();
        List<NotificationResponse> items = repository
            .findAllByUserIdAndChannelOrderByCreatedAtDesc(current, NotificationChannel.IN_APP)
            .stream()
            .limit(200)
            .map(this::view)
            .toList();
        return new NotificationSummary(
            repository.countByUserIdAndChannelAndReadFalse(current, NotificationChannel.IN_APP),
            items
        );
    }

    NotificationResponse read(UUID id) {
        NotificationEntity entity = repository.findById(id).orElseThrow(this::notFound);
        if (!entity.userId.equals(user())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOTIFICATION_OUT_OF_SCOPE", "Không được đọc thông báo của người khác");
        }
        if (!entity.read) {
            entity.read = true;
            entity.readAt = Instant.now();
        }
        return view(repository.save(entity));
    }

    long readAll() {
        UUID current = user();
        List<NotificationEntity> rows = repository.findAllByUserIdAndChannelOrderByCreatedAtDesc(current, NotificationChannel.IN_APP);
        Instant now = Instant.now();
        long changed = 0;
        for (NotificationEntity entity : rows) {
            if (!entity.read) {
                entity.read = true;
                entity.readAt = now;
                changed++;
            }
        }
        if (changed > 0) repository.saveAll(rows);
        return changed;
    }

    NotificationResponse create(NotificationMessage message) {
        NotificationEntity entity = new NotificationEntity();
        entity.sourceEventId = message.sourceEventId() == null ? UUID.randomUUID() : message.sourceEventId();
        entity.userId = message.userId();
        entity.type = message.type() == null ? "GENERAL" : message.type();
        entity.title = message.title() == null ? "" : message.title();
        entity.body = message.body() == null ? "" : message.body();
        return view(repository.save(entity));
    }

    private NotificationResponse view(NotificationEntity entity) {
        return new NotificationResponse(entity.id, entity.type, entity.title, entity.body, entity.read, entity.createdAt, entity.readAt);
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo");
    }

    private UUID user() {
        try { return CurrentUser.id(); }
        catch (Exception ignored) { return new UUID(0, 1); }
    }
}

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationApi {
    private final NotificationService service;

    public NotificationApi(NotificationService service) { this.service = service; }

    @GetMapping
    public NotificationSummary list() { return service.mine(); }

    @PostMapping("/{id}/read")
    public NotificationResponse read(@PathVariable UUID id) { return service.read(id); }

    @PostMapping("/read-all")
    public Map<String, Long> readAll() { return Map.of("updated", service.readAll()); }
}
