package com.lmspilot.reporting.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.reporting.domain.LearnerCourseReadModel
import com.lmspilot.reporting.domain.LearnerCourseReadModelRepository
import com.lmspilot.reporting.domain.ReportExportJobEntity
import com.lmspilot.reporting.domain.ReportExportJobRepository
import com.lmspilot.reporting.domain.ReportExportStatus
import com.lmspilot.reporting.domain.ReportFrequency
import com.lmspilot.reporting.domain.ReportScheduleEntity
import com.lmspilot.reporting.domain.ReportScheduleRepository
import com.lmspilot.reporting.domain.ReportScope
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.LicenseGuard
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class CreateReportExportRequest(val scope: ReportScope = ReportScope.SELF)
data class ReportExportJobResponse(val id: UUID, val scope: ReportScope, val status: ReportExportStatus, val rowCount: Int?, val errorMessage: String?, val createdAt: Instant, val completedAt: Instant?, val expiresAt: Instant)
data class ReportScheduleRequest(
    @field:NotBlank @field:Size(max = 180) val name: String,
    val scope: ReportScope = ReportScope.SELF,
    val frequency: ReportFrequency = ReportFrequency.DAILY,
    @field:Min(1) @field:Max(7) val dayOfWeek: Int? = null,
    @field:Min(0) @field:Max(23) val hourUtc: Int = 0,
    val enabled: Boolean = true,
)
data class ReportScheduleResponse(val id: UUID, val name: String, val scope: ReportScope, val frequency: ReportFrequency, val dayOfWeek: Int?, val hourUtc: Int, val enabled: Boolean, val nextRunAt: Instant, val updatedAt: Instant)

@Service
class ScheduledReportingService(
    private val jobs: ReportExportJobRepository,
    private val schedules: ReportScheduleRepository,
    private val readModels: LearnerCourseReadModelRepository,
    private val enrollmentScope: EnrollmentScopeClient,
    private val reporting: ReportingProjectionService,
    private val license: LicenseGuard,
) {
    @Transactional
    fun createExport(input: CreateReportExportRequest): ReportExportJobResponse {
        license.requireFeature("REPORT_EXPORT", write = false)
        validateScope(input.scope)
        return jobs.save(ReportExportJobEntity(ownerId = CurrentUser.id(), scope = input.scope)).response()
    }

    @Transactional(readOnly = true)
    fun myExports() = jobs.findAllByOwnerIdOrderByCreatedAtDesc(CurrentUser.id()).take(100).map { it.response() }

    @Transactional(readOnly = true)
    fun download(id: UUID): ResponseEntity<ByteArray> {
        val job = jobs.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "REPORT_EXPORT_NOT_FOUND", "Không tìm thấy file báo cáo") }
        if (job.ownerId != CurrentUser.id()) throw ApiException(HttpStatus.FORBIDDEN, "REPORT_EXPORT_SCOPE", "Không có quyền tải báo cáo này")
        if (job.status != ReportExportStatus.COMPLETED || job.outputCsv == null) throw ApiException(HttpStatus.CONFLICT, "REPORT_EXPORT_NOT_READY", "Báo cáo chưa sẵn sàng")
        if (job.expiresAt.isBefore(Instant.now())) throw ApiException(HttpStatus.GONE, "REPORT_EXPORT_EXPIRED", "File báo cáo đã hết hạn")
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=learning-report-${job.id}.csv")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(job.outputCsv!!.toByteArray(StandardCharsets.UTF_8))
    }

    @Transactional
    fun createSchedule(input: ReportScheduleRequest): ReportScheduleResponse {
        license.requireFeature("REPORT_EXPORT", write = false)
        validateSchedule(input)
        validateScope(input.scope)
        val entity = ReportScheduleEntity(
            ownerId = CurrentUser.id(),
            name = input.name.trim(),
            scope = input.scope,
            frequency = input.frequency,
            dayOfWeek = input.dayOfWeek,
            hourUtc = input.hourUtc,
            enabled = input.enabled,
            nextRunAt = nextRun(input.frequency, input.dayOfWeek, input.hourUtc, Instant.now()),
        )
        return schedules.save(entity).response()
    }

    @Transactional
    fun updateSchedule(id: UUID, input: ReportScheduleRequest): ReportScheduleResponse {
        validateSchedule(input)
        validateScope(input.scope)
        val entity = ownedSchedule(id)
        entity.name = input.name.trim(); entity.scope = input.scope; entity.frequency = input.frequency; entity.dayOfWeek = input.dayOfWeek
        entity.hourUtc = input.hourUtc; entity.enabled = input.enabled; entity.updatedAt = Instant.now()
        entity.nextRunAt = nextRun(input.frequency, input.dayOfWeek, input.hourUtc, Instant.now())
        return entity.response()
    }

    @Transactional(readOnly = true)
    fun mySchedules() = schedules.findAllByOwnerIdOrderByCreatedAtDesc(CurrentUser.id()).map { it.response() }

    @Transactional
    fun deleteSchedule(id: UUID) {
        schedules.delete(ownedSchedule(id))
    }

    @Scheduled(fixedDelayString = "\${reporting.export-worker-delay-ms:5000}")
    @Transactional
    fun enqueueSchedules() {
        val now = Instant.now()
        schedules.findTop50ByEnabledTrueAndNextRunAtBeforeOrderByNextRunAtAsc(now).forEach { schedule ->
            val periodKey = schedule.nextRunAt.truncatedTo(ChronoUnit.HOURS).toString()
            if (jobs.findByScheduleIdAndPeriodKey(schedule.id, periodKey) == null) {
                jobs.save(ReportExportJobEntity(ownerId = schedule.ownerId, scope = schedule.scope, scheduleId = schedule.id, periodKey = periodKey))
            }
            schedule.nextRunAt = nextRun(schedule.frequency, schedule.dayOfWeek, schedule.hourUtc, now.plusSeconds(60))
            schedule.updatedAt = now
        }
    }

    @Scheduled(fixedDelayString = "\${reporting.export-worker-delay-ms:5000}")
    @Transactional
    fun processExports() {
        jobs.findTop20ByStatusOrderByCreatedAtAsc(ReportExportStatus.PENDING).forEach { job ->
            job.status = ReportExportStatus.PROCESSING
            job.startedAt = Instant.now()
            runCatching {
                val rows = rowsFor(job.ownerId, job.scope).map { LearnerCourseRow(it.enrollmentId, it.classId, it.courseId, it.userId, it.progressPercent, it.completed, it.dueAt, it.lastScore, it.passed, it.updatedAt) }
                val csv = reporting.exportCsv(rows).toString(StandardCharsets.UTF_8)
                job.outputCsv = csv
                job.rowCount = rows.size
                job.status = ReportExportStatus.COMPLETED
                job.completedAt = Instant.now()
            }.onFailure { error ->
                job.status = ReportExportStatus.FAILED
                job.errorMessage = (error.message ?: error.javaClass.simpleName).take(1000)
                job.completedAt = Instant.now()
            }
        }
        jobs.findTop20ByStatusOrderByCreatedAtAsc(ReportExportStatus.COMPLETED)
            .filter { it.expiresAt.isBefore(Instant.now()) }
            .forEach { it.status = ReportExportStatus.EXPIRED; it.outputCsv = null }
    }

    private fun rowsFor(ownerId: UUID, scope: ReportScope): List<LearnerCourseReadModel> = when (scope) {
        ReportScope.SELF -> readModels.findAllByUserId(ownerId)
        ReportScope.ASSIGNED -> {
            val classIds = enrollmentScope.assignedDeliveryIds(ownerId)
            readModels.findAll().filter { it.classId in classIds }
        }
        ReportScope.SYSTEM -> readModels.findAll()
    }

    private fun validateScope(scope: ReportScope) {
        if (scope == ReportScope.SYSTEM && !CurrentUser.hasRole("ADMIN")) throw ApiException(HttpStatus.FORBIDDEN, "REPORT_SCOPE_DENIED", "Chỉ quản trị hệ thống được xuất báo cáo toàn hệ thống")
        if (scope == ReportScope.ASSIGNED && Permissions.REPORTS_READ !in CurrentUser.authorities()) throw ApiException(HttpStatus.FORBIDDEN, "REPORT_SCOPE_DENIED", "Không có quyền báo cáo theo phạm vi được giao")
    }

    private fun validateSchedule(input: ReportScheduleRequest) {
        if (input.frequency == ReportFrequency.WEEKLY && input.dayOfWeek == null) throw ApiException(HttpStatus.BAD_REQUEST, "REPORT_DAY_REQUIRED", "Lịch tuần cần chọn ngày trong tuần")
    }

    private fun ownedSchedule(id: UUID): ReportScheduleEntity {
        val entity = schedules.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "REPORT_SCHEDULE_NOT_FOUND", "Không tìm thấy lịch báo cáo") }
        if (entity.ownerId != CurrentUser.id()) throw ApiException(HttpStatus.FORBIDDEN, "REPORT_SCHEDULE_SCOPE", "Không có quyền sửa lịch này")
        return entity
    }

    private fun nextRun(frequency: ReportFrequency, dayOfWeek: Int?, hourUtc: Int, from: Instant): Instant {
        var next = ZonedDateTime.ofInstant(from, ZoneOffset.UTC).withMinute(0).withSecond(0).withNano(0).withHour(hourUtc)
        if (!next.toInstant().isAfter(from)) next = next.plusDays(1)
        if (frequency == ReportFrequency.WEEKLY) {
            val target = DayOfWeek.of(dayOfWeek ?: 1)
            while (next.dayOfWeek != target) next = next.plusDays(1)
        }
        return next.toInstant()
    }
}

private fun ReportExportJobEntity.response() = ReportExportJobResponse(id, scope, status, rowCount, errorMessage, createdAt, completedAt, expiresAt)
private fun ReportScheduleEntity.response() = ReportScheduleResponse(id, name, scope, frequency, dayOfWeek, hourUtc, enabled, nextRunAt, updatedAt)

@RestController
@RequestMapping("/api/v1/reports")
class ScheduledReportingController(private val service: ScheduledReportingService) {
    @PostMapping("/exports") @PreAuthorize("hasAuthority('${Permissions.REPORTS_EXPORT}')") fun createExport(@RequestBody input: CreateReportExportRequest) = service.createExport(input)
    @GetMapping("/exports") @PreAuthorize("hasAuthority('${Permissions.REPORTS_EXPORT}')") fun exports() = service.myExports()
    @GetMapping("/exports/{id}/download") @PreAuthorize("hasAuthority('${Permissions.REPORTS_EXPORT}')") fun download(@PathVariable id: UUID) = service.download(id)
    @PostMapping("/schedules") @PreAuthorize("hasAuthority('${Permissions.REPORTS_SCHEDULE}')") fun createSchedule(@Valid @RequestBody input: ReportScheduleRequest) = service.createSchedule(input)
    @GetMapping("/schedules") @PreAuthorize("hasAuthority('${Permissions.REPORTS_SCHEDULE}')") fun schedules() = service.mySchedules()
    @PutMapping("/schedules/{id}") @PreAuthorize("hasAuthority('${Permissions.REPORTS_SCHEDULE}')") fun updateSchedule(@PathVariable id: UUID, @Valid @RequestBody input: ReportScheduleRequest) = service.updateSchedule(id, input)
    @DeleteMapping("/schedules/{id}") @PreAuthorize("hasAuthority('${Permissions.REPORTS_SCHEDULE}')") fun deleteSchedule(@PathVariable id: UUID) = service.deleteSchedule(id)
}
