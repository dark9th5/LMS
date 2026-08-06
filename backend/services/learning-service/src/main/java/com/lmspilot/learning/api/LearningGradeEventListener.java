package com.lmspilot.learning.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmspilot.contracts.DomainEventEnvelope;
import com.lmspilot.contracts.EventTypes;
import com.lmspilot.contracts.ExamGradedPayload;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningGradeEventListener {
    private final ObjectMapper mapper;
    private final LearningProgressService progress;

    public LearningGradeEventListener(ObjectMapper mapper, LearningProgressService progress) {
        this.mapper = mapper;
        this.progress = progress;
    }

    @RabbitListener(queues = LearningMessagingConfiguration.EXAM_GRADED_QUEUE)
    @Transactional
    public void onExamGraded(DomainEventEnvelope event) {
        if (!EventTypes.EXAM_GRADED.equals(event.eventType())) return;
        ExamGradedPayload payload = mapper.convertValue(event.payload(), ExamGradedPayload.class);
        boolean completed = "COMPLETED".equalsIgnoreCase(payload.status());
        boolean passed = payload.effectivePassed() != null ? payload.effectivePassed() : payload.passed();
        if (!completed || !passed) return;
        progress.completePassedExamLesson(
            payload.sessionId(),
            payload.enrollmentId(),
            payload.courseId(),
            payload.lessonId(),
            payload.userId()
        );
    }
}
