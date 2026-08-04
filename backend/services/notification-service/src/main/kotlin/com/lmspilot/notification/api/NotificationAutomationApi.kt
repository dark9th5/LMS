package com.lmspilot.notification.api

import com.lmspilot.contracts.EventTypes
import com.lmspilot.contracts.Permissions
import com.lmspilot.notification.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import org.springframework.http.client.SimpleClientHttpRequestFactory
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

private val TEMPLATE_VARIABLE = Regex("""\{\{\s*([A-Za-z0-9_.-]+)\s*}}""")

data class NotificationTemplateRequest(
    @field:NotBlank @field:Size(max = 80) val code: String,
    @field:NotBlank @field:Size(max = 180) val name: String,
    @field:Size(max = 120) val eventType: String? = null,
    @field:NotBlank @field:Size(max = 240) val titleTemplate: String,
    @field:NotBlank @field:Size(max = 100000) val bodyTemplate: String,
    val inAppEnabled: Boolean = true,
    val emailEnabled: Boolean = false,
    val active: Boolean = true,
)

data class NotificationTemplateResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val eventType: String?,
    val titleTemplate: String,
    val bodyTemplate: String,
    val inAppEnabled: Boolean,
    val emailEnabled: Boolean,
    val active: Boolean,
    val updatedAt: Instant,
)

data class NotificationReminderRuleRequest(
    @field:NotBlank @field:Size(max = 180) val name: String,
    val ruleType: ReminderRuleType = ReminderRuleType.COURSE_DUE,
    val templateId: UUID,
    @field:Min(-365) @field:Max(365) val relativeDays: Int = 7,
    @field:Min(0) @field:Max(23) val hourUtc: Int = 0,
    val enabled: Boolean = true,
)

data class NotificationReminderRuleResponse(
    val id: UUID,
    val name: String,
    val ruleType: ReminderRuleType,
    val templateId: UUID,
    val relativeDays: Int,
    val hourUtc: Int,
    val enabled: Boolean,
    val nextRunAt: Instant,
    val dispatchedCount: Long,
    val updatedAt: Instant,
)

data class ReminderRunResponse(val ruleId: UUID, val matched: Int, val dispatched: Int, val duplicate: Int, val targetDate: LocalDate)

data class DueLearningReminder(
    val enrollmentId: UUID,
    val classId: UUID,
    val courseId: UUID,
    val userId: UUID,
    val dueAt: Instant,
    val progressPercent: Int,
)

data class ResolvedNotificationTemplate(
    val title: String,
    val body: String,
    val channels: Set<NotificationChannel>,
)

@Component
class SafeNotificationTemplateRenderer {
    fun render(template: String, variables: Map<String, Any?>): String = TEMPLATE_VARIABLE.replace(template) { match ->
        variables[match.groupValues[1]]?.toString() ?: match.value
    }
}

@Service
class NotificationTemplateService(
    private val templates: NotificationTemplateRepository,
    private val reminderRules: NotificationReminderRuleRepository,
    private val renderer: SafeNotificationTemplateRenderer,
    private val events: DomainEventPublisher,
) {
    @Transactional(readOnly = true)
    fun list(): List<NotificationTemplateResponse> = templates.findAllByOrderByUpdatedAtDesc().map { it.response() }

    @Transactional
    fun create(input: NotificationTemplateRequest): NotificationTemplateResponse {
        validate(input)
        val code = normalizeCode(input.code)
        if (templates.findByCodeIgnoreCase(code) != null) throw ApiException(HttpStatus.CONFLICT, "NOTIFICATION_TEMPLATE_CODE_EXISTS", "Mã mẫu thông báo đã tồn tại")
        val saved = templates.save(
            NotificationTemplateEntity(
                code = code,
                name = input.name.trim(),
                eventType = input.eventType.normalizedEventType(),
                titleTemplate = input.titleTemplate.trim(),
                bodyTemplate = input.bodyTemplate.trim(),
                inAppEnabled = input.inAppEnabled,
                emailEnabled = input.emailEnabled,
                active = input.active,
                createdBy = CurrentUser.id(),
            )
        )
        audit("NOTIFICATION_TEMPLATE_CREATED", "NotificationTemplate", saved.id)
        return saved.response()
    }

    @Transactional
    fun update(id: UUID, input: NotificationTemplateRequest): NotificationTemplateResponse {
        validate(input)
        val entity = get(id)
        val code = normalizeCode(input.code)
        templates.findByCodeIgnoreCase(code)?.takeIf { it.id != id }?.let {
            throw ApiException(HttpStatus.CONFLICT, "NOTIFICATION_TEMPLATE_CODE_EXISTS", "Mã mẫu thông báo đã tồn tại")
        }
        entity.code = code
        entity.name = input.name.trim()
        entity.eventType = input.eventType.normalizedEventType()
        entity.titleTemplate = input.titleTemplate.trim()
        entity.bodyTemplate = input.bodyTemplate.trim()
        entity.inAppEnabled = input.inAppEnabled
        entity.emailEnabled = input.emailEnabled
        entity.active = input.active
        entity.updatedAt = Instant.now()
        audit("NOTIFICATION_TEMPLATE_UPDATED", "NotificationTemplate", entity.id)
        return entity.response()
    }

    @Transactional
    fun delete(id: UUID) {
        val entity = get(id)
        if (reminderRules.countByTemplateId(id) > 0) throw ApiException(HttpStatus.CONFLICT, "NOTIFICATION_TEMPLATE_IN_USE", "Mẫu đang được quy tắc nhắc hạn sử dụng")
        templates.delete(entity)
        audit("NOTIFICATION_TEMPLATE_DELETED", "NotificationTemplate", entity.id)
    }

    @Transactional(readOnly = true)
    fun getEntity(id: UUID): NotificationTemplateEntity = get(id)

    @Transactional(readOnly = true)
    fun resolve(eventType: String, defaultTitle: String, defaultBody: String, variables: Map<String, Any?>): ResolvedNotificationTemplate {
        val template = templates.findFirstByEventTypeAndActiveTrueOrderByUpdatedAtDesc(eventType)
            ?: return ResolvedNotificationTemplate(defaultTitle, defaultBody, setOf(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
        return resolve(template, variables)
    }

    fun resolve(template: NotificationTemplateEntity, variables: Map<String, Any?>): ResolvedNotificationTemplate {
        val channels = buildSet {
            if (template.inAppEnabled) add(NotificationChannel.IN_APP)
            if (template.emailEnabled) add(NotificationChannel.EMAIL)
        }
        return ResolvedNotificationTemplate(
            title = renderer.render(template.titleTemplate, variables).take(240),
            body = renderer.render(template.bodyTemplate, variables).take(100000),
            channels = channels,
        )
    }

    private fun validate(input: NotificationTemplateRequest) {
        if (!input.inAppEnabled && !input.emailEnabled) throw ApiException(HttpStatus.BAD_REQUEST, "NOTIFICATION_TEMPLATE_CHANNEL_REQUIRED", "Mẫu phải bật ít nhất một kênh")
    }

    private fun audit(action: String, resourceType: String, resourceId: UUID) {
        events.publish(EventTypes.AUDIT_RECORDED, "notification-service", resourceId.toString(), com.lmspilot.contracts.AuditPayload(CurrentUser.id().toString(), CurrentUser.username(), action, resourceType, resourceId.toString(), "SUCCESS"))
    }

    private fun get(id: UUID) = templates.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_TEMPLATE_NOT_FOUND", "Không tìm thấy mẫu thông báo") }
    private fun normalizeCode(value: String) = value.trim().uppercase().replace(Regex("[^A-Z0-9_.-]"), "_").take(80)
    private fun String?.normalizedEventType() = this?.trim()?.takeIf { it.isNotBlank() }?.take(120)
}

@Component
class ReminderReportingClient(
    builder: RestClient.Builder,
    @Value("\${reporting-service.url:http://localhost:8088}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
    @Value("\${notification.reporting-connect-timeout-ms:3000}") connectTimeoutMs: Int,
    @Value("\${notification.reporting-read-timeout-ms:5000}") readTimeoutMs: Int,
) {
    private val client = builder
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(connectTimeoutMs.coerceIn(500, 60_000))
            setReadTimeout(readTimeoutMs.coerceIn(500, 120_000))
        })
        .baseUrl(baseUrl)
        .build()

    fun dueBetween(from: Instant, to: Instant): List<DueLearningReminder> = client.get()
        .uri { uri -> uri.path("/internal/v1/reports/reminders/due").queryParam("from", from.toString()).queryParam("to", to.toString()).build() }
        .header("X-Service-Token", serviceToken)
        .retrieve()
        .body(Array<DueLearningReminder>::class.java)
        ?.toList()
        ?: emptyList()
}

@Service
class NotificationReminderService(
    private val rules: NotificationReminderRuleRepository,
    private val dispatches: NotificationReminderDispatchRepository,
    private val templates: NotificationTemplateService,
    private val reports: ReminderReportingClient,
    private val notifications: NotificationService,
    private val events: DomainEventPublisher,
    @Value("\${notification.reminder-retry-delay-seconds:300}") private val retryDelaySeconds: Long,
) {
    private val log = LoggerFactory.getLogger(NotificationReminderService::class.java)

    @Transactional(readOnly = true)
    fun list(): List<NotificationReminderRuleResponse> = rules.findAllByOrderByUpdatedAtDesc().map { it.response(dispatches.countByRuleId(it.id)) }

    @Transactional
    fun create(input: NotificationReminderRuleRequest): NotificationReminderRuleResponse {
        templates.getEntity(input.templateId)
        val entity = NotificationReminderRuleEntity(
            name = input.name.trim(),
            ruleType = input.ruleType,
            templateId = input.templateId,
            relativeDays = input.relativeDays,
            hourUtc = input.hourUtc,
            enabled = input.enabled,
            nextRunAt = nextDailyRun(input.hourUtc, Instant.now()),
            createdBy = CurrentUser.id(),
        )
        val saved = rules.save(entity)
        audit("NOTIFICATION_REMINDER_CREATED", saved.id)
        return saved.response(0)
    }

    @Transactional
    fun update(id: UUID, input: NotificationReminderRuleRequest): NotificationReminderRuleResponse {
        templates.getEntity(input.templateId)
        val entity = get(id)
        entity.name = input.name.trim()
        entity.ruleType = input.ruleType
        entity.templateId = input.templateId
        entity.relativeDays = input.relativeDays
        entity.hourUtc = input.hourUtc
        entity.enabled = input.enabled
        entity.nextRunAt = nextDailyRun(input.hourUtc, Instant.now())
        entity.updatedAt = Instant.now()
        audit("NOTIFICATION_REMINDER_UPDATED", entity.id)
        return entity.response(dispatches.countByRuleId(entity.id))
    }

    @Transactional
    fun delete(id: UUID) {
        val entity = get(id)
        rules.delete(entity)
        audit("NOTIFICATION_REMINDER_DELETED", entity.id)
    }

    @Transactional
    fun runNow(id: UUID): ReminderRunResponse = dispatch(get(id), LocalDate.now(ZoneOffset.UTC), allowDisabled = true)

    @Scheduled(fixedDelayString = "\${notification.reminder-worker-delay-ms:60000}")
    @Transactional
    fun dispatchDueRules() {
        val now = Instant.now()
        rules.findTop50ByEnabledTrueAndNextRunAtBeforeOrderByNextRunAtAsc(now).forEach { rule ->
            rule.nextRunAt = nextDailyRun(rule.hourUtc, now.plusSeconds(60))
            rule.updatedAt = now
            runCatching { dispatch(rule, LocalDate.now(ZoneOffset.UTC), allowDisabled = false) }
                .onFailure { error ->
                    rule.nextRunAt = now.plusSeconds(retryDelaySeconds.coerceIn(60, 86_400))
                    rule.updatedAt = Instant.now()
                    log.error("Reminder rule {} failed; retry scheduled at {}", rule.id, rule.nextRunAt, error)
                }
        }
    }

    private fun dispatch(rule: NotificationReminderRuleEntity, runDate: LocalDate, allowDisabled: Boolean): ReminderRunResponse {
        val template = templates.getEntity(rule.templateId)
        if (!template.active || (!rule.enabled && !allowDisabled)) return ReminderRunResponse(rule.id, 0, 0, 0, runDate.plusDays(rule.relativeDays.toLong()))
        val targetDate = runDate.plusDays(rule.relativeDays.toLong())
        val from = targetDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        val to = targetDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val due = reports.dueBetween(from, to)
        var sent = 0
        var duplicate = 0
        due.forEach { item ->
            val sourceEventId = UUID.nameUUIDFromBytes("reminder:${rule.id}:${item.enrollmentId}:${item.dueAt}".toByteArray(StandardCharsets.UTF_8))
            val claimed = dispatches.claim(UUID.randomUUID(), rule.id, item.enrollmentId, item.userId, item.dueAt, sourceEventId, Instant.now())
            if (claimed == 0) {
                duplicate += 1
            } else {
                val variables = mapOf(
                    "userId" to item.userId,
                    "enrollmentId" to item.enrollmentId,
                    "classId" to item.classId,
                    "courseId" to item.courseId,
                    "dueAt" to item.dueAt,
                    "dueDate" to item.dueAt.atZone(ZoneOffset.UTC).toLocalDate(),
                    "progressPercent" to item.progressPercent,
                    "relativeDays" to rule.relativeDays,
                )
                val rendered = templates.resolve(template, variables)
                val created = notifications.enqueue(sourceEventId, NotificationMessage(item.userId, "COURSE_DUE_REMINDER", rendered.title, rendered.body, variables), rendered.channels)
                if (created == 0) {
                    // For example, an EMAIL-only template while SMTP delivery is disabled.
                    // Release the claim so the administrator can enable the channel and retry.
                    dispatches.release(rule.id, item.enrollmentId, item.dueAt)
                } else {
                    events.publish(
                        EventTypes.REMINDER_DISPATCHED,
                        "notification-service",
                        item.enrollmentId.toString(),
                        mapOf("ruleId" to rule.id, "userId" to item.userId, "courseId" to item.courseId, "dueAt" to item.dueAt, "sourceEventId" to sourceEventId, "notificationsCreated" to created),
                    )
                    sent += 1
                }
            }
        }
        return ReminderRunResponse(rule.id, due.size, sent, duplicate, targetDate)
    }

    private fun audit(action: String, resourceId: UUID) {
        events.publish(EventTypes.AUDIT_RECORDED, "notification-service", resourceId.toString(), com.lmspilot.contracts.AuditPayload(CurrentUser.id().toString(), CurrentUser.username(), action, "NotificationReminderRule", resourceId.toString(), "SUCCESS"))
    }

    private fun get(id: UUID) = rules.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_REMINDER_NOT_FOUND", "Không tìm thấy quy tắc nhắc hạn") }

    private fun nextDailyRun(hourUtc: Int, from: Instant): Instant {
        var next = ZonedDateTime.ofInstant(from, ZoneOffset.UTC).withHour(hourUtc).withMinute(0).withSecond(0).withNano(0)
        if (!next.toInstant().isAfter(from)) next = next.plusDays(1)
        return next.toInstant()
    }
}

private fun NotificationTemplateEntity.response() = NotificationTemplateResponse(id, code, name, eventType, titleTemplate, bodyTemplate, inAppEnabled, emailEnabled, active, updatedAt)
private fun NotificationReminderRuleEntity.response(dispatched: Long) = NotificationReminderRuleResponse(id, name, ruleType, templateId, relativeDays, hourUtc, enabled, nextRunAt, dispatched, updatedAt)

@RestController
@RequestMapping("/api/v1/notifications/templates")
class NotificationTemplateController(private val service: NotificationTemplateService) {
    @GetMapping
    @PreAuthorize("hasAnyAuthority('${Permissions.NOTIFICATION_TEMPLATES_MANAGE}','${Permissions.NOTIFICATION_REMINDERS_MANAGE}')")
    fun list() = service.list()

    @PostMapping
    @PreAuthorize("hasAuthority('${Permissions.NOTIFICATION_TEMPLATES_MANAGE}')")
    fun create(@Valid @RequestBody input: NotificationTemplateRequest) = service.create(input)

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('${Permissions.NOTIFICATION_TEMPLATES_MANAGE}')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody input: NotificationTemplateRequest) = service.update(id, input)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('${Permissions.NOTIFICATION_TEMPLATES_MANAGE}')")
    fun delete(@PathVariable id: UUID) = service.delete(id)
}

@RestController
@RequestMapping("/api/v1/notifications/reminder-rules")
@PreAuthorize("hasAuthority('${Permissions.NOTIFICATION_REMINDERS_MANAGE}')")
class NotificationReminderController(private val service: NotificationReminderService) {
    @GetMapping fun list() = service.list()
    @PostMapping fun create(@Valid @RequestBody input: NotificationReminderRuleRequest) = service.create(input)
    @PutMapping("/{id}") fun update(@PathVariable id: UUID, @Valid @RequestBody input: NotificationReminderRuleRequest) = service.update(id, input)
    @DeleteMapping("/{id}") fun delete(@PathVariable id: UUID) = service.delete(id)
    @PostMapping("/{id}/run") fun run(@PathVariable id: UUID) = service.runNow(id)
}
