package com.lmspilot.reporting.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.reporting.domain.LearnerCourseReadModel
import com.lmspilot.reporting.domain.LearnerCourseReadModelRepository
import com.lmspilot.reporting.domain.ReportScope
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.round

data class LearningKpiResponse(
    val scope: ReportScope,
    val courseId: UUID?,
    val totalEnrollments: Int,
    val notStarted: Int,
    val inProgress: Int,
    val completed: Int,
    val overdue: Int,
    val dueSoon: Int,
    val passed: Int,
    val failed: Int,
    val activeLast30Days: Int,
    val completionRate: Double,
    val passRate: Double,
    val averageProgress: Double,
    val averageScore: Double?,
    val generatedAt: Instant,
)

data class CourseKpiRow(
    val courseId: UUID,
    val totalEnrollments: Int,
    val completed: Int,
    val overdue: Int,
    val completionRate: Double,
    val passRate: Double,
    val averageProgress: Double,
    val averageScore: Double?,
    val lastActivityAt: Instant?,
)

@Service
class KpiReportingService(
    private val readModels: LearnerCourseReadModelRepository,
    private val enrollmentScope: EnrollmentScopeClient,
) {
    @Transactional(readOnly = true)
    fun summary(scope: ReportScope, courseId: UUID?): LearningKpiResponse {
        val rows = rows(scope).filter { courseId == null || it.courseId == courseId }
        return aggregate(scope, courseId, rows)
    }

    @Transactional(readOnly = true)
    fun byCourse(scope: ReportScope): List<CourseKpiRow> = rows(scope)
        .groupBy { it.courseId }
        .map { (courseId, courseRows) ->
            val summary = aggregate(scope, courseId, courseRows)
            CourseKpiRow(
                courseId = courseId,
                totalEnrollments = summary.totalEnrollments,
                completed = summary.completed,
                overdue = summary.overdue,
                completionRate = summary.completionRate,
                passRate = summary.passRate,
                averageProgress = summary.averageProgress,
                averageScore = summary.averageScore,
                lastActivityAt = courseRows.mapNotNull { it.lastActivityAt }.maxOrNull(),
            )
        }
        .sortedWith(compareByDescending<CourseKpiRow> { it.overdue }.thenByDescending { it.totalEnrollments }.thenBy { it.courseId })

    private fun rows(scope: ReportScope): List<LearnerCourseReadModel> {
        validateScope(scope)
        return when (scope) {
            ReportScope.SELF -> readModels.findAllByUserId(CurrentUser.id())
            ReportScope.ASSIGNED -> {
                val classIds = enrollmentScope.assignedClassIds(CurrentUser.id())
                if (classIds.isEmpty()) emptyList() else readModels.findAllByClassIdIn(classIds)
            }
            ReportScope.SYSTEM -> readModels.findAll()
        }
    }

    private fun aggregate(scope: ReportScope, courseId: UUID?, rows: List<LearnerCourseReadModel>): LearningKpiResponse {
        val now = Instant.now()
        val activeSince = now.minus(30, ChronoUnit.DAYS)
        val dueSoonUntil = now.plus(7, ChronoUnit.DAYS)
        val completed = rows.count { it.completed }
        val overdue = rows.count { !it.completed && it.dueAt?.isBefore(now) == true }
        val inProgress = rows.count { !it.completed && it.progressPercent > 0 && it.dueAt?.isBefore(now) != true }
        val notStarted = rows.size - completed - overdue - inProgress
        val dueSoon = rows.count { !it.completed && it.dueAt?.let { due -> !due.isBefore(now) && !due.isAfter(dueSoonUntil) } == true }
        val graded = rows.filter { it.passed != null }
        val passed = graded.count { it.passed == true }
        val failed = graded.count { it.passed == false }
        val scores = rows.mapNotNull { it.lastScore }
        return LearningKpiResponse(
            scope = scope,
            courseId = courseId,
            totalEnrollments = rows.size,
            notStarted = notStarted,
            inProgress = inProgress,
            completed = completed,
            overdue = overdue,
            dueSoon = dueSoon,
            passed = passed,
            failed = failed,
            activeLast30Days = rows.count { it.lastActivityAt?.isBefore(activeSince) == false },
            completionRate = percentage(completed, rows.size),
            passRate = percentage(passed, graded.size),
            averageProgress = rounded(rows.map { it.progressPercent }.averageOrZero()),
            averageScore = scores.takeIf { it.isNotEmpty() }?.average()?.let(::rounded),
            generatedAt = now,
        )
    }

    private fun validateScope(scope: ReportScope) {
        if (scope == ReportScope.SYSTEM && !CurrentUser.isSystemAdmin() &&
            Permissions.REPORTS_KPI_READ !in CurrentUser.globalAuthorities() && Permissions.REPORTS_READ_SCOPE !in CurrentUser.globalAuthorities()) {
            throw ApiException(HttpStatus.FORBIDDEN, "REPORT_SCOPE_DENIED", "Chỉ quản trị hệ thống được xem KPI toàn hệ thống")
        }
        if (scope == ReportScope.ASSIGNED && Permissions.REPORTS_KPI_READ !in CurrentUser.authorities() && Permissions.REPORTS_READ_SCOPE !in CurrentUser.authorities() && Permissions.REPORTS_READ !in CurrentUser.authorities()) {
            throw ApiException(HttpStatus.FORBIDDEN, "REPORT_SCOPE_DENIED", "Không có quyền xem KPI theo phạm vi được giao")
        }
    }

    private fun percentage(part: Int, total: Int): Double = if (total == 0) 0.0 else rounded(part * 100.0 / total)
    private fun rounded(value: Double): Double = round(value * 100.0) / 100.0
    private fun Iterable<Int>.averageOrZero(): Double = if (none()) 0.0 else average()
}

@RestController
@RequestMapping("/api/v1/reports/kpis")
class KpiReportingController(private val service: KpiReportingService) {
    @GetMapping
    @PreAuthorize("hasAnyAuthority('${Permissions.REPORTS_KPI_READ}','${Permissions.REPORTS_READ_SELF}','${Permissions.REPORTS_READ_SCOPE}','${Permissions.REPORTS_READ}')")
    fun summary(
        @RequestParam(defaultValue = "SELF") scope: ReportScope,
        @RequestParam(required = false) courseId: UUID?,
    ) = service.summary(scope, courseId)

    @GetMapping("/courses")
    @PreAuthorize("hasAnyAuthority('${Permissions.REPORTS_KPI_READ}','${Permissions.REPORTS_READ_SCOPE}','${Permissions.REPORTS_READ}')")
    fun courses(@RequestParam(defaultValue = "ASSIGNED") scope: ReportScope) = service.byCourse(scope)
}
