package com.lmspilot.support.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.DomainEventEnvelope
import org.slf4j.MDC
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

@Configuration
class EventInfrastructureConfiguration {
    @Bean
    fun domainEventExchange() = TopicExchange(EXCHANGE, true, false)

    @Bean
    fun domainEventMessageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    companion object {
        const val EXCHANGE = "lmspilot.events"
    }
}

@Component
class DomainEventPublisher(
    private val rabbitTemplate: RabbitTemplate,
    private val objectMapper: ObjectMapper,
) {
    fun publish(eventType: String, producer: String, aggregateId: String, payload: Any) {
        val envelope = DomainEventEnvelope(
            eventType = eventType,
            correlationId = MDC.get("correlationId") ?: UUID.randomUUID().toString(),
            producer = producer,
            aggregateId = aggregateId,
            payload = objectMapper.valueToTree(payload),
        )
        if (TransactionSynchronizationManager.isActualTransactionActive() &&
            TransactionSynchronizationManager.isSynchronizationActive()
        ) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = send(eventType, envelope)
            })
        } else {
            send(eventType, envelope)
        }
    }

    private fun send(eventType: String, envelope: DomainEventEnvelope) {
        rabbitTemplate.convertAndSend(EventInfrastructureConfiguration.EXCHANGE, eventType, envelope)
    }
}
