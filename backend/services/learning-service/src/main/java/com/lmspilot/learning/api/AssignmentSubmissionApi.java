package com.lmspilot.learning.api;

import com.lmspilot.learning.domain.AssignmentSubmissionEntity;
import com.lmspilot.learning.domain.AssignmentSubmissionRepository;
import com.lmspilot.learning.domain.AssignmentSubmissionStatus;
import com.lmspilot.support.api.ApiException;
import com.lmspilot.support.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

record SubmitAssignmentRequest(
    @NotNull UUID enrollmentId,
    @NotNull UUID fileId,
    @Size(max = 5000) String comment
) {}

record GradeAssignmentRequest(
    @NotNull @DecimalMin("0") Double score,
    @NotNull @DecimalMin("0.01") Double maxScore,
    @Size(max = 10000) String feedback,
    Boolean returnForRevision
) {}

record AssignmentSubmissionResponse(
    UUID id,
    UUID enrollmentId,
    UUID courseId,
    int courseVersion,
    UUID lessonId,
    UUID userId,
    int attemptNumber,
    UUID fileId,
    String comment,
    Instant submittedAt,
    boolean late,
    AssignmentSubmissionStatus status,
    Double score,
    Double maxScore,
    String feedback,
    UUID gradedBy,
    Instant gradedAt
) {}

record AssignmentEnrollment(
    UUID enrollmentId,
    UUID classId,
    UUID courseId,
    int courseVersion,
    UUID userId,
    String status,
    Instant dueAt
) {}

record AssignmentFile(
    UUID id,
    UUID ownerId,
    String originalName,
    String contentType,
    long sizeBytes,
    String sha256,
    String purpose,
    String status,
    Instant createdAt
) {}

@Service
@Transactional
class AssignmentSubmissionService {
    private final AssignmentSubmissionRepository repository;
    private final LearningProgressService learningProgress;
    private final RestClient enrollmentClient;
    private final RestClient fileClient;
    private final String internalToken;
    private final int maxAttempts;

    AssignmentSubmissionService(
        AssignmentSubmissionRepository repository,
        LearningProgressService learningProgress,
        @Value("${enrollment-service.url:http://localhost:8084}") String enrollmentServiceUrl,
        @Value("${file-storage-service.url:http://localhost:8089}") String fileStorageServiceUrl,
        @Value("${lmspilot.internal-token}") String internalToken,
        @Value("${learning.assignment-max-attempts:10}") int maxAttempts
    ) {
        this.repository = repository;
        this.learningProgress = learningProgress;
        this.enrollmentClient = client(enrollmentServiceUrl);
        this.fileClient = client(fileStorageServiceUrl);
        this.internalToken = internalToken;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Transactional(readOnly = true)
    List<AssignmentSubmissionResponse> mine() {
        return repository.findAllByUserIdOrderBySubmittedAtDesc(user()).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    List<AssignmentSubmissionResponse> attempts(UUID enrollmentId, UUID lessonId) {
        UUID current = user();
        AssignmentEnrollment enrollment = requireEnrollment(enrollmentId);
        if (!Objects.equals(enrollment.userId(), current)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ASSIGNMENT_OUT_OF_SCOPE", "Không được xem bài nộp của người khác");
        }
        return repository.findAllByEnrollmentIdAndLessonIdOrderByAttemptNumberDesc(enrollmentId, lessonId)
            .stream()
            .filter(item -> Objects.equals(item.userId, current))
            .map(this::view)
            .toList();
    }

    AssignmentSubmissionResponse submit(UUID lessonId, SubmitAssignmentRequest input, String idempotencyKey) {
        String key = requireIdempotencyKey(idempotencyKey);
        AssignmentSubmissionEntity duplicate = repository.findByIdempotencyKey(key).orElse(null);
        if (duplicate != null) {
            if (!Objects.equals(duplicate.userId, user())) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "Khóa chống gửi trùng đã được sử dụng");
            }
            return view(duplicate);
        }

        AssignmentEnrollment enrollment = requireEnrollment(input.enrollmentId());
        UUID current = user();
        if (!Objects.equals(enrollment.userId(), current)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ASSIGNMENT_OUT_OF_SCOPE", "Không được nộp bài thay người khác");
        }
        if (enrollment.status() != null && "CANCELLED".equalsIgnoreCase(enrollment.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "ENROLLMENT_INACTIVE", "Lượt ghi danh đã bị hủy");
        }

        long existingAttempts = repository.countByEnrollmentIdAndLessonId(input.enrollmentId(), lessonId);
        if (existingAttempts >= maxAttempts) {
            throw new ApiException(HttpStatus.CONFLICT, "ASSIGNMENT_ATTEMPT_LIMIT", "Đã đạt số lần nộp bài tối đa");
        }
        AssignmentFile file = requireFile(input.fileId());
        if (!Objects.equals(file.ownerId(), current)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ASSIGNMENT_FILE_FORBIDDEN", "Tệp bài làm không thuộc tài khoản hiện tại");
        }
        if (!"AVAILABLE".equalsIgnoreCase(file.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "ASSIGNMENT_FILE_UNAVAILABLE", "Tệp bài làm không còn khả dụng");
        }
        if (!"ASSIGNMENT_SUBMISSION".equalsIgnoreCase(file.purpose())) {
            throw new ApiException(HttpStatus.CONFLICT, "ASSIGNMENT_FILE_PURPOSE_INVALID", "Tệp không được tải lên với mục đích nộp bài");
        }

        Instant now = Instant.now();
        AssignmentSubmissionEntity entity = new AssignmentSubmissionEntity();
        entity.enrollmentId = enrollment.enrollmentId();
        entity.classId = enrollment.classId() == null ? enrollment.courseId() : enrollment.classId();
        entity.courseId = enrollment.courseId();
        entity.courseVersion = Math.max(1, enrollment.courseVersion());
        entity.lessonId = lessonId;
        entity.userId = current;
        entity.attemptNumber = Math.toIntExact(existingAttempts + 1);
        entity.fileId = input.fileId();
        entity.comment = normalize(input.comment());
        entity.submittedAt = now;
        entity.late = enrollment.dueAt() != null && now.isAfter(enrollment.dueAt());
        entity.status = AssignmentSubmissionStatus.SUBMITTED;
        entity.idempotencyKey = key;
        entity.updatedAt = now;
        return view(repository.save(entity));
    }

    @Transactional(readOnly = true)
    List<AssignmentSubmissionResponse> queueByCourses(Set<UUID> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) return List.of();
        return repository.findAllByCourseIdInAndStatusOrderBySubmittedAtAsc(courseIds, AssignmentSubmissionStatus.SUBMITTED)
            .stream()
            .map(this::view)
            .toList();
    }

    AssignmentSubmissionResponse grade(UUID id, GradeAssignmentRequest input) {
        if (input.score() > input.maxScore()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ASSIGNMENT_SCORE_INVALID", "Điểm không được lớn hơn điểm tối đa");
        }
        AssignmentSubmissionEntity entity = repository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUBMISSION_NOT_FOUND", "Không tìm thấy bài nộp"));
        entity.score = input.score();
        entity.maxScore = input.maxScore();
        entity.feedback = normalize(input.feedback());
        entity.gradedBy = user();
        entity.gradedAt = Instant.now();
        entity.updatedAt = entity.gradedAt;
        entity.status = Boolean.TRUE.equals(input.returnForRevision())
            ? AssignmentSubmissionStatus.RETURNED
            : AssignmentSubmissionStatus.GRADED;
        AssignmentSubmissionEntity saved = repository.save(entity);
        if (saved.status == AssignmentSubmissionStatus.GRADED) {
            learningProgress.completeGradedAssignmentLesson(new AssignmentSubmissionEntityView(
                saved.id,
                saved.enrollmentId,
                saved.courseId,
                saved.lessonId,
                saved.userId
            ));
        }
        return view(saved);
    }

    private AssignmentEnrollment requireEnrollment(UUID enrollmentId) {
        try {
            AssignmentEnrollment result = enrollmentClient.get()
                .uri("/internal/v1/enrollments/{id}", enrollmentId)
                .header("X-Service-Token", internalToken)
                .retrieve()
                .body(AssignmentEnrollment.class);
            if (result == null) throw new IllegalStateException("empty response");
            return result;
        } catch (RestClientException | IllegalStateException error) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ENROLLMENT_SERVICE_UNAVAILABLE", "Không thể xác minh lượt ghi danh lúc này");
        }
    }

    private AssignmentFile requireFile(UUID fileId) {
        try {
            AssignmentFile result = fileClient.get()
                .uri("/internal/v1/files/{id}", fileId)
                .header("X-Service-Token", internalToken)
                .retrieve()
                .body(AssignmentFile.class);
            if (result == null) throw new IllegalStateException("empty response");
            return result;
        } catch (RestClientException | IllegalStateException error) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "FILE_SERVICE_UNAVAILABLE", "Không thể xác minh tệp bài làm lúc này");
        }
    }

    private RestClient client(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    private String requireIdempotencyKey(String value) {
        String key = value == null ? "" : value.trim();
        if (key.isEmpty() || key.length() > 160) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key là bắt buộc và tối đa 160 ký tự");
        }
        return key;
    }

    private String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private AssignmentSubmissionResponse view(AssignmentSubmissionEntity entity) {
        return new AssignmentSubmissionResponse(
            entity.id,
            entity.enrollmentId,
            entity.courseId,
            entity.courseVersion,
            entity.lessonId,
            entity.userId,
            entity.attemptNumber,
            entity.fileId,
            entity.comment,
            entity.submittedAt,
            entity.late,
            entity.status,
            entity.score,
            entity.maxScore,
            entity.feedback,
            entity.gradedBy,
            entity.gradedAt
        );
    }

    private UUID user() {
        try {
            return CurrentUser.id();
        } catch (Exception error) {
            return new UUID(0, 1);
        }
    }
}

@RestController
@RequestMapping("/api/v1/learning/assignments")
public class AssignmentSubmissionApi {
    private final AssignmentSubmissionService service;

    public AssignmentSubmissionApi(AssignmentSubmissionService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public List<AssignmentSubmissionResponse> mine() {
        return service.mine();
    }

    @GetMapping("/{lessonId}/attempts")
    public List<AssignmentSubmissionResponse> attempts(
        @PathVariable UUID lessonId,
        @RequestParam UUID enrollmentId
    ) {
        return service.attempts(enrollmentId, lessonId);
    }

    @PostMapping("/{lessonId}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentSubmissionResponse submit(
        @PathVariable UUID lessonId,
        @Valid @RequestBody SubmitAssignmentRequest input,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return service.submit(lessonId, input, idempotencyKey);
    }

    @GetMapping("/queue-by-course")
    public List<AssignmentSubmissionResponse> queue(@RequestParam Set<UUID> courseId) {
        return service.queueByCourses(courseId);
    }

    @PutMapping("/submissions/{id}/grade")
    public AssignmentSubmissionResponse grade(
        @PathVariable UUID id,
        @Valid @RequestBody GradeAssignmentRequest input
    ) {
        return service.grade(id, input);
    }
}
