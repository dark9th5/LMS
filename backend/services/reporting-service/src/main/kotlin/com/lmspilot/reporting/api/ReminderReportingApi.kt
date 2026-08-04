package com.lmspilot.reporting.api

import com.lmspilot.reporting.domain.LearnerCourseReadModelRepository
import com.lmspilot.support.security.InternalTokenAuthorizer
import com.lmspilot.support.api.ApiException
import org.springframework.http.HttpStatus
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class DueLearningReminder(
    val enrollmentId: UUID,
    val classId: UUID,
    val courseId: UUID,
    val userId: UUID,
    val dueAt: Instant,
    val progressPercent: Int,
)

@Service
class ReminderReportingService(private val readModels: LearnerCourseReadModelRepository) {
    @Transactional(readOnly = true)
    fun dueBetween(from: Instant, to: Instant): List<DueLearningReminder> {
        if (!from.isBefore(to)) throw ApiException(HttpStatus.BAD_REQUEST, "REMINDER_WINDOW_INVALID", "Thời điểm bắt đầu phải trước thời điểm kết thúc")
        if (to.epochSecond - from.epochSecond > 32L * 86400L) throw ApiException(HttpStatus.BAD_REQUEST, "REMINDER_WINDOW_TOO_LARGE", "Khoảng truy vấn nhắc hạn không được vượt quá 32 ngày")
        return readModels.findAllByCompletedFalseAndDueAtGreaterThanEqualAndDueAtLessThanOrderByDueAtAsc(from, to)
            .map { DueLearningReminder(it.enrollmentId, it.classId, it.courseId, it.userId, requireNotNull(it.dueAt), it.progressPercent) }
    }
}

@RestController
@RequestMapping("/internal/v1/reports/reminders")
class InternalReminderReportingController(
    private val service: ReminderReportingService,
    private val internal: InternalTokenAuthorizer,
) {
    @GetMapping("/due")
    fun due(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ): List<DueLearningReminder> {
        internal.require(token)
        return service.dueBetween(from, to)
    }
}
