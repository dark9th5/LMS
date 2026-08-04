package com.lmspilot.learning.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.learning.domain.AssignmentSubmissionEntity
import com.lmspilot.learning.domain.AssignmentSubmissionRepository
import com.lmspilot.learning.domain.AssignmentSubmissionStatus
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

/** Dedicated assignment-submission aggregate; file metadata remains owned by file-storage-service. */
data class SubmitAssignmentRequest(
    val enrollmentId: UUID,
    val fileId: UUID,
    @field:Size(max = 5000) val comment: String? = null,
)

data class GradeAssignmentRequest(
    @field:DecimalMin("0.0") val score: Double,
    @field:DecimalMin("0.000001") val maxScore: Double,
    @field:Size(max = 10000) val feedback: String? = null,
    val returnForRevision: Boolean = false,
)

data class AssignmentSubmissionResponse(
    val id: UUID,
    val enrollmentId: UUID,
    val classId: UUID,
    val courseId: UUID,
    val courseVersion: Int,
    val lessonId: UUID,
    val userId: UUID,
    val attemptNumber: Int,
    val fileId: UUID,
    val comment: String?,
    val submittedAt: Instant,
    val late: Boolean,
    val status: AssignmentSubmissionStatus,
    val score: Double?,
    val maxScore: Double?,
    val feedback: String?,
    val gradedBy: UUID?,
    val gradedAt: Instant?,
)


data class AssignmentFileMetadata(
    val id: UUID,
    val ownerId: UUID,
    val purpose: String,
    val status: String,
)

@Service
class AssignmentFileClient(
    builder: RestClient.Builder,
    @Value("\${file-storage-service.url:http://localhost:8089}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client = builder.baseUrl(baseUrl).build()

    fun metadata(fileId: UUID): AssignmentFileMetadata = client.get()
        .uri("/internal/v1/files/{id}", fileId)
        .header("X-Service-Token", serviceToken)
        .retrieve()
        .body(AssignmentFileMetadata::class.java)
        ?: throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "FILE_SERVICE_UNAVAILABLE", "Không nhận được metadata tệp bài làm")
}

@Service
class AssignmentSubmissionService(
    private val repository: AssignmentSubmissionRepository,
    private val enrollments: EnrollmentValidationClient,
    private val courses: CourseLearningClient,
    private val files: AssignmentFileClient,
    private val fileAccess: FileAccessClient,
    private val progress: LearningProgressService,
    @Value("\${learning.assignment-max-attempts:10}") private val maximumAttempts: Int,
) {
    @Transactional
    fun submit(lessonId: UUID, input: SubmitAssignmentRequest, idempotencyKey: String): AssignmentSubmissionResponse {
        if (idempotencyKey.isBlank() || idempotencyKey.length > 160) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key không hợp lệ")
        }
        val enrollment = enrollments.get(input.enrollmentId)
        if (enrollment.userId != CurrentUser.id()) {
            throw ApiException(HttpStatus.FORBIDDEN, "ENROLLMENT_OWNER_MISMATCH", "Không thể nộp bài cho ghi danh của người khác")
        }
        if (enrollment.status == "CANCELLED") {
            throw ApiException(HttpStatus.CONFLICT, "ENROLLMENT_INACTIVE", "Ghi danh đã bị hủy")
        }
        val metadata = courses.get(enrollment.courseId, enrollment.courseVersion)
        if (metadata.lessonTypes[lessonId] != "ASSIGNMENT") {
            throw ApiException(HttpStatus.BAD_REQUEST, "NOT_ASSIGNMENT_LESSON", "Bài học không phải bài thực hành")
        }
        val file = files.metadata(input.fileId)
        if (file.ownerId != CurrentUser.id()) {
            throw ApiException(HttpStatus.FORBIDDEN, "ASSIGNMENT_FILE_OWNER_MISMATCH", "Tệp bài làm không thuộc người dùng hiện tại")
        }
        if (file.status != "AVAILABLE" || file.purpose != "ASSIGNMENT_SUBMISSION") {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_ASSIGNMENT_FILE", "Tệp không được tải lên cho mục đích nộp bài")
        }

        val scopedKey = digest("${CurrentUser.id()}|${input.enrollmentId}|$lessonId|$idempotencyKey")
        repository.lockAttemptSequence("${input.enrollmentId}|$lessonId")
        repository.findByIdempotencyKey(scopedKey)?.let { existing ->
            val accepted = repository.findAllByEnrollmentIdAndLessonIdOrderByAttemptNumberDesc(existing.enrollmentId, existing.lessonId)
                .firstOrNull { it.status == AssignmentSubmissionStatus.GRADED }
            progress.recordVerifiedOutcome(
                enrollmentId = existing.enrollmentId,
                courseId = existing.courseId,
                lessonId = existing.lessonId,
                userId = existing.userId,
                completed = accepted != null,
                expectedLessonType = "ASSIGNMENT",
                position = "assignment-submission:${accepted?.id ?: existing.id}",
            )
            return existing.response()
        }
        val previousAttempts = repository.findAllByEnrollmentIdAndLessonIdOrderByAttemptNumberDesc(input.enrollmentId, lessonId)
        when (previousAttempts.firstOrNull()?.status) {
            AssignmentSubmissionStatus.GRADED ->
                throw ApiException(HttpStatus.CONFLICT, "ASSIGNMENT_ALREADY_GRADED", "Bài thực hành đã được chấm hoàn tất")
            AssignmentSubmissionStatus.SUBMITTED ->
                throw ApiException(HttpStatus.CONFLICT, "ASSIGNMENT_PENDING_GRADE", "Lần nộp hiện tại đang chờ chấm")
            else -> Unit
        }
        val existingAttempts = previousAttempts.size
        if (maximumAttempts > 0 && existingAttempts >= maximumAttempts) {
            throw ApiException(HttpStatus.CONFLICT, "ASSIGNMENT_ATTEMPT_LIMIT", "Đã sử dụng hết số lần nộp bài")
        }
        val now = Instant.now()
        val entity = AssignmentSubmissionEntity(
            enrollmentId = enrollment.enrollmentId,
            classId = enrollment.classId,
            courseId = enrollment.courseId,
            courseVersion = enrollment.courseVersion,
            lessonId = lessonId,
            userId = enrollment.userId,
            attemptNumber = existingAttempts + 1,
            fileId = input.fileId,
            comment = input.comment?.trim()?.takeIf(String::isNotBlank),
            submittedAt = now,
            late = enrollment.dueAt?.let(now::isAfter) ?: false,
            idempotencyKey = scopedKey,
        )
        val saved = repository.save(entity)
        progress.recordVerifiedOutcome(
            enrollmentId = saved.enrollmentId,
            courseId = saved.courseId,
            lessonId = saved.lessonId,
            userId = saved.userId,
            completed = false,
            expectedLessonType = "ASSIGNMENT",
            position = "assignment-submission:${saved.id}",
        )
        return saved.response()
    }

    @Transactional(readOnly = true)
    fun mine(): List<AssignmentSubmissionResponse> =
        repository.findAllByUserIdOrderBySubmittedAtDesc(CurrentUser.id()).map { it.response() }

    @Transactional(readOnly = true)
    fun attempts(enrollmentId: UUID, lessonId: UUID): List<AssignmentSubmissionResponse> {
        val enrollment = enrollments.get(enrollmentId)
        if (enrollment.userId != CurrentUser.id() && !canManageClass(enrollment.classId)) {
            throw ApiException(HttpStatus.FORBIDDEN, "ASSIGNMENT_OUT_OF_SCOPE", "Bài nộp ngoài phạm vi")
        }
        val items = repository.findAllByEnrollmentIdAndLessonIdOrderByAttemptNumberDesc(enrollmentId, lessonId)
        if (enrollment.userId != CurrentUser.id()) {
            fileAccess.grant(CurrentUser.id(), items.map { it.fileId }.toSet(), "ASSIGNMENT_REVIEW:$enrollmentId")
        }
        return items.map { it.response() }
    }

    @Transactional(readOnly = true)
    fun queue(classId: UUID): List<AssignmentSubmissionResponse> {
        requireClassScope(classId)
        val items = repository.findAllByClassIdOrderBySubmittedAtDesc(classId).filter { it.status == AssignmentSubmissionStatus.SUBMITTED }
        fileAccess.grant(CurrentUser.id(), items.map { it.fileId }.toSet(), "ASSIGNMENT_QUEUE:$classId")
        return items.map { it.response() }
    }

    @Transactional(readOnly = true)
    fun queue(classIds: Set<UUID>): List<AssignmentSubmissionResponse> {
        if (classIds.size > 100) {
            throw ApiException(HttpStatus.BAD_REQUEST, "ASSIGNMENT_QUEUE_CLASS_LIMIT", "Mỗi lần chỉ tải tối đa 100 lớp")
        }
        classIds.forEach(::requireClassScope)
        val items = if (classIds.isEmpty()) emptyList() else repository.findAllByClassIdInOrderBySubmittedAtDesc(classIds).filter { it.status == AssignmentSubmissionStatus.SUBMITTED }
        fileAccess.grant(CurrentUser.id(), items.map { it.fileId }.toSet(), "ASSIGNMENT_QUEUE_MULTI")
        return items.map { it.response() }
    }

    @Transactional
    fun grade(id: UUID, input: GradeAssignmentRequest): AssignmentSubmissionResponse {
        if (input.score > input.maxScore) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_ASSIGNMENT_SCORE", "Điểm không được vượt quá điểm tối đa")
        }
        val entity = repository.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "ASSIGNMENT_SUBMISSION_NOT_FOUND", "Không tìm thấy bài nộp")
        }
        requireClassScope(entity.classId)
        val latest = repository.findAllByEnrollmentIdAndLessonIdOrderByAttemptNumberDesc(entity.enrollmentId, entity.lessonId).firstOrNull()
        if (latest?.id != entity.id) {
            throw ApiException(HttpStatus.CONFLICT, "ASSIGNMENT_SUPERSEDED", "Chỉ có thể chấm lần nộp mới nhất")
        }
        if (entity.status != AssignmentSubmissionStatus.SUBMITTED) {
            throw ApiException(HttpStatus.CONFLICT, "ASSIGNMENT_NOT_PENDING", "Bài nộp không còn ở trạng thái chờ chấm")
        }
        entity.score = input.score
        entity.maxScore = input.maxScore
        entity.feedback = input.feedback?.trim()?.takeIf(String::isNotBlank)
        entity.status = if (input.returnForRevision) AssignmentSubmissionStatus.RETURNED else AssignmentSubmissionStatus.GRADED
        entity.gradedBy = CurrentUser.id()
        entity.gradedAt = Instant.now()
        entity.updatedAt = Instant.now()
        progress.recordVerifiedOutcome(
            enrollmentId = entity.enrollmentId,
            courseId = entity.courseId,
            lessonId = entity.lessonId,
            userId = entity.userId,
            completed = !input.returnForRevision,
            expectedLessonType = "ASSIGNMENT",
            position = "assignment-grade:${entity.id}:${entity.status}",
        )
        return entity.response()
    }

    private fun requireClassScope(classId: UUID) {
        if (!canManageClass(classId)) {
            throw ApiException(HttpStatus.FORBIDDEN, "ASSIGNMENT_CLASS_OUT_OF_SCOPE", "Lớp ngoài phạm vi được phân công")
        }
    }

    private fun canManageClass(classId: UUID): Boolean = CurrentUser.isSystemAdmin() ||
        Permissions.ASSESSMENTS_GRADE in CurrentUser.authorities() ||
        Permissions.GRADING_MANAGE in CurrentUser.authorities() ||
        classId in enrollments.assignedClassIds(CurrentUser.id())

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

private fun AssignmentSubmissionEntity.response() = AssignmentSubmissionResponse(
    id, enrollmentId, classId, courseId, courseVersion, lessonId, userId, attemptNumber, fileId,
    comment, submittedAt, late, status, score, maxScore, feedback, gradedBy, gradedAt,
)

@RestController
@RequestMapping("/api/v1/learning/assignments")
class AssignmentSubmissionController(private val service: AssignmentSubmissionService) {
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_READ_SELF}')")
    fun mine() = service.mine()

    @GetMapping("/{lessonId}/attempts")
    @PreAuthorize("hasAnyAuthority('${Permissions.LEARNING_READ_SELF}','${Permissions.LEARNING_READ_SCOPE}','${Permissions.ASSESSMENTS_GRADE}')")
    fun attempts(@PathVariable lessonId: UUID, @RequestParam enrollmentId: UUID) = service.attempts(enrollmentId, lessonId)

    @PostMapping("/{lessonId}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.LEARNING_WRITE_SELF}')")
    fun submit(
        @PathVariable lessonId: UUID,
        @Valid @RequestBody input: SubmitAssignmentRequest,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
    ) = service.submit(lessonId, input, idempotencyKey)

    @GetMapping("/queue/{classId}")
    @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_GRADE}','${Permissions.GRADING_MANAGE}')")
    fun queue(@PathVariable classId: UUID) = service.queue(classId)

    @GetMapping("/queue")
    @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_GRADE}','${Permissions.GRADING_MANAGE}')")
    fun queueMany(@RequestParam classId: Set<UUID>) = service.queue(classId)

    @PutMapping("/submissions/{id}/grade")
    @PreAuthorize("hasAnyAuthority('${Permissions.ASSESSMENTS_GRADE}','${Permissions.GRADING_MANAGE}')")
    fun grade(@PathVariable id: UUID, @Valid @RequestBody input: GradeAssignmentRequest) = service.grade(id, input)
}
