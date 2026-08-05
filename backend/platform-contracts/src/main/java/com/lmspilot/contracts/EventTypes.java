package com.lmspilot.contracts;

public final class EventTypes {
    private EventTypes() {}
    public static final String USER_CREATED = "identity.user.created.v1";
    public static final String USER_STATUS_CHANGED = "identity.user.status-changed.v1";
    public static final String USER_PASSWORD_CHANGED = "identity.user.password-changed.v1";
    public static final String USER_SESSION_REVOKED = "identity.user.session-revoked.v1";
    public static final String COURSE_PUBLISHED = "course.course.published.v1";
    public static final String ENROLLED = "enrollment.learner.enrolled.v1";
    public static final String LEARNING_PATH_ASSIGNED = "enrollment.learning-path.assigned.v1";
    public static final String LEARNING_PATH_COMPLETED = "enrollment.learning-path.completed.v1";
    public static final String LESSON_COMPLETED = "learning.lesson.completed.v1";
    public static final String XAPI_STATEMENT_RECORDED = "learning.xapi.statement-recorded.v1";
    public static final String EXAM_SUBMITTED = "assessment.exam.submitted.v1";
    public static final String EXAM_GRADED = "grading.exam.graded.v1";
    public static final String GRADE_APPEAL_OPENED = "grading.appeal.opened.v1";
    public static final String GRADE_APPEAL_RESOLVED = "grading.appeal.resolved.v1";
    public static final String COURSE_COMPLETED = "learning.course.completed.v1";
    public static final String CERTIFICATE_ISSUED = "certificate.issued.v1";
    public static final String AUDIT_RECORDED = "audit.recorded.v1";
    public static final String COMPETENCY_ASSESSED = "competency.assessed.v1";
    public static final String REMINDER_DISPATCHED = "notification.reminder.dispatched.v1";
}
