package com.lmspilot.reporting.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.lmspilot.contracts.CourseCompletedPayload;

import com.lmspilot.contracts.DomainEventEnvelope;

import com.lmspilot.contracts.EnrolledPayload;

import com.lmspilot.contracts.EventTypes;

import com.lmspilot.contracts.ExamGradedPayload;

import com.lmspilot.contracts.LessonCompletedPayload;

import com.lmspilot.reporting.domain.LearnerCourseReadModel;

import com.lmspilot.reporting.domain.LearnerCourseReadModelRepository;

import com.lmspilot.reporting.domain.ReportEventEntity;

import com.lmspilot.reporting.domain.ReportEventRepository;

import com.lmspilot.support.security.CurrentUser;

import com.lmspilot.support.security.LicenseGuard;

import java.nio.charset.StandardCharsets;

import java.time.Instant;

import java.util.Comparator;

import java.util.List;

import java.util.Set;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
@Service
public class ReportingProjectionService {
    private final ReportEventRepository events;
    private final LearnerCourseReadModelRepository models;
    private final ObjectMapper mapper;
    private final EnrollmentScopeClient scope;
    private final LicenseGuard license;
    public ReportingProjectionService(
    ReportEventRepository events,
    LearnerCourseReadModelRepository models,
    ObjectMapper mapper,
    EnrollmentScopeClient scope,
    LicenseGuard license
    ) {
        this.events = events;
        this.models = models;
        this.mapper = mapper;
        this.scope = scope;
        this.license = license;
    }
    @RabbitListener(queues = "reporting.domain-events")
    @Transactional
    public void project(DomainEventEnvelope event) {
        if (events.existsByEventId(event.eventId())) return;
        events.save(new ReportEventEntity(event.eventId(), event.eventType(), event.aggregateId(), event.occurredAt(), write(event.payload())));
        switch (event.eventType()) {
            case EventTypes.ENROLLED -> {
                EnrolledPayload payload = mapper.convertValue(event.payload(), EnrolledPayload.class);
                if (models.findByEnrollmentId(payload.enrollmentId()) == null) {
                    models.save(new LearnerCourseReadModel(payload.enrollmentId(), payload.classId(), payload.courseId(), payload.userId(), payload.dueAt(), event.occurredAt()));
                }

            }
            case EventTypes.LESSON_COMPLETED -> {
                LessonCompletedPayload payload = mapper.convertValue(event.payload(), LessonCompletedPayload.class);
                LearnerCourseReadModel row = models.findByEnrollmentId(payload.enrollmentId());
                if (row != null) {
                    row.setProgressPercent(Math.max(row.getProgressPercent(), payload.progressPercent()));
                    row.setLastActivityAt(event.occurredAt());
                    row.setUpdatedAt(event.occurredAt());
                }

            }
            case EventTypes.COURSE_COMPLETED -> {
                CourseCompletedPayload payload = mapper.convertValue(event.payload(), CourseCompletedPayload.class);
                LearnerCourseReadModel row = models.findByEnrollmentId(payload.enrollmentId());
                if (row != null) {
                    row.setCompleted(true);
                    row.setProgressPercent(100);
                    row.setCompletedAt(payload.completedAt());
                    row.setLastActivityAt(payload.completedAt());
                    row.setUpdatedAt(event.occurredAt());
                }

            }
            case EventTypes.EXAM_GRADED -> applyExamGrade(mapper.convertValue(event.payload(), ExamGradedPayload.class), event.occurredAt());
            default -> {
            }

        }

    }
    private void applyExamGrade(ExamGradedPayload payload, Instant occurredAt) {
        LearnerCourseReadModel row = payload.enrollmentId() == null ? null : models.findByEnrollmentId(payload.enrollmentId());
        if (row == null && payload.enrollmentId() == null) {
            List<LearnerCourseReadModel> candidates = models.findAllByUserId(payload.userId()).stream()
            .filter(item -> payload.courseId() == null || item.getCourseId().equals(payload.courseId()))
            .toList();
            if (candidates.size() == 1) row = candidates.getFirst();
        }
        if (row == null) return;
        double score = payload.effectivePercentage() != null
        ? payload.effectivePercentage()
        : payload.maxScore() == 0 ? 0 : payload.score() * 100 / payload.maxScore();
        row.setLastScore(score);
        row.setPassed(payload.effectivePassed() != null ? payload.effectivePassed() : payload.passed());
        row.setUpdatedAt(occurredAt);
    }
    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        List<LearnerCourseReadModel> rows = scoped();
        long completed = rows.stream().filter(LearnerCourseReadModel::isCompleted).count();
        long overdue = rows.stream().filter(row -> !row.isCompleted() && row.getDueAt() != null && row.getDueAt().isBefore(Instant.now())).count();
        long inProgress = rows.stream().filter(row -> !row.isCompleted() && row.getProgressPercent() > 0 && (row.getDueAt() == null || !row.getDueAt().isBefore(Instant.now()))).count();
        double average = rows.stream().mapToInt(LearnerCourseReadModel::getProgressPercent).average().orElse(0);
        Instant last = rows.stream().map(LearnerCourseReadModel::getUpdatedAt).max(Comparator.naturalOrder()).orElse(Instant.now());
        return new DashboardResponse(rows.size(), inProgress, completed, overdue, average, last);
    }
    @Transactional(readOnly = true)
    public List<LearnerCourseRow> rows(boolean self) {
        List<LearnerCourseReadModel> source = self ? models.findAllByUserId(CurrentUser.id()) : scoped();
        return source.stream().map(this::row).toList();
    }
    private List<LearnerCourseReadModel> scoped() {
        if (CurrentUser.hasRole("ADMIN")) return models.findAll();
        Set<UUID> assigned = scope.assignedDeliveryIds(CurrentUser.id());
        return models.findAll().stream().filter(row -> assigned.contains(row.getClassId())).toList();
    }
    public byte[] exportCsv(List<LearnerCourseRow> rows) {
        license.requireFeature("REPORT_EXPORT", false);
        StringBuilder csv = new StringBuilder("\uFEFFenrollmentId,classId,courseId,userId,progressPercent,completed,dueAt,lastScore,passed,updatedAt\r\n");
        for (LearnerCourseRow row : rows) {
            csv.append(csv(row.enrollmentId())).append(',')
            .append(csv(row.classId())).append(',')
            .append(csv(row.courseId())).append(',')
            .append(csv(row.userId())).append(',')
            .append(row.progressPercent()).append(',')
            .append(row.completed()).append(',')
            .append(csv(row.dueAt())).append(',')
            .append(csv(row.lastScore())).append(',')
            .append(csv(row.passed())).append(',')
            .append(csv(row.updatedAt())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    private LearnerCourseRow row(LearnerCourseReadModel item) {
        return new LearnerCourseRow(item.getEnrollmentId(), item.getClassId(), item.getCourseId(), item.getUserId(), item.getProgressPercent(), item.isCompleted(), item.getDueAt(), item.getLastScore(), item.getPassed(), item.getUpdatedAt());
    }
    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }

    }
    private static String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

}
