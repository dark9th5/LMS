# Bản đồ API và database LMSPilot

> Các prefix dưới đây được route qua `api-gateway:8080`. API nội bộ `/internal/v1/**` không được gọi trực tiếp từ trình duyệt.

## apps/web — port 3000

- **Phạm vi:** Giao diện Next.js cho ba cổng Admin, Giảng viên và Học viên.
- **API chính:** Chỉ gọi API Gateway qua `/api/**` hoặc URL gateway cấu hình.
- **PostgreSQL schema:** `—`
- **Bảng sở hữu:** —
- **Phụ thuộc:** API Gateway
- **Ưu tiên nâng cấp/test:** Frontend/UI/UX, accessibility, browser E2E

## api-gateway — port 8080

- **Phạm vi:** Điểm vào duy nhất của frontend; xác thực JWT, rate limit, correlation ID và định tuyến.
- **API chính:** `/api/v1/**`, `/public/v1/**` → các service nội bộ
- **PostgreSQL schema:** `—`
- **Bảng sở hữu:** —
- **Phụ thuộc:** Identity, Redis và toàn bộ service backend
- **Ưu tiên nâng cấp/test:** Security, routing, rate limit, integration test

## identity-service — port 8081

- **Phạm vi:** Đăng nhập, phiên, tài khoản và mô hình một tài khoản–một vai trò `ADMIN`/`INSTRUCTOR`/`STUDENT`.
- **API chính:** `/api/v1/auth`, `/api/v1/users`, `/api/v1/roles`, `/api/v1/authorization`, `/api/v1/directory`
- **PostgreSQL schema:** `identity`
- **Bảng sở hữu:** roles, role_permissions, user_accounts, user_roles, refresh_tokens, authorization_grants, scoped_role_assignments, bulk_operations, identity_system_locks, password_history
- **Phụ thuộc:** PostgreSQL, RabbitMQ
- **Ưu tiên nâng cấp/test:** Auth, account security, session, role isolation

## organization-service — port 8082

- **Phạm vi:** Cây cơ cấu tổ chức, đơn vị, quan hệ thành viên và phạm vi dữ liệu.
- **API chính:** `/api/v1/organization/units`, `/api/v1/organization/memberships`
- **PostgreSQL schema:** `organization`
- **Bảng sở hữu:** organization_units, organization_memberships_v2, organization_scope_projection
- **Phụ thuộc:** PostgreSQL
- **Ưu tiên nâng cấp/test:** Organization tree, membership, scope query

## course-service — port 8083

- **Phạm vi:** Khóa học, chương, bài học, tài liệu, video, bài thực hành, thảo luận và phiên bản xuất bản.
- **API chính:** `/api/v1/categories`, `/api/v1/courses`, `/api/v1/discussions`
- **PostgreSQL schema:** `course`
- **Bảng sở hữu:** course_categories, courses, lessons, demo_seed_history, course_document_links, course_versions, discussion_threads, discussion_posts
- **Phụ thuộc:** PostgreSQL, File Storage, Enrollment
- **Ưu tiên nâng cấp/test:** Course authoring, publishing, content versioning

## enrollment-service — port 8084

- **Phạm vi:** Giao khóa học trực tiếp cho học viên, ghi danh, hạn học, phiên học trực tuyến và lộ trình học.
- **API chính:** `/api/v1/enrollments`, `/api/v1/course-assignments`, `/api/v1/live-sessions`, `/api/v1/learning-paths`
- **PostgreSQL schema:** `enrollment`
- **Bảng sở hữu:** training_classes (tương thích dữ liệu cũ, không có UI lớp học), class_instructors, enrollments, course_cohorts_v2, course_assignments_v2, live_sessions, learning_paths, learning_path_items, learning_path_assignments, user_learning_paths
- **Phụ thuộc:** PostgreSQL, Course, Organization, Learning
- **Ưu tiên nâng cấp/test:** Assignment, enrollment, deadline, learning path

## learning-service — port 8085

- **Phạm vi:** Tiến độ học, mở nội dung, hoàn thành bài học, nộp/chấm bài thực hành và xAPI.
- **API chính:** `/api/v1/learning`, `/api/v1/learning/assignments`, `/api/v1/xapi/statements`
- **PostgreSQL schema:** `learning`
- **Bảng sở hữu:** course_progress, lesson_progress, idempotency_records, assignment_submissions, xapi_statements
- **Phụ thuộc:** PostgreSQL, Course, Enrollment, File Storage
- **Ưu tiên nâng cấp/test:** Learning player, progress, assignment submission

## assessment-service — port 8086

- **Phạm vi:** Ngân hàng câu hỏi, bài kiểm tra trong khóa học, bài thi độc lập, phiên làm bài và cuộc thi.
- **API chính:** `/api/v1/questions`, `/api/v1/exams`, `/api/v1/exam-sessions`, `/api/v1/competitions`, `/api/v1/assessment-assignments`
- **PostgreSQL schema:** `assessment`
- **Bảng sở hữu:** questions, exams, exam_questions, exam_sessions, demo_seed_history, assessment_contexts, competitions, competition_leaderboard, competition_rewards, reward_ledger, question_provenance, assessment_assignments, exam_session_events
- **Phụ thuộc:** PostgreSQL, Course, Enrollment, Organization
- **Ưu tiên nâng cấp/test:** Exam lifecycle, autosave, anti-loss, objective scoring

## grading-service — port 8087

- **Phạm vi:** Chấm tự động, chấm thủ công, lịch sử điểm, phản hồi và phúc khảo.
- **API chính:** `/api/v1/grades`, `/api/v1/grading`
- **PostgreSQL schema:** `grading`
- **Bảng sở hữu:** grade_results, grade_revisions, grade_appeals
- **Phụ thuộc:** PostgreSQL, Assessment
- **Ưu tiên nâng cấp/test:** Grading queue, revisions, appeal workflow

## reporting-service — port 8088

- **Phạm vi:** Read model, dashboard, KPI, báo cáo học tập và xuất báo cáo theo lịch.
- **API chính:** `/api/v1/reports`, `/api/v1/dashboard`, `/internal/v1/reports/reminders`
- **PostgreSQL schema:** `reporting`
- **Bảng sở hữu:** report_events, learner_course_read_model, report_schedules, report_export_jobs
- **Phụ thuộc:** PostgreSQL, RabbitMQ, Enrollment
- **Ưu tiên nâng cấp/test:** Projection, KPI, export, performance

## file-storage-service — port 8089

- **Phạm vi:** Tải lên/tải xuống, quyền truy cập, phiên bản, xem PDF/DOCX/video và phiên chỉnh sửa.
- **API chính:** `/api/v1/files`, `/internal/v1/files`, `/public/v1/file-edit`
- **PostgreSQL schema:** `file_storage`
- **Bảng sở hữu:** stored_files, demo_seed_history, file_versions_v2, file_edit_sessions, file_access_grants
- **Phụ thuộc:** PostgreSQL, S3 tùy chọn, ONLYOFFICE/Collabora tùy chọn
- **Ưu tiên nâng cấp/test:** Upload security, preview, versioning, editor integration

## license-service — port 8090

- **Phạm vi:** Kích hoạt giấy phép, entitlement và giới hạn tính năng triển khai.
- **API chính:** `/api/v1/license`
- **PostgreSQL schema:** `license`
- **Bảng sở hữu:** licenses
- **Phụ thuộc:** PostgreSQL
- **Ưu tiên nâng cấp/test:** License validation, grace period, tamper resistance

## audit-service — port 8091

- **Phạm vi:** Nhật ký kiểm toán bất biến và xuất dữ liệu kiểm toán.
- **API chính:** `/api/v1/audit`
- **PostgreSQL schema:** `audit`
- **Bảng sở hữu:** audit_entries
- **Phụ thuộc:** PostgreSQL, RabbitMQ
- **Ưu tiên nâng cấp/test:** Immutable audit, retention, export

## notification-service — port 8092

- **Phạm vi:** Thông báo trong hệ thống, email, tin tức, mẫu và nhắc hạn.
- **API chính:** `/api/v1/notifications`, `/api/v1/news`
- **PostgreSQL schema:** `notification`
- **Bảng sở hữu:** notifications, news_articles, news_attachments, news_receipts, notification_templates, notification_reminder_rules, notification_reminder_dispatches
- **Phụ thuộc:** PostgreSQL, RabbitMQ, SMTP tùy chọn
- **Ưu tiên nâng cấp/test:** Outbox, retry, templates, reminders

## certificate-service — port 8093

- **Phạm vi:** Mẫu chứng chỉ, cấp, tra cứu, in, thu hồi và cấp lại chứng chỉ.
- **API chính:** `/api/v1/certificates`, `/public/v1/certificates`
- **PostgreSQL schema:** `certificate`
- **Bảng sở hữu:** certificates, certificate_templates
- **Phụ thuộc:** PostgreSQL
- **Ưu tiên nâng cấp/test:** Certificate lifecycle, verification, template rendering

## ai-service — port 8094

- **Phạm vi:** Cấu hình model OpenAI-compatible; trích xuất PDF/DOCX; sinh, kiểm tra và duyệt câu hỏi theo độ khó.
- **API chính:** `/api/v1/ai`
- **PostgreSQL schema:** `ai`
- **Bảng sở hữu:** ai_provider_configs, question_generation_jobs, question_generation_reviews
- **Phụ thuộc:** PostgreSQL, File Storage, Course, Assessment, AI provider
- **Ưu tiên nâng cấp/test:** Prompt/schema, grounding, quality validator, provider adapters

## configuration-service — port 8095

- **Phạm vi:** Thông tin hệ thống, thương hiệu, logo, màu sắc, ảnh nền đăng nhập và cấu hình dịch vụ ngoài.
- **API chính:** `/api/v1/configuration`, `/api/v1/branding`, `/api/v1/external-services`, `/public/v1/configuration`, `/public/v1/branding`
- **PostgreSQL schema:** `configuration`
- **Bảng sở hữu:** product_configuration, branding_profiles, external_service_configs
- **Phụ thuộc:** PostgreSQL, File Storage
- **Ưu tiên nâng cấp/test:** Branding, public config, secret-safe settings

## integration-service — port 8096

- **Phạm vi:** Adapter cho Redis, SMTP, S3, ONLYOFFICE, họp trực tuyến và dịch vụ bên thứ ba.
- **API chính:** `/api/v1/integrations`
- **PostgreSQL schema:** `integration`
- **Bảng sở hữu:** integration_adapters
- **Phụ thuộc:** PostgreSQL và nhà cung cấp bên thứ ba
- **Ưu tiên nâng cấp/test:** Adapters, health test, credentials, resilience

## operations-service — port 8097

- **Phạm vi:** Health tổng hợp, tác vụ vận hành, agent lease và lịch chạy nội bộ.
- **API chính:** `/api/v1/operations`
- **PostgreSQL schema:** `operations`
- **Bảng sở hữu:** operation_jobs, operation_schedules
- **Phụ thuộc:** PostgreSQL và các service nghiệp vụ
- **Ưu tiên nâng cấp/test:** Health, scheduled jobs, backup/restore orchestration

## competency-service — port 8098

- **Phạm vi:** Khung năng lực, hồ sơ năng lực, đánh giá khoảng thiếu và ánh xạ khóa học.
- **API chính:** `/api/v1/competencies`
- **PostgreSQL schema:** `competency`
- **Bảng sở hữu:** competencies, competency_profiles, competency_profile_requirements, user_competency_profiles, user_competency_assessments, course_competency_maps
- **Phụ thuộc:** PostgreSQL, License
- **Ưu tiên nâng cấp/test:** Competency framework, gap analysis, course mapping

# Phụ lục: endpoint inventory từ controller

## `ai-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/ai/status` | `AiController` (`AiApi.kt`) |
| `POST` | `/api/v1/ai/question-drafts` | `AiController` (`AiApi.kt`) |
| `GET` | `/api/v1/ai/providers` | `QuestionGenerationController` (`QuestionGenerationApi.kt`) |
| `POST` | `/api/v1/ai/providers` | `QuestionGenerationController` (`QuestionGenerationApi.kt`) |
| `PUT` | `/api/v1/ai/providers/{id}` | `QuestionGenerationController` (`QuestionGenerationApi.kt`) |
| `GET` | `/api/v1/ai/question-generation-jobs` | `QuestionGenerationController` (`QuestionGenerationApi.kt`) |
| `GET` | `/api/v1/ai/question-generation-jobs/{id}` | `QuestionGenerationController` (`QuestionGenerationApi.kt`) |
| `POST` | `/api/v1/ai/question-generation-jobs` | `QuestionGenerationController` (`QuestionGenerationApi.kt`) |
| `POST` | `/api/v1/ai/question-generation-jobs/{id}/review` | `QuestionGenerationController` (`QuestionGenerationApi.kt`) |
| `POST` | `/api/v1/ai/question-generation-jobs/{id}/import` | `QuestionGenerationController` (`QuestionGenerationApi.kt`) |

## `assessment-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/questions` | `QuestionController` (`AssessmentApi.kt`) |
| `POST` | `/api/v1/questions` | `QuestionController` (`AssessmentApi.kt`) |
| `PUT` | `/api/v1/questions/{id}` | `QuestionController` (`AssessmentApi.kt`) |
| `DELETE` | `/api/v1/questions/{id}` | `QuestionController` (`AssessmentApi.kt`) |
| `GET` | `/api/v1/exams` | `ExamController` (`AssessmentApi.kt`) |
| `POST` | `/api/v1/exams` | `ExamController` (`AssessmentApi.kt`) |
| `PUT` | `/api/v1/exams/{id}` | `ExamController` (`AssessmentApi.kt`) |
| `DELETE` | `/api/v1/exams/{id}` | `ExamController` (`AssessmentApi.kt`) |
| `GET` | `/api/v1/exams/{id}` | `ExamController` (`AssessmentApi.kt`) |
| `POST` | `/api/v1/exams/start` | `ExamController` (`AssessmentApi.kt`) |
| `GET` | `/api/v1/exam-sessions/{id}` | `SessionController` (`AssessmentApi.kt`) |
| `POST` | `/api/v1/exam-sessions/{id}/heartbeat` | `SessionController` (`AssessmentApi.kt`) |
| `POST` | `/api/v1/exam-sessions/{id}/events` | `SessionController` (`AssessmentApi.kt`) |
| `GET` | `/api/v1/exam-sessions/{id}/events` | `SessionController` (`AssessmentApi.kt`) |
| `PUT` | `/api/v1/exam-sessions/{id}/answers` | `SessionController` (`AssessmentApi.kt`) |
| `POST` | `/api/v1/exam-sessions/{id}/submit` | `SessionController` (`AssessmentApi.kt`) |
| `GET` | `/internal/v1/assessment/sessions/{id}/grading-payload` | `InternalAssessmentController` (`AssessmentApi.kt`) |
| `POST` | `/internal/v1/assessment/sessions/{id}/graded` | `InternalAssessmentController` (`AssessmentApi.kt`) |
| `GET` | `/internal/v1/assessment/exams/manageable/{userId}` | `InternalAssessmentController` (`AssessmentApi.kt`) |
| `POST` | `/internal/v1/questions/import-generated` | `InternalQuestionImportController` (`AssessmentApi.kt`) |
| `POST` | `/api/v1/assessment-assignments` | `AssessmentAssignmentController` (`AssessmentAssignmentApi.kt`) |
| `GET` | `/api/v1/assessment-assignments` | `AssessmentAssignmentController` (`AssessmentAssignmentApi.kt`) |
| `DELETE` | `/api/v1/assessment-assignments/{id}` | `AssessmentAssignmentController` (`AssessmentAssignmentApi.kt`) |
| `GET` | `/api/v1/competitions` | `CompetitionController` (`CompetitionApi.kt`) |
| `GET` | `/api/v1/competitions/{id}` | `CompetitionController` (`CompetitionApi.kt`) |
| `POST` | `/api/v1/competitions` | `CompetitionController` (`CompetitionApi.kt`) |
| `PUT` | `/api/v1/competitions/{id}` | `CompetitionController` (`CompetitionApi.kt`) |
| `GET` | `/api/v1/competitions/{id}/leaderboard` | `CompetitionController` (`CompetitionApi.kt`) |
| `POST` | `/api/v1/competitions/{id}/publish` | `CompetitionController` (`CompetitionApi.kt`) |
| `POST` | `/api/v1/competitions/{id}/rewards/issue` | `CompetitionController` (`CompetitionApi.kt`) |
| `GET` | `/api/v1/competitions/{id}/rewards/ledger` | `CompetitionController` (`CompetitionApi.kt`) |
| `POST` | `/internal/v1/competitions/{id}/results` | `InternalCompetitionController` (`CompetitionApi.kt`) |

## `audit-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/audit` | `AuditController` (`AuditApi.kt`) |
| `GET` | `/api/v1/audit/export.csv` | `AuditController` (`AuditApi.kt`) |
| `POST` | `/internal/v1/audit` | `InternalAuditController` (`AuditApi.kt`) |

## `certificate-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/certificates` | `CertificateController` (`CertificateApi.kt`) |
| `GET` | `/api/v1/certificates/me` | `CertificateController` (`CertificateApi.kt`) |
| `GET` | `/api/v1/certificates/{id}/print` | `CertificateController` (`CertificateApi.kt`) |
| `PUT` | `/api/v1/certificates/{id}/revoke` | `CertificateController` (`CertificateApi.kt`) |
| `POST` | `/api/v1/certificates/{id}/reissue` | `CertificateController` (`CertificateApi.kt`) |
| `GET` | `/api/v1/certificates/templates` | `CertificateController` (`CertificateApi.kt`) |
| `POST` | `/api/v1/certificates/templates` | `CertificateController` (`CertificateApi.kt`) |
| `PUT` | `/api/v1/certificates/templates/{id}` | `CertificateController` (`CertificateApi.kt`) |
| `DELETE` | `/api/v1/certificates/templates/{id}` | `CertificateController` (`CertificateApi.kt`) |
| `GET` | `/public/v1/certificates/{code}` | `PublicCertificateController` (`CertificateApi.kt`) |

## `competency-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/competencies` | `CompetencyController` (`CompetencyApi.kt`) |
| `POST` | `/api/v1/competencies` | `CompetencyController` (`CompetencyApi.kt`) |
| `PUT` | `/api/v1/competencies/{id}` | `CompetencyController` (`CompetencyApi.kt`) |
| `GET` | `/api/v1/competencies/profiles` | `CompetencyController` (`CompetencyApi.kt`) |
| `POST` | `/api/v1/competencies/profiles` | `CompetencyController` (`CompetencyApi.kt`) |
| `PUT` | `/api/v1/competencies/profiles/{id}` | `CompetencyController` (`CompetencyApi.kt`) |
| `POST` | `/api/v1/competencies/profile-assignments` | `CompetencyController` (`CompetencyApi.kt`) |
| `DELETE` | `/api/v1/competencies/profile-assignments` | `CompetencyController` (`CompetencyApi.kt`) |
| `POST` | `/api/v1/competencies/assessments` | `CompetencyController` (`CompetencyApi.kt`) |
| `GET` | `/api/v1/competencies/me/gaps` | `CompetencyController` (`CompetencyApi.kt`) |
| `GET` | `/api/v1/competencies/users/{userId}/gaps` | `CompetencyController` (`CompetencyApi.kt`) |
| `GET` | `/api/v1/competencies/me/assessments` | `CompetencyController` (`CompetencyApi.kt`) |
| `GET` | `/api/v1/competencies/users/{userId}/assessments` | `CompetencyController` (`CompetencyApi.kt`) |
| `POST` | `/api/v1/competencies/course-maps` | `CompetencyController` (`CompetencyApi.kt`) |

## `configuration-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/public/v1/configuration` | `ProductConfigurationController` (`ConfigurationApi.kt`) |
| `GET` | `/api/v1/configuration` | `ProductConfigurationController` (`ConfigurationApi.kt`) |
| `PUT` | `/api/v1/configuration` | `ProductConfigurationController` (`ConfigurationApi.kt`) |
| `GET` | `/public/v1/branding` | `CustomizationController` (`CustomizationApi.kt`) |
| `GET` | `/public/v1/branding/assets/{kind}` | `CustomizationController` (`CustomizationApi.kt`) |
| `GET` | `/api/v1/branding` | `CustomizationController` (`CustomizationApi.kt`) |
| `PUT` | `/api/v1/branding` | `CustomizationController` (`CustomizationApi.kt`) |
| `GET` | `/api/v1/external-services` | `CustomizationController` (`CustomizationApi.kt`) |
| `POST` | `/api/v1/external-services` | `CustomizationController` (`CustomizationApi.kt`) |
| `PUT` | `/api/v1/external-services/{id}` | `CustomizationController` (`CustomizationApi.kt`) |
| `POST` | `/api/v1/external-services/{id}/test` | `CustomizationController` (`CustomizationApi.kt`) |

## `course-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/categories` | `CategoryController` (`CourseApi.kt`) |
| `POST` | `/api/v1/categories` | `CategoryController` (`CourseApi.kt`) |
| `PUT` | `/api/v1/categories/{id}` | `CategoryController` (`CourseApi.kt`) |
| `DELETE` | `/api/v1/categories/{id}` | `CategoryController` (`CourseApi.kt`) |
| `GET` | `/api/v1/courses` | `CourseController` (`CourseApi.kt`) |
| `GET` | `/api/v1/courses/{id}` | `CourseController` (`CourseApi.kt`) |
| `GET` | `/api/v1/courses/{id}/versions` | `CourseController` (`CourseApi.kt`) |
| `GET` | `/api/v1/courses/{id}/versions/{version}` | `CourseController` (`CourseApi.kt`) |
| `POST` | `/api/v1/courses` | `CourseController` (`CourseApi.kt`) |
| `PUT` | `/api/v1/courses/{id}` | `CourseController` (`CourseApi.kt`) |
| `POST` | `/api/v1/courses/{id}/lessons` | `CourseController` (`CourseApi.kt`) |
| `PUT` | `/api/v1/courses/{courseId}/lessons/{lessonId}` | `CourseController` (`CourseApi.kt`) |
| `DELETE` | `/api/v1/courses/{courseId}/lessons/{lessonId}` | `CourseController` (`CourseApi.kt`) |
| `DELETE` | `/api/v1/courses/{id}` | `CourseController` (`CourseApi.kt`) |
| `POST` | `/api/v1/courses/{id}/status/{status}` | `CourseController` (`CourseApi.kt`) |
| `GET` | `/internal/v1/courses/{id}/publication` | `InternalCourseController` (`CourseApi.kt`) |
| `GET` | `/internal/v1/courses/{id}/document-scope` | `InternalCourseController` (`CourseApi.kt`) |
| `GET` | `/internal/v1/courses/{id}/learning-metadata` | `InternalCourseController` (`CourseApi.kt`) |
| `GET` | `/api/v1/discussions/courses/{courseId}/threads` | `DiscussionController` (`DiscussionApi.kt`) |
| `POST` | `/api/v1/discussions/courses/{courseId}/threads` | `DiscussionController` (`DiscussionApi.kt`) |
| `GET` | `/api/v1/discussions/threads/{id}` | `DiscussionController` (`DiscussionApi.kt`) |
| `POST` | `/api/v1/discussions/threads/{id}/posts` | `DiscussionController` (`DiscussionApi.kt`) |
| `PATCH` | `/api/v1/discussions/threads/{id}` | `DiscussionController` (`DiscussionApi.kt`) |
| `DELETE` | `/api/v1/discussions/posts/{id}` | `DiscussionController` (`DiscussionApi.kt`) |

## `enrollment-service`

| Method | Path | Controller |
|---|---|---|
| `POST` | `/api/v1/course-assignments` | `CourseAssignmentController` (`AssignmentAndLiveApi.kt`) |
| `GET` | `/api/v1/course-assignments` | `CourseAssignmentController` (`AssignmentAndLiveApi.kt`) |
| `DELETE` | `/api/v1/course-assignments/{id}` | `CourseAssignmentController` (`AssignmentAndLiveApi.kt`) |
| `GET` | `/api/v1/course-assignments/me` | `CourseAssignmentController` (`AssignmentAndLiveApi.kt`) |
| `POST` | `/api/v1/live-sessions` | `LiveSessionController` (`AssignmentAndLiveApi.kt`) |
| `GET` | `/api/v1/live-sessions/me` | `LiveSessionController` (`AssignmentAndLiveApi.kt`) |
| `GET` | `/api/v1/live-sessions` | `LiveSessionController` (`AssignmentAndLiveApi.kt`) |
| `GET` | `/api/v1/enrollments/me` | `EnrollmentController` (`EnrollmentApi.kt`) |
| `GET` | `/internal/v1/enrollments/{id}` | `InternalEnrollmentController` (`EnrollmentApi.kt`) |
| `GET` | `/internal/v1/enrollments/users/{userId}/courses/{courseId}` | `InternalEnrollmentController` (`EnrollmentApi.kt`) |
| `GET` | `/internal/v1/course-access/instructors/{userId}/delivery-ids` | `InternalCourseAccessController` (`EnrollmentApi.kt`) |
| `GET` | `/internal/v1/course-access/instructors/{userId}/courses` | `InternalCourseAccessController` (`EnrollmentApi.kt`) |
| `GET` | `/internal/v1/course-access/users/{userId}/courses` | `InternalCourseAccessController` (`EnrollmentApi.kt`) |
| `GET` | `/internal/v1/course-access/users/{userId}/courses/{courseId}/versions` | `InternalCourseAccessController` (`EnrollmentApi.kt`) |
| `GET` | `/api/v1/learning-paths` | `LearningPathController` (`LearningPathApi.kt`) |
| `GET` | `/api/v1/learning-paths/{id}` | `LearningPathController` (`LearningPathApi.kt`) |
| `POST` | `/api/v1/learning-paths` | `LearningPathController` (`LearningPathApi.kt`) |
| `PUT` | `/api/v1/learning-paths/{id}` | `LearningPathController` (`LearningPathApi.kt`) |
| `POST` | `/api/v1/learning-paths/{id}/clone` | `LearningPathController` (`LearningPathApi.kt`) |
| `POST` | `/api/v1/learning-paths/{id}/publish` | `LearningPathController` (`LearningPathApi.kt`) |
| `POST` | `/api/v1/learning-paths/{id}/archive` | `LearningPathController` (`LearningPathApi.kt`) |
| `POST` | `/api/v1/learning-paths/{id}/assignments` | `LearningPathController` (`LearningPathApi.kt`) |
| `GET` | `/api/v1/learning-paths/{id}/assignments` | `LearningPathController` (`LearningPathApi.kt`) |
| `DELETE` | `/api/v1/learning-paths/{pathId}/assignments/{assignmentId}` | `LearningPathController` (`LearningPathApi.kt`) |
| `GET` | `/api/v1/learning-paths/{id}/participants` | `LearningPathController` (`LearningPathApi.kt`) |
| `GET` | `/api/v1/learning-paths/me/assigned` | `LearningPathController` (`LearningPathApi.kt`) |

## `file-storage-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/files/{id}/versions` | `FileEditingController` (`FileEditingApi.kt`) |
| `POST` | `/api/v1/files/{id}/edit-sessions` | `FileEditingController` (`FileEditingApi.kt`) |
| `POST` | `/api/v1/files/edit-sessions/{id}/pdf` | `FileEditingController` (`FileEditingApi.kt`) |
| `DELETE` | `/api/v1/files/edit-sessions/{id}` | `FileEditingController` (`FileEditingApi.kt`) |
| `GET` | `/public/v1/file-edit/{id}/content` | `PublicFileEditingController` (`FileEditingApi.kt`) |
| `POST` | `/public/v1/file-edit/{id}/callback` | `PublicFileEditingController` (`FileEditingApi.kt`) |
| `GET` | `/api/v1/files` | `FileController` (`FileStorageApi.kt`) |
| `POST` | `/api/v1/files` | `FileController` (`FileStorageApi.kt`) |
| `GET` | `/api/v1/files/{id}` | `FileController` (`FileStorageApi.kt`) |
| `GET` | `/api/v1/files/{id}/docx-preview` | `FileController` (`FileStorageApi.kt`) |
| `GET` | `/api/v1/files/{id}/content` | `FileController` (`FileStorageApi.kt`) |
| `DELETE` | `/api/v1/files/{id}` | `FileController` (`FileStorageApi.kt`) |
| `GET` | `/internal/v1/files/{id}/content` | `InternalFileController` (`InternalFileApi.kt`) |
| `GET` | `/internal/v1/files/{id}` | `InternalFileController` (`InternalFileApi.kt`) |
| `POST` | `/internal/v1/files/access-grants` | `InternalFileController` (`InternalFileApi.kt`) |

## `grading-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/grades/me` | `GradeController` (`GradingApi.kt`) |
| `GET` | `/api/v1/grades/queue` | `GradeController` (`GradingApi.kt`) |
| `PUT` | `/api/v1/grades/{id}` | `GradeController` (`GradingApi.kt`) |
| `GET` | `/api/v1/grades/{id}/history` | `GradeController` (`GradingApi.kt`) |
| `POST` | `/api/v1/grades/{id}/appeals` | `GradeController` (`GradingApi.kt`) |
| `GET` | `/api/v1/grades/appeals/me` | `GradeController` (`GradingApi.kt`) |
| `GET` | `/api/v1/grades/appeals` | `GradeController` (`GradingApi.kt`) |
| `PUT` | `/api/v1/grades/appeals/{id}` | `GradeController` (`GradingApi.kt`) |

## `identity-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/authorization/catalog` | `AuthorizationCatalogController` (`AuthorizationCatalogApi.kt`) |
| `GET` | `/api/v1/authorization/users/{userId}/assignments` | `AuthorizationCatalogController` (`AuthorizationCatalogApi.kt`) |
| `GET` | `/internal/v1/authorization/check` | `InternalAuthorizationController` (`AuthorizationCatalogApi.kt`) |
| `GET` | `/internal/v1/authorization/scope-ids` | `InternalAuthorizationController` (`AuthorizationCatalogApi.kt`) |
| `POST` | `/api/v1/auth/login` | `AuthController` (`Controllers.kt`) |
| `POST` | `/api/v1/auth/refresh` | `AuthController` (`Controllers.kt`) |
| `POST` | `/api/v1/auth/logout` | `AuthController` (`Controllers.kt`) |
| `GET` | `/api/v1/auth/me` | `AuthController` (`Controllers.kt`) |
| `POST` | `/api/v1/auth/change-password` | `AuthController` (`Controllers.kt`) |
| `GET` | `/api/v1/auth/sessions` | `AuthController` (`Controllers.kt`) |
| `DELETE` | `/api/v1/auth/sessions/{sessionId}` | `AuthController` (`Controllers.kt`) |
| `DELETE` | `/api/v1/auth/sessions` | `AuthController` (`Controllers.kt`) |
| `GET` | `/api/v1/users` | `UserController` (`Controllers.kt`) |
| `GET` | `/api/v1/users/{id}` | `UserController` (`Controllers.kt`) |
| `POST` | `/api/v1/users` | `UserController` (`Controllers.kt`) |
| `POST` | `/api/v1/users/bulk` | `UserController` (`Controllers.kt`) |
| `POST` | `/api/v1/users/import/inspect` | `UserController` (`Controllers.kt`) |
| `POST` | `/api/v1/users/import/preview` | `UserController` (`Controllers.kt`) |
| `POST` | `/api/v1/users/import/commit` | `UserController` (`Controllers.kt`) |
| `PUT` | `/api/v1/users/{id}` | `UserController` (`Controllers.kt`) |
| `POST` | `/api/v1/users/{id}/reset-password` | `UserController` (`Controllers.kt`) |
| `GET` | `/api/v1/directory/students` | `DirectoryController` (`Controllers.kt`) |
| `GET` | `/api/v1/roles` | `RoleController` (`Controllers.kt`) |
| `POST` | `/api/v1/roles` | `RoleController` (`Controllers.kt`) |
| `PUT` | `/api/v1/roles/{id}` | `RoleController` (`Controllers.kt`) |
| `POST` | `/api/v1/authorization/grants/preview` | `AuthorizationController` (`Controllers.kt`) |
| `POST` | `/api/v1/authorization/grants/bulk` | `AuthorizationController` (`Controllers.kt`) |
| `DELETE` | `/api/v1/authorization/grants/bulk` | `AuthorizationController` (`Controllers.kt`) |
| `GET` | `/api/v1/authorization/effective` | `AuthorizationController` (`Controllers.kt`) |
| `GET` | `/api/v1/authorization/explain` | `AuthorizationController` (`Controllers.kt`) |
| `GET` | `/internal/v1/users/{id}/contact` | `InternalUserController` (`Controllers.kt`) |
| `GET` | `/api/v1/users/{userId}/sessions` | `UserSessionAdminController` (`Controllers.kt`) |
| `DELETE` | `/api/v1/users/{userId}/sessions/{sessionId}` | `UserSessionAdminController` (`Controllers.kt`) |
| `DELETE` | `/api/v1/users/{userId}/sessions` | `UserSessionAdminController` (`Controllers.kt`) |

## `integration-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/integrations` | `IntegrationController` (`IntegrationApi.kt`) |
| `POST` | `/api/v1/integrations` | `IntegrationController` (`IntegrationApi.kt`) |
| `PUT` | `/api/v1/integrations/{id}` | `IntegrationController` (`IntegrationApi.kt`) |
| `POST` | `/api/v1/integrations/{id}/test` | `IntegrationController` (`IntegrationApi.kt`) |

## `learning-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/learning/assignments/me` | `AssignmentSubmissionController` (`AssignmentSubmissionApi.kt`) |
| `GET` | `/api/v1/learning/assignments/{lessonId}/attempts` | `AssignmentSubmissionController` (`AssignmentSubmissionApi.kt`) |
| `POST` | `/api/v1/learning/assignments/{lessonId}/submissions` | `AssignmentSubmissionController` (`AssignmentSubmissionApi.kt`) |
| `GET` | `/api/v1/learning/assignments/queue-by-course` | `AssignmentSubmissionController` (`AssignmentSubmissionApi.kt`) |
| `PUT` | `/api/v1/learning/assignments/submissions/{id}/grade` | `AssignmentSubmissionController` (`AssignmentSubmissionApi.kt`) |
| `GET` | `/api/v1/learning/me` | `LearningController` (`LearningApi.kt`) |
| `GET` | `/api/v1/learning/{enrollmentId}` | `LearningController` (`LearningApi.kt`) |
| `PUT` | `/api/v1/learning/progress` | `LearningController` (`LearningApi.kt`) |
| `GET` | `/internal/v1/learning/users/{userId}/courses` | `InternalLearningController` (`LearningApi.kt`) |
| `POST` | `/api/v1/xapi/statements` | `XapiController` (`XapiApi.kt`) |
| `GET` | `/api/v1/xapi/statements/me` | `XapiController` (`XapiApi.kt`) |
| `GET` | `/api/v1/xapi/statements/users/{userId}` | `XapiController` (`XapiApi.kt`) |
| `GET` | `/api/v1/xapi/statements` | `XapiController` (`XapiApi.kt`) |

## `license-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/license` | `LicenseController` (`LicenseApi.kt`) |
| `POST` | `/api/v1/license/activate` | `LicenseController` (`LicenseApi.kt`) |
| `GET` | `/internal/v1/license/entitlements` | `InternalLicenseController` (`LicenseApi.kt`) |

## `notification-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/news/feed` | `NewsController` (`NewsApi.kt`) |
| `GET` | `/api/v1/news` | `NewsController` (`NewsApi.kt`) |
| `POST` | `/api/v1/news` | `NewsController` (`NewsApi.kt`) |
| `PUT` | `/api/v1/news/{id}` | `NewsController` (`NewsApi.kt`) |
| `POST` | `/api/v1/news/{id}/publish` | `NewsController` (`NewsApi.kt`) |
| `POST` | `/api/v1/news/{id}/archive` | `NewsController` (`NewsApi.kt`) |
| `PUT` | `/api/v1/news/{id}/read` | `NewsController` (`NewsApi.kt`) |
| `PUT` | `/api/v1/news/{id}/acknowledge` | `NewsController` (`NewsApi.kt`) |
| `GET` | `/api/v1/notifications` | `NotificationController` (`NotificationApi.kt`) |
| `PUT` | `/api/v1/notifications/{id}/read` | `NotificationController` (`NotificationApi.kt`) |
| `GET` | `/api/v1/notifications/templates` | `NotificationTemplateController` (`NotificationAutomationApi.kt`) |
| `POST` | `/api/v1/notifications/templates` | `NotificationTemplateController` (`NotificationAutomationApi.kt`) |
| `PUT` | `/api/v1/notifications/templates/{id}` | `NotificationTemplateController` (`NotificationAutomationApi.kt`) |
| `DELETE` | `/api/v1/notifications/templates/{id}` | `NotificationTemplateController` (`NotificationAutomationApi.kt`) |
| `GET` | `/api/v1/notifications/reminder-rules` | `NotificationReminderController` (`NotificationAutomationApi.kt`) |
| `POST` | `/api/v1/notifications/reminder-rules` | `NotificationReminderController` (`NotificationAutomationApi.kt`) |
| `PUT` | `/api/v1/notifications/reminder-rules/{id}` | `NotificationReminderController` (`NotificationAutomationApi.kt`) |
| `DELETE` | `/api/v1/notifications/reminder-rules/{id}` | `NotificationReminderController` (`NotificationAutomationApi.kt`) |
| `POST` | `/api/v1/notifications/reminder-rules/{id}/run` | `NotificationReminderController` (`NotificationAutomationApi.kt`) |

## `operations-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/operations/health` | `OperationsController` (`OperationsApi.kt`) |
| `GET` | `/api/v1/operations/jobs` | `OperationsController` (`OperationsApi.kt`) |
| `POST` | `/api/v1/operations/jobs/{type}` | `OperationsController` (`OperationsApi.kt`) |
| `GET` | `/api/v1/operations/schedules` | `OperationsController` (`OperationsApi.kt`) |
| `POST` | `/api/v1/operations/schedules` | `OperationsController` (`OperationsApi.kt`) |
| `PUT` | `/api/v1/operations/schedules/{id}` | `OperationsController` (`OperationsApi.kt`) |
| `DELETE` | `/api/v1/operations/schedules/{id}` | `OperationsController` (`OperationsApi.kt`) |
| `POST` | `/internal/v1/operations/jobs/claim` | `InternalOperationsAgentController` (`OperationsApi.kt`) |
| `POST` | `/internal/v1/operations/jobs/{id}/heartbeat` | `InternalOperationsAgentController` (`OperationsApi.kt`) |
| `POST` | `/internal/v1/operations/jobs/{id}/complete` | `InternalOperationsAgentController` (`OperationsApi.kt`) |

## `organization-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/organization/memberships` | `OrganizationMembershipController` (`MembershipApi.kt`) |
| `POST` | `/api/v1/organization/memberships/bulk` | `OrganizationMembershipController` (`MembershipApi.kt`) |
| `DELETE` | `/api/v1/organization/memberships/bulk` | `OrganizationMembershipController` (`MembershipApi.kt`) |
| `GET` | `/internal/v1/organization/units/{id}/descendants` | `InternalOrganizationScopeController` (`MembershipApi.kt`) |
| `GET` | `/internal/v1/organization/users/{userId}/unit-ids` | `InternalOrganizationScopeController` (`MembershipApi.kt`) |
| `GET` | `/internal/v1/organization/units/{id}/users` | `InternalOrganizationScopeController` (`MembershipApi.kt`) |
| `GET` | `/internal/v1/organization/units/{id}/ancestors` | `InternalOrganizationScopeController` (`MembershipApi.kt`) |
| `GET` | `/api/v1/organization/units` | `OrganizationController` (`OrganizationApi.kt`) |
| `GET` | `/api/v1/organization/units/tree` | `OrganizationController` (`OrganizationApi.kt`) |
| `POST` | `/api/v1/organization/units` | `OrganizationController` (`OrganizationApi.kt`) |
| `PUT` | `/api/v1/organization/units/{id}` | `OrganizationController` (`OrganizationApi.kt`) |
| `POST` | `/api/v1/organization/units/{id}/deactivate` | `OrganizationController` (`OrganizationApi.kt`) |
| `POST` | `/internal/v1/organization/units/validate-active` | `InternalOrganizationUnitController` (`OrganizationApi.kt`) |

## `reporting-service`

| Method | Path | Controller |
|---|---|---|
| `GET` | `/api/v1/reports/kpis` | `KpiReportingController` (`KpiReportingApi.kt`) |
| `GET` | `/api/v1/reports/kpis/courses` | `KpiReportingController` (`KpiReportingApi.kt`) |
| `GET` | `/internal/v1/reports/reminders/due` | `InternalReminderReportingController` (`ReminderReportingApi.kt`) |
| `GET` | `/api/v1/reports/dashboard` | `ReportingController` (`ReportingApi.kt`) |
| `GET` | `/api/v1/reports/learning` | `ReportingController` (`ReportingApi.kt`) |
| `GET` | `/api/v1/reports/me` | `ReportingController` (`ReportingApi.kt`) |
| `GET` | `/api/v1/reports/learning/export.csv` | `ReportingController` (`ReportingApi.kt`) |
| `POST` | `/api/v1/reports/exports` | `ScheduledReportingController` (`ScheduledReportingApi.kt`) |
| `GET` | `/api/v1/reports/exports` | `ScheduledReportingController` (`ScheduledReportingApi.kt`) |
| `GET` | `/api/v1/reports/exports/{id}/download` | `ScheduledReportingController` (`ScheduledReportingApi.kt`) |
| `POST` | `/api/v1/reports/schedules` | `ScheduledReportingController` (`ScheduledReportingApi.kt`) |
| `GET` | `/api/v1/reports/schedules` | `ScheduledReportingController` (`ScheduledReportingApi.kt`) |
| `PUT` | `/api/v1/reports/schedules/{id}` | `ScheduledReportingController` (`ScheduledReportingApi.kt`) |
| `DELETE` | `/api/v1/reports/schedules/{id}` | `ScheduledReportingController` (`ScheduledReportingApi.kt`) |

