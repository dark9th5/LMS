package com.lmspilot.support.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmspilot.contracts.DomainEventEnvelope;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class DomainEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public DomainEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String eventType, String producer, String aggregateId, Object payload) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();
        DomainEventEnvelope envelope = new DomainEventEnvelope(
            eventType, correlationId, producer, aggregateId, objectMapper.valueToTree(payload));
        if (TransactionSynchronizationManager.isActualTransactionActive()
            && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { send(eventType, envelope); }
            });
        } else {
            send(eventType, envelope);
        }
    }

    private void send(String eventType, DomainEventEnvelope envelope) {
        rabbitTemplate.convertAndSend(EventInfrastructureConfiguration.EXCHANGE, eventType, envelope);
    }
}
