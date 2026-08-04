package com.lmspilot.assessment.cls

import java.time.Instant
import java.util.UUID

enum class AssessmentContextType { COURSE_QUIZ, COURSE_ASSIGNMENT, STANDALONE_EXAM, COMPETITION }

data class AssessmentContextSpec(
    val type: AssessmentContextType,
    val courseId: UUID?,
    val cohortId: UUID? = null,
    val opensAt: Instant? = null,
    val closesAt: Instant? = null,
    val maxAttempts: Int = 1,
    val autoGrade: Boolean = true,
) {
    init {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
        require(closesAt == null || opensAt == null || closesAt.isAfter(opensAt)) { "Invalid assessment window" }
        when (type) {
            AssessmentContextType.COURSE_QUIZ,
            AssessmentContextType.COURSE_ASSIGNMENT -> require(courseId != null) { "$type requires courseId" }
            AssessmentContextType.STANDALONE_EXAM,
            AssessmentContextType.COMPETITION -> require(courseId == null) { "$type must not reference a course" }
        }
    }
}
