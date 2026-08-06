package com.lmspilot.learning.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmspilot.contracts.EventTypes;
import com.lmspilot.learning.domain.XapiObjectType;
import com.lmspilot.learning.domain.XapiStatementEntity;
import com.lmspilot.learning.domain.XapiStatementRepository;
import com.lmspilot.support.api.ApiException;
import com.lmspilot.support.events.DomainEventPublisher;
import com.lmspilot.support.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

record XapiStatementRequest(
    UUID id,
    @Size(max = 80) String source,
    Instant timestamp,
    @NotBlank @Size(max = 180) String verb,
    @NotBlank @Size(max = 500) String objectId,
    @NotNull XapiObjectType objectType,
    UUID courseId,
    UUID lessonId,
    UUID enrollmentId,
    @DecimalMin("0") @DecimalMax("100") Double score,
    Boolean success,
    Boolean completion,
    @PositiveOrZero Long durationSeconds,
    Map<String, Object> context
) {}

record XapiStatementResponse(
    UUID id,
    UUID actorUserId,
    String source,
    Instant timestamp,
    String verb,
    String objectId,
    XapiObjectType objectType,
    UUID courseId,
    UUID lessonId,
    UUID enrollmentId,
    Double score,
    Boolean success,
    Boolean completion,
    Long durationSeconds,
    Map<String, Object> context,
    Instant storedAt
) {}

@Service
@Transactional
class XapiService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final XapiStatementRepository repository;
    private final ObjectMapper mapper;
    private final DomainEventPublisher events;

    XapiService(XapiStatementRepository repository, ObjectMapper mapper, DomainEventPublisher events) {
        this.repository = repository;
        this.mapper = mapper;
        this.events = events;
    }

    XapiStatementResponse record(XapiStatementRequest input) {
        UUID current = user();
        UUID statementId = input.id() == null ? UUID.randomUUID() : input.id();
        XapiStatementEntity duplicate = repository.findById(statementId).orElse(null);
        if (duplicate != null) {
            if (!Objects.equals(duplicate.actorUserId, current)) {
                throw new ApiException(HttpStatus.CONFLICT, "XAPI_ID_REUSED", "Mã sự kiện xAPI đã được sử dụng");
            }
            return view(duplicate);
        }

        XapiStatementEntity entity = new XapiStatementEntity();
        entity.id = statementId;
        entity.actorUserId = current;
        entity.source = normalizeSource(input.source());
        entity.occurredAt = input.timestamp() == null ? Instant.now() : input.timestamp();
        entity.storedAt = Instant.now();
        entity.verb = input.verb().trim();
        entity.objectId = input.objectId().trim();
        entity.objectType = input.objectType();
        entity.courseId = input.courseId();
        entity.lessonId = input.lessonId();
        entity.enrollmentId = input.enrollmentId();
        entity.resultScore = input.score();
        entity.resultSuccess = input.success();
        entity.resultCompletion = input.completion();
        entity.durationSeconds = input.durationSeconds();
        entity.contextJson = write(input.context());
        XapiStatementEntity saved = repository.save(entity);
        events.publish(
            EventTypes.XAPI_STATEMENT_RECORDED,
            "learning-service",
            saved.id.toString(),
            Map.of(
                "statementId", saved.id,
                "actorUserId", saved.actorUserId,
                "verb", saved.verb,
                "objectId", saved.objectId,
                "objectType", saved.objectType.name(),
                "occurredAt", saved.occurredAt
            )
        );
        return view(saved);
    }

    @Transactional(readOnly = true)
    List<XapiStatementResponse> mine() {
        return byUser(user());
    }

    @Transactional(readOnly = true)
    List<XapiStatementResponse> byUser(UUID userId) {
        return repository.findTop200ByActorUserIdOrderByOccurredAtDesc(userId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    List<XapiStatementResponse> byCourse(UUID courseId) {
        return repository.findTop500ByCourseIdOrderByOccurredAtDesc(courseId).stream().map(this::view).toList();
    }

    private XapiStatementResponse view(XapiStatementEntity entity) {
        return new XapiStatementResponse(
            entity.id,
            entity.actorUserId,
            entity.source,
            entity.occurredAt,
            entity.verb,
            entity.objectId,
            entity.objectType,
            entity.courseId,
            entity.lessonId,
            entity.enrollmentId,
            entity.resultScore,
            entity.resultSuccess,
            entity.resultCompletion,
            entity.durationSeconds,
            read(entity.contextJson),
            entity.storedAt
        );
    }

    private String write(Map<String, Object> value) {
        try {
            return mapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException error) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "XAPI_CONTEXT_INVALID", "Ngữ cảnh xAPI không hợp lệ");
        }
    }

    private Map<String, Object> read(String value) {
        try {
            return value == null || value.isBlank() ? Map.of() : mapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException error) {
            return Map.of();
        }
    }

    private String normalizeSource(String value) {
        String source = value == null ? "WEB" : value.trim().toUpperCase();
        return source.isBlank() ? "WEB" : source;
    }

    private UUID user() {
        try {
            return CurrentUser.id();
        } catch (Exception error) {
            return new UUID(0, 1);
        }
    }
}

@RestController
@RequestMapping("/api/v1/xapi/statements")
public class XapiApi {
    private final XapiService service;

    public XapiApi(XapiService service) {
        this.service = service;
    }

    @PostMapping
    public XapiStatementResponse record(@Valid @RequestBody XapiStatementRequest input) {
        return service.record(input);
    }

    @GetMapping("/me")
    public List<XapiStatementResponse> mine() {
        return service.mine();
    }

    @GetMapping("/users/{userId}")
    public List<XapiStatementResponse> byUser(@PathVariable UUID userId) {
        return service.byUser(userId);
    }

    @GetMapping
    public List<XapiStatementResponse> byCourse(@RequestParam UUID courseId) {
        return service.byCourse(courseId);
    }
}
