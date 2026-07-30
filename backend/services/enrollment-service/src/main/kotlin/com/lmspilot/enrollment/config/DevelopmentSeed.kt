package com.lmspilot.enrollment.config

import com.lmspilot.contracts.EnrolledPayload
import com.lmspilot.contracts.EventTypes
import com.lmspilot.enrollment.domain.EnrollmentEntity
import com.lmspilot.enrollment.domain.EnrollmentRepository
import com.lmspilot.enrollment.domain.TrainingClassEntity
import com.lmspilot.enrollment.domain.TrainingClassRepository
import com.lmspilot.support.events.DomainEventPublisher
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Component
class DevelopmentSeed(
    private val classes: TrainingClassRepository,
    private val enrollments: EnrollmentRepository,
    private val events: DomainEventPublisher,
    @Value("\${lmspilot.seed-demo:false}") private val enabled: Boolean,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled || classes.count() > 0) return
        val courseId = UUID.fromString("00000000-0000-0000-0000-000000000101")
        val instructorId = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val studentId = UUID.fromString("00000000-0000-0000-0000-000000000003")
        val now = Instant.now()
        val trainingClass = classes.save(
            TrainingClassEntity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000201"),
                code = "LMS-101-DEMO",
                name = "Lớp làm quen LMSPilot",
                courseId = courseId,
                courseVersion = 1,
                startsAt = now.minus(2, ChronoUnit.DAYS),
                endsAt = now.plus(30, ChronoUnit.DAYS),
                dueAt = now.plus(21, ChronoUnit.DAYS),
                instructorIds = mutableSetOf(instructorId),
                createdBy = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            )
        )
        val enrollment = enrollments.save(
            EnrollmentEntity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000202"),
                classId = trainingClass.id,
                courseId = courseId,
                userId = studentId,
                dueAt = trainingClass.dueAt,
                idempotencyKey = "seed:lms-101-demo:$studentId",
            )
        )
        events.publish(
            EventTypes.ENROLLED,
            "enrollment-service",
            enrollment.id.toString(),
            EnrolledPayload(enrollment.id, trainingClass.id, courseId, studentId, enrollment.dueAt),
        )
    }
}
