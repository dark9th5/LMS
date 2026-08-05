package com.lmspilot.support.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventInfrastructureConfiguration {
    public static final String EXCHANGE = "lmspilot.events";

    @Bean
    public TopicExchange domainEventExchange() { return new TopicExchange(EXCHANGE, true, false); }

    @Bean
    public Jackson2JsonMessageConverter domainEventMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
