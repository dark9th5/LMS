package com.lmspilot.contracts

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

data class DomainEventEnvelope(
    val eventId: UUID = UUID.randomUUID(),
    val eventType: String,
    val eventVersion: Int = 1,
    val occurredAt: Instant = Instant.now(),
    val correlationId: String,
    val producer: String,
    val aggregateId: String,
    val payload: JsonNode,
)

object EventTypes {
    const val USER_CREATED = "identity.user.created.v1"
    const val USER_STATUS_CHANGED = "identity.user.status-changed.v1"
    const val USER_PASSWORD_CHANGED = "identity.user.password-changed.v1"
    const val USER_SESSION_REVOKED = "identity.user.session-revoked.v1"
    const val COURSE_PUBLISHED = "course.course.published.v1"
    const val ENROLLED = "enrollment.learner.enrolled.v1"
    const val LEARNING_PATH_ASSIGNED = "enrollment.learning-path.assigned.v1"
    const val LEARNING_PATH_COMPLETED = "enrollment.learning-path.completed.v1"
    const val LESSON_COMPLETED = "learning.lesson.completed.v1"
    const val XAPI_STATEMENT_RECORDED = "learning.xapi.statement-recorded.v1"
    const val EXAM_SUBMITTED = "assessment.exam.submitted.v1"
    const val EXAM_GRADED = "grading.exam.graded.v1"
    const val GRADE_APPEAL_OPENED = "grading.appeal.opened.v1"
    const val GRADE_APPEAL_RESOLVED = "grading.appeal.resolved.v1"
    const val COURSE_COMPLETED = "learning.course.completed.v1"
    const val CERTIFICATE_ISSUED = "certificate.issued.v1"
    const val AUDIT_RECORDED = "audit.recorded.v1"
    const val COMPETENCY_ASSESSED = "competency.assessed.v1"
    const val REMINDER_DISPATCHED = "notification.reminder.dispatched.v1"
}

data class UserCreatedPayload(
    val userId: UUID,
    val username: String,
    val fullName: String,
    val organizationUnitId: UUID?,
    val roles: Set<String>,
)

data class EnrolledPayload(
    val enrollmentId: UUID,
    val classId: UUID,
    val courseId: UUID,
    val userId: UUID,
    val dueAt: Instant?,
)

data class LessonCompletedPayload(
    val enrollmentId: UUID,
    val courseId: UUID,
    val lessonId: UUID,
    val userId: UUID,
    val progressPercent: Int,
)

data class ExamSubmittedPayload(
    val sessionId: UUID,
    val examId: UUID,
    val userId: UUID,
    val submittedAt: Instant,
)

data class ExamGradedPayload(
    val sessionId: UUID,
    val examId: UUID,
    val userId: UUID,
    val score: Double,
    val maxScore: Double,
    val passed: Boolean,
    val status: String,
    val enrollmentId: UUID? = null,
    val courseId: UUID? = null,
    val lessonId: UUID? = null,
    val effectivePassed: Boolean? = null,
    val effectivePercentage: Double? = null,
    val scoreStrategy: String = "LATEST",
)

data class CourseCompletedPayload(
    val enrollmentId: UUID,
    val courseId: UUID,
    val userId: UUID,
    val completedAt: Instant,
)

data class AuditPayload(
    val actorId: String?,
    val actorUsername: String?,
    val action: String,
    val resourceType: String,
    val resourceId: String?,
    val outcome: String,
    val beforeJson: JsonNode? = null,
    val afterJson: JsonNode? = null,
    val ipAddress: String? = null,
)
