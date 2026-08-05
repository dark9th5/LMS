package com.lmspilot.enrollment.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.enrollment.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.InternalTokenAuthorizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.UUID

data class EnrollmentResponse(val id: UUID, val courseId: UUID, val userId: UUID, val dueAt: Instant?, val status: EnrollmentStatus, val enrolledAt: Instant)
data class CoursePublication(val courseId: UUID, val published: Boolean, val version: Int, val status: String, val ownerId: UUID)
data class EnrollmentValidation(val enrollmentId: UUID, val classId: UUID, val courseId: UUID, val courseVersion: Int, val userId: UUID, val status: EnrollmentStatus, val dueAt: Instant?)

@Service
class CoursePublicationClient(
    builder: RestClient.Builder,
    @Value("\${course-service.url:http://localhost:8083}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client = builder.baseUrl(baseUrl).build()
    fun get(courseId: UUID): CoursePublication = client.get().uri("/internal/v1/courses/{id}/publication", courseId)
        .header("X-Service-Token", serviceToken).retrieve().body(CoursePublication::class.java)
        ?: throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "COURSE_SERVICE_UNAVAILABLE", "Không nhận được trạng thái khóa học")
}

@Service
class EnrollmentManagementService(
    private val deliveries: TrainingClassRepository,
    private val enrollments: EnrollmentRepository,
) {
    @Transactional(readOnly = true)
    fun myEnrollments(): List<EnrollmentResponse> =
        enrollments.findAllByUserIdOrderByEnrolledAtDesc(CurrentUser.id()).map { it.response() }

    /** Internal delivery ids used only to scope instructor-owned course operations. */
    @Transactional(readOnly = true)
    fun assignedDeliveryIds(userId: UUID): Set<UUID> =
        deliveries.findAllAssignedTo(userId).map { it.id }.toSet()

    @Transactional(readOnly = true)
    fun assignedCourseIds(userId: UUID): Set<UUID> =
        deliveries.findAllAssignedTo(userId).map { it.courseId }.toSet()

    @Transactional(readOnly = true)
    fun activeCourseIds(userId: UUID): Set<UUID> =
        enrollments.findAllByUserIdOrderByEnrolledAtDesc(userId)
            .filter { it.status != EnrollmentStatus.CANCELLED }
            .map { it.courseId }
            .toSet()

    @Transactional(readOnly = true)
    fun accessibleCourseVersions(userId: UUID, courseId: UUID): Set<Int> {
        val activeEnrollments = enrollments.findAllByUserIdOrderByEnrolledAtDesc(userId)
            .filter { it.courseId == courseId && it.status != EnrollmentStatus.CANCELLED }
        if (activeEnrollments.isEmpty()) return emptySet()
        val deliveryById = deliveries.findAllById(activeEnrollments.map { it.classId }.toSet()).associateBy { it.id }
        return activeEnrollments.mapNotNull { deliveryById[it.classId]?.courseVersion }.toSet()
    }

    @Transactional(readOnly = true)
    fun validateEnrollment(id: UUID): EnrollmentValidation {
        val enrollment = enrollments.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "ENROLLMENT_NOT_FOUND", "Không tìm thấy lượt học")
        }
        val delivery = deliveries.findById(enrollment.classId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "COURSE_DELIVERY_NOT_FOUND", "Không tìm thấy phạm vi phân phối khóa học")
        }
        return EnrollmentValidation(
            enrollment.id,
            enrollment.classId,
            enrollment.courseId,
            delivery.courseVersion,
            enrollment.userId,
            enrollment.status,
            enrollment.dueAt,
        )
    }

    @Transactional(readOnly = true)
    fun activeEnrollments(userId: UUID, courseId: UUID): List<EnrollmentValidation> {
        val active = enrollments.findAllByUserIdOrderByEnrolledAtDesc(userId)
            .filter { it.courseId == courseId && it.status != EnrollmentStatus.CANCELLED }
        if (active.isEmpty()) return emptyList()
        val deliveryById = deliveries.findAllById(active.map { it.classId }.toSet()).associateBy { it.id }
        return active.mapNotNull { enrollment ->
            deliveryById[enrollment.classId]?.let { delivery ->
                EnrollmentValidation(
                    enrollment.id,
                    enrollment.classId,
                    enrollment.courseId,
                    delivery.courseVersion,
                    enrollment.userId,
                    enrollment.status,
                    enrollment.dueAt,
                )
            }
        }
    }
}

private fun EnrollmentEntity.response() = EnrollmentResponse(id, courseId, userId, dueAt, status, enrolledAt)

@RestController
@RequestMapping("/api/v1/enrollments")
class EnrollmentController(private val service: EnrollmentManagementService) {
    @GetMapping("/me") @PreAuthorize("hasAuthority('${Permissions.LEARNING_READ_SELF}')") fun me() = service.myEnrollments()
}


@RestController
@RequestMapping("/internal/v1/enrollments")
class InternalEnrollmentController(
    private val service: EnrollmentManagementService,
    private val internal: InternalTokenAuthorizer,
) {
    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID, @RequestHeader("X-Service-Token", required = false) token: String?): EnrollmentValidation {
        internal.require(token)
        return service.validateEnrollment(id)
    }

    @GetMapping("/users/{userId}/courses/{courseId}")
    fun activeForCourse(
        @PathVariable userId: UUID,
        @PathVariable courseId: UUID,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ): List<EnrollmentValidation> {
        internal.require(token)
        return service.activeEnrollments(userId, courseId)
    }
}


@RestController
@RequestMapping("/internal/v1/course-access")
class InternalCourseAccessController(
    private val service: EnrollmentManagementService,
    private val internal: InternalTokenAuthorizer,
) {
    @GetMapping("/instructors/{userId}/delivery-ids")
    fun assigned(@PathVariable userId: UUID, @RequestHeader("X-Service-Token", required = false) token: String?): Set<UUID> {
        internal.require(token)
        return service.assignedDeliveryIds(userId)
    }

    @GetMapping("/instructors/{userId}/courses")
    fun assignedCourses(@PathVariable userId: UUID, @RequestHeader("X-Service-Token", required = false) token: String?): Set<UUID> {
        internal.require(token)
        return service.assignedCourseIds(userId)
    }

    @GetMapping("/users/{userId}/courses")
    fun activeCourses(@PathVariable userId: UUID, @RequestHeader("X-Service-Token", required = false) token: String?): Set<UUID> {
        internal.require(token)
        return service.activeCourseIds(userId)
    }

    @GetMapping("/users/{userId}/courses/{courseId}/versions")
    fun accessibleVersions(
        @PathVariable userId: UUID,
        @PathVariable courseId: UUID,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ): Set<Int> {
        internal.require(token)
        return service.accessibleCourseVersions(userId, courseId)
    }
}
