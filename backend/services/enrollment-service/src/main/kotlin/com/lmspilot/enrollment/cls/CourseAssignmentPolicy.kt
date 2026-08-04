package com.lmspilot.enrollment.cls

import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class AssigneeType { USER, GROUP, DEPARTMENT, BRANCH }

data class CourseAssignmentSpec(
    val courseId: UUID,
    val assignedVersion: Int,
    val assigneeType: AssigneeType,
    val assigneeId: UUID,
    val availableFrom: Instant? = null,
    val dueAt: Instant? = null,
    val gracePeriod: Duration = Duration.ZERO,
    val required: Boolean = true,
) {
    init {
        require(assignedVersion > 0) { "assignedVersion must be positive" }
        require(!gracePeriod.isNegative) { "gracePeriod cannot be negative" }
        require(dueAt == null || availableFrom == null || dueAt.isAfter(availableFrom)) { "Invalid assignment window" }
    }

    fun effectiveDeadline(): Instant? = dueAt?.plus(gracePeriod)
    fun isLate(submittedAt: Instant): Boolean = effectiveDeadline()?.let(submittedAt::isAfter) ?: false
}
