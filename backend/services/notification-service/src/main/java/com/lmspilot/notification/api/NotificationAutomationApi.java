package com.lmspilot.notification.api;

import com.lmspilot.notification.domain.NotificationReminderRuleEntity;
import com.lmspilot.notification.domain.NotificationReminderRuleRepository;
import com.lmspilot.notification.domain.NotificationTemplateEntity;
import com.lmspilot.notification.domain.NotificationTemplateRepository;
import com.lmspilot.notification.domain.ReminderRuleType;
import com.lmspilot.support.api.ApiException;
import com.lmspilot.support.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

record NotificationTemplateRequest(
    @NotBlank @Size(max = 80) String code,
    @Size(max = 180) String name,
    @Size(max = 120) String eventType,
    @NotBlank @Size(max = 240) String titleTemplate,
    @NotBlank String bodyTemplate,
    Boolean inAppEnabled,
    Boolean emailEnabled,
    Boolean active
) {}

record NotificationTemplateResponse(
    UUID id,
    String code,
    String name,
    String eventType,
    String titleTemplate,
    String bodyTemplate,
    boolean inAppEnabled,
    boolean emailEnabled,
    boolean active,
    Instant updatedAt
) {}

record NotificationReminderRuleRequest(
    @NotBlank @Size(max = 180) String name,
    ReminderRuleType type,
    @NotNull UUID templateId,
    @Min(-365) @Max(365) Integer daysBeforeDue,
    @Min(0) @Max(23) Integer hourUtc,
    Boolean enabled
) {}

record NotificationReminderRuleResponse(
    UUID id,
    String name,
    ReminderRuleType type,
    UUID templateId,
    int daysBeforeDue,
    int hourUtc,
    boolean enabled,
    Instant nextRunAt,
    Instant updatedAt
) {}

record ReminderRunResponse(UUID ruleId, int matched, int dispatched, int duplicate, LocalDate targetDate) {}

@Service
@Transactional
class NotificationAutomationService {
    private final NotificationTemplateRepository templates;
    private final NotificationReminderRuleRepository rules;

    NotificationAutomationService(
        NotificationTemplateRepository templates,
        NotificationReminderRuleRepository rules
    ) {
        this.templates = templates;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    List<NotificationTemplateResponse> templates() {
        return templates.findAllByOrderByCodeAsc().stream().map(this::view).toList();
    }

    NotificationTemplateResponse saveTemplate(UUID id, NotificationTemplateRequest input) {
        NotificationTemplateEntity entity = id == null
            ? new NotificationTemplateEntity()
            : templates.findById(id).orElseThrow(() -> notFound("TEMPLATE_NOT_FOUND", "Không tìm thấy mẫu"));
        if (id == null && templates.findByCodeIgnoreCase(input.code()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "TEMPLATE_CODE_EXISTS", "Mã mẫu đã tồn tại");
        }
        String code = input.code().trim().toUpperCase(Locale.ROOT);
        entity.code = code;
        entity.name = normalize(input.name(), code);
        entity.eventType = normalize(input.eventType(), null);
        entity.titleTemplate = input.titleTemplate().trim();
        entity.bodyTemplate = input.bodyTemplate().trim();
        entity.inAppEnabled = input.inAppEnabled() == null || input.inAppEnabled();
        entity.emailEnabled = Boolean.TRUE.equals(input.emailEnabled());
        entity.active = input.active() == null || input.active();
        if (id == null) entity.createdBy = user();
        entity.updatedAt = Instant.now();
        return view(templates.save(entity));
    }

    void deleteTemplate(UUID id) {
        if (!templates.existsById(id)) throw notFound("TEMPLATE_NOT_FOUND", "Không tìm thấy mẫu");
        templates.deleteById(id);
    }

    @Transactional(readOnly = true)
    List<NotificationReminderRuleResponse> rules() {
        return rules.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList();
    }

    NotificationReminderRuleResponse saveRule(UUID id, NotificationReminderRuleRequest input) {
        templates.findById(input.templateId())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "TEMPLATE_NOT_FOUND", "Không tìm thấy mẫu"));
        NotificationReminderRuleEntity entity = id == null
            ? new NotificationReminderRuleEntity()
            : rules.findById(id).orElseThrow(() -> notFound("RULE_NOT_FOUND", "Không tìm thấy quy tắc"));
        entity.name = input.name().trim();
        entity.ruleType = input.type() == null ? ReminderRuleType.COURSE_DUE : input.type();
        entity.templateId = input.templateId();
        entity.relativeDays = input.daysBeforeDue() == null ? 1 : input.daysBeforeDue();
        entity.hourUtc = input.hourUtc() == null ? 0 : input.hourUtc();
        entity.enabled = input.enabled() == null || input.enabled();
        if (id == null) entity.createdBy = user();
        entity.nextRunAt = nextRun(entity.hourUtc);
        entity.updatedAt = Instant.now();
        return view(rules.save(entity));
    }

    void deleteRule(UUID id) {
        if (!rules.existsById(id)) throw notFound("RULE_NOT_FOUND", "Không tìm thấy quy tắc");
        rules.deleteById(id);
    }

    ReminderRunResponse run(UUID id) {
        NotificationReminderRuleEntity entity = rules.findById(id)
            .orElseThrow(() -> notFound("RULE_NOT_FOUND", "Không tìm thấy quy tắc"));
        entity.nextRunAt = nextRun(entity.hourUtc);
        rules.save(entity);
        return new ReminderRunResponse(id, 0, 0, 0, LocalDate.now(ZoneOffset.UTC).plusDays(entity.relativeDays));
    }

    private Instant nextRun(int hourUtc) {
        Instant now = Instant.now();
        Instant candidate = now.truncatedTo(ChronoUnit.DAYS).plus(hourUtc, ChronoUnit.HOURS);
        return candidate.isAfter(now) ? candidate : candidate.plus(1, ChronoUnit.DAYS);
    }

    private NotificationTemplateResponse view(NotificationTemplateEntity entity) {
        return new NotificationTemplateResponse(
            entity.id, entity.code, entity.name, entity.eventType, entity.titleTemplate, entity.bodyTemplate,
            entity.inAppEnabled, entity.emailEnabled, entity.active, entity.updatedAt
        );
    }

    private NotificationReminderRuleResponse view(NotificationReminderRuleEntity entity) {
        return new NotificationReminderRuleResponse(
            entity.id, entity.name, entity.ruleType, entity.templateId, entity.relativeDays,
            entity.hourUtc, entity.enabled, entity.nextRunAt, entity.updatedAt
        );
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }

    private ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    private UUID user() {
        try { return CurrentUser.id(); }
        catch (Exception ignored) { return new UUID(0, 1); }
    }
}

@RestController
@RequestMapping("/api/v1/notifications/templates")
public class NotificationAutomationApi {
    private final NotificationAutomationService service;
    public NotificationAutomationApi(NotificationAutomationService service) { this.service = service; }

    @GetMapping public List<NotificationTemplateResponse> list() { return service.templates(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public NotificationTemplateResponse create(@Valid @RequestBody NotificationTemplateRequest input) { return service.saveTemplate(null, input); }
    @PutMapping("/{id}") public NotificationTemplateResponse update(@PathVariable UUID id, @Valid @RequestBody NotificationTemplateRequest input) { return service.saveTemplate(id, input); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { service.deleteTemplate(id); }
}

@RestController
@RequestMapping("/api/v1/notifications/reminder-rules")
class NotificationReminderController {
    private final NotificationAutomationService service;
    NotificationReminderController(NotificationAutomationService service) { this.service = service; }

    @GetMapping List<NotificationReminderRuleResponse> list() { return service.rules(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) NotificationReminderRuleResponse create(@Valid @RequestBody NotificationReminderRuleRequest input) { return service.saveRule(null, input); }
    @PutMapping("/{id}") NotificationReminderRuleResponse update(@PathVariable UUID id, @Valid @RequestBody NotificationReminderRuleRequest input) { return service.saveRule(id, input); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID id) { service.deleteRule(id); }
    @PostMapping("/{id}/run") ReminderRunResponse run(@PathVariable UUID id) { return service.run(id); }
}
