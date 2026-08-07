package com.lmspilot.audit.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.lmspilot.audit.domain.*;

import com.lmspilot.contracts.*;

import java.nio.charset.StandardCharsets;

import java.time.Instant;

import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.data.domain.*;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
@Service
public class AuditService {
    private final AuditEntryRepository repository;
    private final ObjectMapper mapper;
    public AuditService(AuditEntryRepository repository,ObjectMapper mapper){
        this.repository=repository;
        this.mapper=mapper;
    }
    @RabbitListener(queues="audit.recorded")
    @Transactional
    public void consume(DomainEventEnvelope event){
        if(repository.existsByEventId(event.eventId())) return;
        AuditPayload p=mapper.convertValue(event.payload(),AuditPayload.class);
        repository.save(new AuditEntryEntity(event.eventId(),p.actorId(),p.actorUsername(),p.action(),p.resourceType(),p.resourceId(),p.outcome(),
        p.beforeJson()==null?null:p.beforeJson().toString(),p.afterJson()==null?null:p.afterJson().toString(),p.ipAddress(),event.correlationId(),event.occurredAt()));
    }
    @Transactional(readOnly=true)
    public Page<AuditEntryResponse> search(String actor,String action,String resourceType,String resourceId,String outcome,String correlationId,
    Instant from,Instant to,int page,int size){
        Specification<AuditEntryEntity> spec=(root,q,cb)->cb.conjunction();
        if(actor!=null&&!actor.isBlank()) spec=spec.and((root,q,cb)->cb.or(cb.equal(root.get("actorId"),actor),cb.like(cb.lower(root.get("actorUsername")),"%"+actor.toLowerCase()+"%")));
        if(action!=null&&!action.isBlank()) spec=spec.and((root,q,cb)->cb.equal(root.get("action"),action));
        if(resourceType!=null&&!resourceType.isBlank()) spec=spec.and((root,q,cb)->cb.equal(root.get("resourceType"),resourceType));
        if(resourceId!=null&&!resourceId.isBlank()) spec=spec.and((root,q,cb)->cb.equal(root.get("resourceId"),resourceId));
        if(outcome!=null&&!outcome.isBlank()) spec=spec.and((root,q,cb)->cb.equal(root.get("outcome"),outcome.toUpperCase()));
        if(correlationId!=null&&!correlationId.isBlank()) spec=spec.and((root,q,cb)->cb.equal(root.get("correlationId"),correlationId));
        if(from!=null) spec=spec.and((root,q,cb)->cb.greaterThanOrEqualTo(root.get("occurredAt"),from));
        if(to!=null) spec=spec.and((root,q,cb)->cb.lessThanOrEqualTo(root.get("occurredAt"),to));
        return repository.findAll(spec,PageRequest.of(Math.max(0,page),Math.max(1,Math.min(200,size)),Sort.by(Sort.Direction.DESC,"occurredAt"))).map(AuditService::response);
    }
    @Transactional(readOnly = true)
    public byte[] export(String actor, String action, String resourceType, String resourceId, String outcome, String correlationId, Instant from, Instant to) {
        var rows = search(actor, action, resourceType, resourceId, outcome, correlationId, from, to, 0, 10000).getContent();
        String header = "id,actorId,actorUsername,action,resourceType,resourceId,outcome,ipAddress,correlationId,occurredAt,beforeJson,afterJson";
        StringBuilder body = new StringBuilder();
        for (var row : rows) {
            if (body.length() > 0) body.append("\r\n");
            Object[] values = {
                row.id(), nz(row.actorId()), nz(row.actorUsername()), row.action(), row.resourceType(), nz(row.resourceId()), row.outcome(), nz(row.ipAddress()), nz(row.correlationId()), row.occurredAt(), nz(row.beforeJson()), nz(row.afterJson())
            }
            ;
            for (int index = 0;
            index < values.length;
            index++) {
                if (index > 0) body.append(',');
                body.append(csvCell(String.valueOf(values[index])));
            }

        }
        String result = "\uFEFF" + header + "\r\n" + body + (body.length() > 0 ? "\r\n" : "");
        return result.getBytes(StandardCharsets.UTF_8);
    }
    @Transactional
    public void record(DomainEventEnvelope event) {
        consume(event);
    }
    private static String nz(String value) {
        return value == null ? "" : value;
    }
    private static String csvCell(String value) {
        String safe = value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
    private static AuditEntryResponse response(AuditEntryEntity e){
        return new AuditEntryResponse(e.getId(),e.getActorId(),e.getActorUsername(),e.getAction(),e.getResourceType(),e.getResourceId(),e.getOutcome(),e.getBeforeJson(),e.getAfterJson(),e.getIpAddress(),e.getCorrelationId(),e.getOccurredAt());
    }

}
