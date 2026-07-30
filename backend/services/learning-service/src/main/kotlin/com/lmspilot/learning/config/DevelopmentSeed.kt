package com.lmspilot.learning.config

import com.lmspilot.learning.domain.CourseProgressEntity
import com.lmspilot.learning.domain.CourseProgressRepository
import com.lmspilot.learning.domain.LearningStatus
import com.lmspilot.learning.domain.LessonProgressEntity
import com.lmspilot.learning.domain.LessonProgressRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class DevelopmentSeed(
    private val courses: CourseProgressRepository,
    private val lessons: LessonProgressRepository,
    @Value("\${lmspilot.seed-demo:false}") private val enabled: Boolean,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled) return
        val enrollmentId = UUID.fromString("00000000-0000-0000-0000-000000000202")
        val courseId = UUID.fromString("00000000-0000-0000-0000-000000000101")
        val studentId = UUID.fromString("00000000-0000-0000-0000-000000000003")
        val now = Instant.now()
        val lessonId = UUID.fromString("00000000-0000-0000-0000-000000000111")
        val course = courses.findByEnrollmentId(enrollmentId) ?: CourseProgressEntity(
            enrollmentId = enrollmentId,
            courseId = courseId,
            userId = studentId,
        )
        course.progressPercent = 50
        course.status = LearningStatus.IN_PROGRESS
        course.lastLessonId = lessonId
        course.lastPosition = "completed"
        course.totalLearningSeconds = maxOf(course.totalLearningSeconds, 600)
        course.startedAt = course.startedAt ?: now.minusSeconds(3600)
        course.lastAccessedAt = now
        course.updatedAt = now
        courses.save(course)

        val lesson = lessons.findByEnrollmentIdAndLessonId(enrollmentId, lessonId) ?: LessonProgressEntity(
            enrollmentId = enrollmentId,
            courseId = courseId,
            lessonId = lessonId,
            userId = studentId,
            openedAt = now.minusSeconds(3600),
        )
        lesson.completed = true
        lesson.learningSeconds = maxOf(lesson.learningSeconds, 600)
        lesson.position = "completed"
        lesson.completedAt = lesson.completedAt ?: now.minusSeconds(1200)
        lesson.updatedAt = now
        lessons.save(lesson)
    }
}
