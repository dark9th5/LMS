package com.lmspilot.learning.api;

import com.lmspilot.contracts.EventTypes;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LearningMessagingConfiguration {
    static final String EXAM_GRADED_QUEUE = "learning.exam-graded";

    @Bean
    Queue learningExamGradedQueue() {
        return new Queue(EXAM_GRADED_QUEUE, true);
    }

    @Bean
    Binding learningExamGradedBinding(Queue learningExamGradedQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(learningExamGradedQueue)
            .to(domainEventExchange)
            .with(EventTypes.EXAM_GRADED);
    }
}
