package com.lmspilot.reporting.config

import com.lmspilot.reporting.domain.LearnerCourseReadModel
import com.lmspilot.reporting.domain.LearnerCourseReadModelRepository
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
    private val readModels: LearnerCourseReadModelRepository,
    @Value("\${lmspilot.seed-demo:false}") private val enabled: Boolean,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled) return
        val enrollmentId = UUID.fromString("00000000-0000-0000-0000-000000000202")
        val model = readModels.findByEnrollmentId(enrollmentId) ?: LearnerCourseReadModel(
                enrollmentId = enrollmentId,
                classId = UUID.fromString("00000000-0000-0000-0000-000000000201"),
                courseId = UUID.fromString("00000000-0000-0000-0000-000000000101"),
                userId = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                dueAt = Instant.now().plus(21, ChronoUnit.DAYS),
                progressPercent = 50,
                lastActivityAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        model.progressPercent = 50
        model.lastActivityAt = Instant.now()
        model.updatedAt = Instant.now()
        readModels.save(model)
    }
}
