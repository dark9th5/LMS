package com.lmspilot.learning.api;

import com.lmspilot.contracts.CourseCompletedPayload;
import com.lmspilot.contracts.EventTypes;
import com.lmspilot.contracts.LessonCompletedPayload;
import com.lmspilot.learning.domain.CourseProgressEntity;
import com.lmspilot.learning.domain.CourseProgressRepository;
import com.lmspilot.learning.domain.IdempotencyRecordEntity;
import com.lmspilot.learning.domain.IdempotencyRecordRepository;
import com.lmspilot.learning.domain.LearningStatus;
import com.lmspilot.learning.domain.LessonProgressEntity;
import com.lmspilot.learning.domain.LessonProgressRepository;
import com.lmspilot.support.api.ApiException;
import com.lmspilot.support.events.DomainEventPublisher;
import com.lmspilot.support.security.CurrentUser;
import com.lmspilot.support.security.InternalTokenAuthorizer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

record ProgressUpdateRequest(
    @NotNull UUID enrollmentId,
    @NotNull UUID courseId,
    @NotNull UUID lessonId,
    Boolean completed,
    @Min(0) @Max(86_400) Long learningSecondsDelta,
    @Size(max = 500) String position
) {}

record LessonProgressResponse(
    UUID lessonId,
    boolean completed,
    long learningSeconds,
    String position,
    Instant completedAt,
    Instant updatedAt
) {}

record CourseProgressResponse(
    UUID enrollmentId,
    UUID courseId,
    int courseVersion,
    UUID userId,
    int progressPercent,
    LearningStatus status,
    UUID lastLessonId,
    String lastPosition,
    long totalLearningSeconds,
    Instant lastAccessedAt,
    Instant completedAt,
    List<LessonProgressResponse> lessons
) {}

record InternalCourseProgressSummary(
    UUID enrollmentId,
    UUID courseId,
    int courseVersion,
    int progressPercent,
    LearningStatus status,
    Instant completedAt
) {}

record EnrollmentValidationPayload(
    UUID enrollmentId,
    UUID classId,
    UUID courseId,
    int courseVersion,
    UUID userId,
    String status,
    Instant dueAt
) {}

record CourseLessonMetadata(UUID id, boolean required) {}

record CourseLearningMetadataPayload(
    UUID courseId,
    int version,
    String code,
    String name,
    String status,
    List<CourseLessonMetadata> lessons
) {}

record CourseShape(int version, Set<UUID> lessonIds, Set<UUID> requiredLessonIds, boolean authoritative) {
    static CourseShape unavailable(Collection<LessonProgressEntity> current, int version) {
        Set<UUID> ids = current.stream().map(item -> item.lessonId).collect(Collectors.toSet());
        return new CourseShape(Math.max(1, version), ids, ids, false);
    }

    Set<UUID> completionLessonIds() {
        return requiredLessonIds.isEmpty() ? lessonIds : requiredLessonIds;
    }
}

record CachedCourseShape(CourseShape value, Instant expiresAt) {}

@Service
@Transactional
class LearningProgressService {
    private static final Duration COURSE_SHAPE_TTL = Duration.ofMinutes(5);

    private final CourseProgressRepository courses;
    private final LessonProgressRepository lessons;
    private final IdempotencyRecordRepository keys;
    private final DomainEventPublisher events;
    private final RestClient courseClient;
    private final RestClient enrollmentClient;
    private final String internalToken;
    private final Map<UUID, CachedCourseShape> courseShapeCache = new ConcurrentHashMap<>();

    LearningProgressService(
        CourseProgressRepository courses,
        LessonProgressRepository lessons,
        IdempotencyRecordRepository keys,
        DomainEventPublisher events,
        @Value("${course-service.url:http://localhost:8083}") String courseServiceUrl,
        @Value("${enrollment-service.url:http://localhost:8084}") String enrollmentServiceUrl,
        @Value("${lmspilot.internal-token}") String internalToken
    ) {
        this.courses = courses;
        this.lessons = lessons;
        this.keys = keys;
        this.events = events;
        this.internalToken = internalToken;
        this.courseClient = client(courseServiceUrl);
        this.enrollmentClient = client(enrollmentServiceUrl);
    }

    private RestClient client(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(1));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Transactional(readOnly = true)
    List<CourseProgressResponse> mine() {
        List<CourseProgressEntity> rows = courses.findAllByUserIdOrderByUpdatedAtDesc(user());
        Map<UUID, List<LessonProgressEntity>> byEnrollment = lessonRows(rows);
        return rows.stream()
            .map(row -> view(row, byEnrollment.getOrDefault(row.enrollmentId, List.of())))
            .toList();
    }

    CourseProgressResponse detail(UUID enrollmentId) {
        CourseProgressEntity row = courses.findByEnrollmentId(enrollmentId).orElse(null);
        if (row == null) {
            EnrollmentValidationPayload enrollment = requireEnrollment(enrollmentId);
            if (!Objects.equals(enrollment.userId(), user())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "LEARNING_OUT_OF_SCOPE", "Không được xem tiến độ của người khác");
            }
            row = createProgress(enrollment);
            courses.save(row);
        }
        if (!row.userId.equals(user())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "LEARNING_OUT_OF_SCOPE", "Không được xem tiến độ của người khác");
        }
        return view(row, lessons.findAllByEnrollmentIdOrderByUpdatedAtAsc(enrollmentId));
    }

    CourseProgressResponse update(ProgressUpdateRequest input, String idempotencyKey) {
        requireIdempotencyKey(idempotencyKey);
        CourseProgressEntity existing = courses.findByEnrollmentId(input.enrollmentId()).orElse(null);
        if (existing == null) {
            EnrollmentValidationPayload enrollment = requireEnrollment(input.enrollmentId());
            if (!Objects.equals(enrollment.userId(), user())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "LEARNING_OUT_OF_SCOPE", "Không được cập nhật tiến độ của người khác");
            }
            if (!Objects.equals(enrollment.courseId(), input.courseId())) {
                throw new ApiException(HttpStatus.CONFLICT, "LEARNING_COURSE_MISMATCH", "Lượt ghi danh không thuộc khóa học này");
            }
            existing = createProgress(enrollment);
        }
        if (!existing.userId.equals(user())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "LEARNING_OUT_OF_SCOPE", "Không được cập nhật tiến độ của người khác");
        }
        if (!existing.courseId.equals(input.courseId())) {
            throw new ApiException(HttpStatus.CONFLICT, "LEARNING_COURSE_MISMATCH", "Tiến độ không thuộc khóa học này");
        }
        if (keys.existsById(idempotencyKey)) {
            return view(existing, lessons.findAllByEnrollmentIdOrderByUpdatedAtAsc(input.enrollmentId()));
        }

        ApplyResult result = applyProgress(
            existing,
            input.lessonId(),
            Boolean.TRUE.equals(input.completed()),
            input.learningSecondsDelta() == null ? 0 : input.learningSecondsDelta(),
            input.position(),
            idempotencyKey
        );
        return view(result.course(), result.lessons());
    }

    void completePassedExamLesson(UUID sessionId, UUID enrollmentId, UUID courseId, UUID lessonId, UUID userId) {
        if (sessionId == null || enrollmentId == null || courseId == null || lessonId == null || userId == null) return;
        String key = "exam-grade:" + sessionId;
        if (keys.existsById(key)) return;
        CourseProgressEntity course = courses.findByEnrollmentId(enrollmentId).orElseGet(() -> {
            EnrollmentValidationPayload enrollment = requireEnrollment(enrollmentId);
            if (!Objects.equals(enrollment.userId(), userId) || !Objects.equals(enrollment.courseId(), courseId)) {
                throw new ApiException(HttpStatus.CONFLICT, "EXAM_ENROLLMENT_MISMATCH", "Kết quả thi không khớp lượt ghi danh");
            }
            return createProgress(enrollment);
        });
        if (!course.userId.equals(userId) || !course.courseId.equals(courseId)) return;
        applyProgress(course, lessonId, true, 0, "passed-exam", key);
    }

    void completeGradedAssignmentLesson(AssignmentSubmissionEntityView submission) {
        String key = "assignment-grade:" + submission.id();
        if (keys.existsById(key)) return;
        CourseProgressEntity course = courses.findByEnrollmentId(submission.enrollmentId()).orElseGet(() -> {
            EnrollmentValidationPayload enrollment = requireEnrollment(submission.enrollmentId());
            return createProgress(enrollment);
        });
        if (!course.userId.equals(submission.userId()) || !course.courseId.equals(submission.courseId())) return;
        applyProgress(course, submission.lessonId(), true, 0, "graded-assignment", key);
    }

    @Transactional(readOnly = true)
    List<InternalCourseProgressSummary> summaries(UUID userId) {
        return courses.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
            .map(row -> new InternalCourseProgressSummary(
                row.enrollmentId,
                row.courseId,
                row.courseVersion,
                row.progressPercent,
                row.status,
                row.completedAt
            ))
            .toList();
    }

    private CourseProgressEntity createProgress(EnrollmentValidationPayload enrollment) {
        if (enrollment.status() != null && "CANCELLED".equalsIgnoreCase(enrollment.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "ENROLLMENT_INACTIVE", "Lượt ghi danh đã bị hủy");
        }
        CourseProgressEntity row = new CourseProgressEntity();
        row.enrollmentId = enrollment.enrollmentId();
        row.courseId = enrollment.courseId();
        row.courseVersion = Math.max(1, enrollment.courseVersion());
        row.userId = enrollment.userId();
        row.startedAt = Instant.now();
        row.lastAccessedAt = row.startedAt;
        row.updatedAt = row.startedAt;
        return row;
    }

    private ApplyResult applyProgress(
        CourseProgressEntity course,
        UUID lessonId,
        boolean completed,
        long learningSecondsDelta,
        String position,
        String idempotencyKey
    ) {
        List<LessonProgressEntity> currentRows = new ArrayList<>(lessons.findAllByEnrollmentIdOrderByUpdatedAtAsc(course.enrollmentId));
        CourseShape shape = courseShape(course.courseId, currentRows, course.courseVersion);
        if (shape.authoritative() && !shape.lessonIds().contains(lessonId)) {
            throw new ApiException(HttpStatus.CONFLICT, "LESSON_OUT_OF_COURSE", "Bài học không thuộc phiên bản khóa học hiện tại");
        }

        LessonProgressEntity lesson = currentRows.stream()
            .filter(item -> item.lessonId.equals(lessonId))
            .findFirst()
            .orElseGet(() -> {
                LessonProgressEntity created = new LessonProgressEntity();
                created.enrollmentId = course.enrollmentId;
                created.courseId = course.courseId;
                created.lessonId = lessonId;
                created.userId = course.userId;
                created.openedAt = Instant.now();
                currentRows.add(created);
                return created;
            });
        boolean lessonWasCompleted = lesson.completed;
        lesson.learningSeconds = Math.min(Long.MAX_VALUE - Math.max(0, learningSecondsDelta), lesson.learningSeconds)
            + Math.max(0, learningSecondsDelta);
        if (position != null && !position.isBlank()) lesson.position = position.trim();
        if (completed) {
            lesson.completed = true;
            if (lesson.completedAt == null) lesson.completedAt = Instant.now();
        }
        lesson.updatedAt = Instant.now();
        lessons.save(lesson);

        boolean courseWasCompleted = course.status == LearningStatus.COMPLETED;
        course.lastLessonId = lessonId;
        course.lastPosition = lesson.position;
        course.lastAccessedAt = Instant.now();
        recomputeCourse(course, currentRows, shape);
        courses.save(course);
        rememberIdempotencyKey(idempotencyKey);

        boolean lessonTransitioned = !lessonWasCompleted && lesson.completed;
        boolean courseTransitioned = !courseWasCompleted && course.status == LearningStatus.COMPLETED;
        if (lessonTransitioned) {
            events.publish(
                EventTypes.LESSON_COMPLETED,
                "learning-service",
                course.enrollmentId.toString(),
                new LessonCompletedPayload(course.enrollmentId, course.courseId, lessonId, course.userId, course.progressPercent)
            );
        }
        if (courseTransitioned && !course.completionEventPublished) {
            course.completionEventPublished = true;
            courses.save(course);
            events.publish(
                EventTypes.COURSE_COMPLETED,
                "learning-service",
                course.enrollmentId.toString(),
                new CourseCompletedPayload(course.enrollmentId, course.courseId, course.userId, course.completedAt)
            );
        }
        currentRows.sort(Comparator.comparing(item -> item.updatedAt));
        return new ApplyResult(course, List.copyOf(currentRows));
    }

    private void recomputeCourse(CourseProgressEntity course, List<LessonProgressEntity> rows, CourseShape shape) {
        Map<UUID, LessonProgressEntity> byLesson = rows.stream()
            .collect(Collectors.toMap(item -> item.lessonId, item -> item, (left, right) -> right, LinkedHashMap::new));
        course.totalLearningSeconds = rows.stream().mapToLong(item -> Math.max(0, item.learningSeconds)).sum();

        if (shape.authoritative()) {
            int total = shape.lessonIds().size();
            long completedCount = shape.lessonIds().stream()
                .map(byLesson::get)
                .filter(Objects::nonNull)
                .filter(item -> item.completed)
                .count();
            boolean complete = !shape.completionLessonIds().isEmpty()
                && shape.completionLessonIds().stream().allMatch(id -> {
                    LessonProgressEntity progress = byLesson.get(id);
                    return progress != null && progress.completed;
                });
            int percent = total == 0 ? 0 : (int) Math.round(completedCount * 100.0 / total);
            course.progressPercent = complete ? 100 : Math.max(0, Math.min(99, percent));
            course.status = complete
                ? LearningStatus.COMPLETED
                : course.progressPercent > 0 || !rows.isEmpty() ? LearningStatus.IN_PROGRESS : LearningStatus.NOT_STARTED;
            if (complete && course.completedAt == null) course.completedAt = Instant.now();
            if (!complete) {
                course.completedAt = null;
                course.completionEventPublished = false;
            }
            course.courseVersion = shape.version();
        } else {
            // Khi Course Service tạm thời không phản hồi, tuyệt đối không suy ra 100%
            // từ tập các bài đã có bản ghi, vì các bài chưa mở chưa tồn tại trong DB.
            long completedCount = rows.stream().filter(item -> item.completed).count();
            int observed = rows.isEmpty() ? 0 : (int) Math.round(completedCount * 100.0 / rows.size());
            course.progressPercent = Math.max(0, Math.min(99, observed));
            course.status = rows.isEmpty() ? LearningStatus.NOT_STARTED : LearningStatus.IN_PROGRESS;
            course.completedAt = null;
            course.completionEventPublished = false;
        }
        course.updatedAt = Instant.now();
    }

    private EnrollmentValidationPayload requireEnrollment(UUID enrollmentId) {
        try {
            EnrollmentValidationPayload payload = enrollmentClient.get()
                .uri("/internal/v1/enrollments/{id}", enrollmentId)
                .header("X-Service-Token", internalToken)
                .retrieve()
                .body(EnrollmentValidationPayload.class);
            if (payload == null) throw new IllegalStateException("empty enrollment response");
            return payload;
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ENROLLMENT_SERVICE_UNAVAILABLE",
                "Không xác minh được lượt ghi danh. Vui lòng thử lại sau vài giây."
            );
        }
    }

    private CourseShape courseShape(UUID courseId, Collection<LessonProgressEntity> fallbackRows, int version) {
        CachedCourseShape cached = courseShapeCache.get(courseId);
        Instant now = Instant.now();
        if (cached != null && cached.expiresAt().isAfter(now) && cached.value().version() == version) return cached.value();
        try {
            CourseLearningMetadataPayload metadata = courseClient.get()
                .uri(uri -> uri.path("/internal/v1/courses/{id}/learning-metadata").queryParam("version", version).build(courseId))
                .header("X-Service-Token", internalToken)
                .retrieve()
                .body(CourseLearningMetadataPayload.class);
            if (metadata == null || metadata.lessons() == null) return CourseShape.unavailable(fallbackRows, version);
            Set<UUID> ids = metadata.lessons().stream().map(CourseLessonMetadata::id).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<UUID> required = metadata.lessons().stream().filter(CourseLessonMetadata::required).map(CourseLessonMetadata::id).filter(Objects::nonNull).collect(Collectors.toSet());
            CourseShape result = new CourseShape(Math.max(1, metadata.version()), ids, required, true);
            courseShapeCache.put(courseId, new CachedCourseShape(result, now.plus(COURSE_SHAPE_TTL)));
            return result;
        } catch (RuntimeException ignored) {
            return CourseShape.unavailable(fallbackRows, version);
        }
    }

    private Map<UUID, List<LessonProgressEntity>> lessonRows(List<CourseProgressEntity> courseRows) {
        if (courseRows.isEmpty()) return Map.of();
        Set<UUID> enrollmentIds = courseRows.stream().map(item -> item.enrollmentId).collect(Collectors.toSet());
        return lessons.findAllByEnrollmentIdInOrderByUpdatedAtAsc(enrollmentIds).stream()
            .collect(Collectors.groupingBy(item -> item.enrollmentId, LinkedHashMap::new, Collectors.toList()));
    }

    private CourseProgressResponse view(CourseProgressEntity course, List<LessonProgressEntity> lessonRows) {
        return new CourseProgressResponse(
            course.enrollmentId,
            course.courseId,
            course.courseVersion,
            course.userId,
            course.progressPercent,
            course.status,
            course.lastLessonId,
            course.lastPosition,
            course.totalLearningSeconds,
            course.lastAccessedAt,
            course.completedAt,
            lessonRows.stream().map(lesson -> new LessonProgressResponse(
                lesson.lessonId,
                lesson.completed,
                lesson.learningSeconds,
                lesson.position,
                lesson.completedAt,
                lesson.updatedAt
            )).toList()
        );
    }

    private void requireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED", "Thiếu Idempotency-Key");
        }
        if (key.length() > 160) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_TOO_LONG", "Idempotency-Key quá dài");
        }
    }

    private void rememberIdempotencyKey(String key) {
        IdempotencyRecordEntity row = new IdempotencyRecordEntity();
        row.idempotencyKey = key;
        keys.save(row);
    }

    private UUID user() {
        try {
            return CurrentUser.id();
        } catch (Exception ignored) {
            return new UUID(0, 1);
        }
    }

    private record ApplyResult(CourseProgressEntity course, List<LessonProgressEntity> lessons) {}
}

record AssignmentSubmissionEntityView(UUID id, UUID enrollmentId, UUID courseId, UUID lessonId, UUID userId) {}

@RestController
@RequestMapping("/api/v1/learning")
public class LearningApi {
    private final LearningProgressService service;

    public LearningApi(LearningProgressService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public List<CourseProgressResponse> mine() {
        return service.mine();
    }

    @GetMapping("/{enrollmentId}")
    public CourseProgressResponse detail(@PathVariable UUID enrollmentId) {
        return service.detail(enrollmentId);
    }

    @PutMapping("/progress")
    public CourseProgressResponse update(
        @Valid @RequestBody ProgressUpdateRequest input,
        @RequestHeader(value = "Idempotency-Key", required = false) String key
    ) {
        return service.update(input, key);
    }
}

@RestController
@RequestMapping("/internal/v1/learning")
class InternalLearningController {
    private final LearningProgressService service;
    private final InternalTokenAuthorizer token;

    InternalLearningController(LearningProgressService service, InternalTokenAuthorizer token) {
        this.service = service;
        this.token = token;
    }

    @GetMapping("/users/{userId}/courses")
    List<InternalCourseProgressSummary> summaries(
        @PathVariable UUID userId,
        @RequestHeader(value = "X-Service-Token", required = false) String key
    ) {
        token.require(key);
        return service.summaries(userId);
    }
}
