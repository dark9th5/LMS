package com.lmspilot.audit.api;

import com.lmspilot.contracts.EventTypes;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditMessagingConfiguration {
    @Bean public Queue auditQueue(){ return new Queue("audit.recorded", true); }
    @Bean public Binding auditBinding(Queue auditQueue, TopicExchange domainEventExchange){
        return BindingBuilder.bind(auditQueue).to(domainEventExchange).with(EventTypes.AUDIT_RECORDED);
    }
}
