package com.lmspilot.reporting.api;

import org.springframework.amqp.core.*;

import org.springframework.context.annotation.*;
@Configuration
public class ReportingMessagingConfiguration{
    @Bean Queue reportingQueue(){
        return new Queue("reporting.domain-events",true);
    }
    @Bean Binding reportingBinding(Queue q,TopicExchange e){
        return BindingBuilder.bind(q).to(e).with("#");
    }

}
