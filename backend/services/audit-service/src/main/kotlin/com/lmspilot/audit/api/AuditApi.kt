package com.lmspilot.audit.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.audit.domain.*
import com.lmspilot.contracts.*
import com.lmspilot.support.security.InternalTokenAuthorizer
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

@Configuration
class AuditMessagingConfiguration {
    @Bean fun auditQueue() = Queue("audit.recorded", true)
    @Bean fun auditBinding(auditQueue: Queue, domainEventExchange: TopicExchange) = BindingBuilder.bind(auditQueue).to(domainEventExchange).with(EventTypes.AUDIT_RECORDED)
}

data class AuditEntryResponse(val id: UUID, val actorId: String?, val actorUsername: String?, val action: String, val resourceType: String, val resourceId: String?, val outcome: String, val beforeJson: String?, val afterJson: String?, val ipAddress: String?, val correlationId: String?, val occurredAt: Instant)

@Service
class AuditService(private val repository: AuditEntryRepository, private val mapper: ObjectMapper) {
    @RabbitListener(queues = ["audit.recorded"])
    @Transactional
    fun consume(event: DomainEventEnvelope) {
        if (repository.existsByEventId(event.eventId)) return
        val p = mapper.treeToValue(event.payload, AuditPayload::class.java)
        repository.save(AuditEntryEntity(eventId = event.eventId, actorId = p.actorId, actorUsername = p.actorUsername, action = p.action, resourceType = p.resourceType, resourceId = p.resourceId, outcome = p.outcome, beforeJson = p.beforeJson?.toString(), afterJson = p.afterJson?.toString(), ipAddress = p.ipAddress, correlationId = event.correlationId, occurredAt = event.occurredAt))
    }

    @Transactional(readOnly = true)
    fun search(actor: String?, action: String?, resourceType: String?, from: Instant?, to: Instant?, page: Int, size: Int): Page<AuditEntryResponse> {
        var spec: Specification<AuditEntryEntity> = Specification { _, _, cb -> cb.conjunction() }
        if (!actor.isNullOrBlank()) spec = spec.and { root, _, cb -> cb.or(cb.equal(root.get<String>("actorId"), actor), cb.like(cb.lower(root.get("actorUsername")), "%${actor.lowercase()}%")) }
        if (!action.isNullOrBlank()) spec = spec.and { root, _, cb -> cb.equal(root.get<String>("action"), action) }
        if (!resourceType.isNullOrBlank()) spec = spec.and { root, _, cb -> cb.equal(root.get<String>("resourceType"), resourceType) }
        if (from != null) spec = spec.and { root, _, cb -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from) }
        if (to != null) spec = spec.and { root, _, cb -> cb.lessThanOrEqualTo(root.get("occurredAt"), to) }
        return repository.findAll(spec, PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 200), Sort.by(Sort.Direction.DESC, "occurredAt"))).map { it.response() }
    }

    @Transactional
    fun record(event: DomainEventEnvelope) = consume(event)
}
private fun AuditEntryEntity.response() = AuditEntryResponse(id, actorId, actorUsername, action, resourceType, resourceId, outcome, beforeJson, afterJson, ipAddress, correlationId, occurredAt)

@RestController
@RequestMapping("/api/v1/audit")
class AuditController(private val service: AuditService) {
    @GetMapping @PreAuthorize("hasAuthority('${Permissions.AUDIT_READ}')")
    fun search(@RequestParam(required=false) actor: String?, @RequestParam(required=false) action: String?, @RequestParam(required=false) resourceType: String?, @RequestParam(required=false) from: Instant?, @RequestParam(required=false) to: Instant?, @RequestParam(defaultValue="0") page: Int, @RequestParam(defaultValue="50") size: Int) = service.search(actor, action, resourceType, from, to, page, size)
}

@RestController
@RequestMapping("/internal/v1/audit")
class InternalAuditController(private val service: AuditService, private val internal: InternalTokenAuthorizer) {
    @PostMapping fun record(@RequestHeader("X-Service-Token", required=false) token: String?, @RequestBody event: DomainEventEnvelope) { internal.require(token); service.record(event) }
}
