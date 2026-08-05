# Bản đồ API và database LMSPilot 0.21.0

API được liệt kê theo base path thực tế trong Java controller; bảng được trích từ Flyway migration.

## `api-gateway` — port 8080

Điểm vào duy nhất của frontend; xác thực JWT, rate limit, correlation ID và định tuyến API.

- PostgreSQL schema: `—`
- Controller Java: `—`
- API base: `—`
- Bảng sở hữu: `—`
- Flyway: không dùng database

## `identity-service` — port 8081

Đăng nhập, JWT/refresh token, phiên, tài khoản, vai trò độc quyền ADMIN/INSTRUCTOR/STUDENT và quyền theo phạm vi.

- PostgreSQL schema: `identity`
- Controller Java: `IdentityControllers.java`
- API base: `/api/v1/auth`, `/api/v1/authorization`, `/api/v1/directory`, `/api/v1/roles`, `/api/v1/users`, `/api/v1/users/{userId}/sessions`, `/internal/v1/authorization`, `/internal/v1/users`
- Bảng sở hữu: `authorization_grants`, `bulk_operations`, `identity_system_locks`, `password_history`, `refresh_tokens`, `role_permissions`, `roles`, `scoped_role_assignments`, `user_accounts`, `user_roles`
- Flyway: `identity-service/src/main/resources/db/migration`

## `organization-service` — port 8082

Cây cơ cấu tổ chức, đơn vị và quan hệ thành viên.

- PostgreSQL schema: `organization`
- Controller Java: `InternalOrganizationScopeController.java`, `InternalOrganizationUnitController.java`, `OrganizationController.java`, `OrganizationMembershipController.java`
- API base: `/api/v1/organization/memberships`, `/api/v1/organization/units`, `/internal/v1/organization`, `/internal/v1/organization/units`
- Bảng sở hữu: `organization_memberships_v2`, `organization_scope_projection`, `organization_units`
- Flyway: `organization-service/src/main/resources/db/migration`

## `course-service` — port 8083

Khóa học, danh mục, chương/bài học, phiên bản xuất bản và thảo luận.

- PostgreSQL schema: `course`
- Controller Java: `CategoryController.java`, `CourseController.java`, `DiscussionApi.java`, `InternalCourseController.java`
- API base: `/api/v1/categories`, `/api/v1/courses`, `/api/v1/discussions`, `/internal/v1/courses`
- Bảng sở hữu: `course_categories`, `course_document_links`, `course_versions`, `courses`, `demo_seed_history`, `discussion_posts`, `discussion_threads`, `lessons`
- Flyway: `course-service/src/main/resources/db/migration`

## `enrollment-service` — port 8084

Giao khóa học, ghi danh, lộ trình học và phiên học trực tuyến.

- PostgreSQL schema: `enrollment`
- Controller Java: `AssignmentAndLiveApi.java`, `EnrollmentApi.java`, `LearningPathApi.java`
- API base: `/api/v1/course-assignments`, `/api/v1/enrollments`, `/api/v1/learning-paths`, `/api/v1/live-sessions`, `/internal/v1/course-access`, `/internal/v1/enrollments`
- Bảng sở hữu: `class_instructors`, `course_assignments_v2`, `course_cohorts_v2`, `enrollments`, `learning_path_assignments`, `learning_path_items`, `learning_paths`, `live_sessions`, `training_classes`, `user_learning_paths`
- Flyway: `enrollment-service/src/main/resources/db/migration`

## `learning-service` — port 8085

Tiến độ học, hoàn thành bài học, nộp/chấm bài thực hành và xAPI.

- PostgreSQL schema: `learning`
- Controller Java: `AssignmentSubmissionApi.java`, `LearningApi.java`, `XapiApi.java`
- API base: `/api/v1/learning`, `/api/v1/learning/assignments`, `/api/v1/xapi/statements`, `/internal/v1/learning`
- Bảng sở hữu: `assignment_submissions`, `course_progress`, `idempotency_records`, `lesson_progress`, `xapi_statements`
- Flyway: `learning-service/src/main/resources/db/migration`

## `assessment-service` — port 8086

Ngân hàng câu hỏi, bài kiểm tra trong khóa học, bài thi độc lập, phiên làm bài và cuộc thi.

- PostgreSQL schema: `assessment`
- Controller Java: `AssessmentControllers.java`
- API base: `/api/v1/assessment-assignments`, `/api/v1/competitions`, `/api/v1/exam-sessions`, `/api/v1/exams`, `/api/v1/questions`, `/internal/v1/assessment`
- Bảng sở hữu: `assessment_assignments`, `assessment_contexts`, `competition_leaderboard`, `competition_rewards`, `competitions`, `demo_seed_history`, `exam_questions`, `exam_session_events`, `exam_sessions`, `exams`, `question_provenance`, `questions`, `reward_ledger`
- Flyway: `assessment-service/src/main/resources/db/migration`

## `grading-service` — port 8087

Chấm tự động, chấm thủ công, lịch sử điểm và phúc khảo.

- PostgreSQL schema: `grading`
- Controller Java: `GradeController.java`
- API base: `/api/v1/grades`
- Bảng sở hữu: `grade_appeals`, `grade_results`, `grade_revisions`
- Flyway: `grading-service/src/main/resources/db/migration`

## `reporting-service` — port 8088

Read model báo cáo, dashboard, KPI, xuất báo cáo và lịch báo cáo.

- PostgreSQL schema: `reporting`
- Controller Java: `InternalReminderReportingController.java`, `KpiReportingController.java`, `ReportingController.java`, `ScheduledReportingController.java`
- API base: `/api/v1/reports`, `/api/v1/reports/kpis`, `/internal/v1/reports/reminders`
- Bảng sở hữu: `learner_course_read_model`, `report_events`, `report_export_jobs`, `report_schedules`
- Flyway: `reporting-service/src/main/resources/db/migration`

## `file-storage-service` — port 8089

Lưu trữ file, quyền truy cập, phiên bản, xem PDF/DOCX/video và phiên chỉnh sửa.

- PostgreSQL schema: `file_storage`
- Controller Java: `FileEditingApi.java`, `FileStorageApi.java`
- API base: `/api/v1/files`, `/internal/v1/files`, `/public/v1/file-edit`
- Bảng sở hữu: `demo_seed_history`, `file_access_grants`, `file_edit_sessions`, `file_versions_v2`, `stored_files`
- Flyway: `file-storage-service/src/main/resources/db/migration`

## `license-service` — port 8090

Kích hoạt giấy phép, entitlement và giới hạn tính năng.

- PostgreSQL schema: `license`
- Controller Java: `InternalLicenseController.java`, `LicenseController.java`
- API base: `/api/v1/license`, `/internal/v1/license`
- Bảng sở hữu: `licenses`
- Flyway: `license-service/src/main/resources/db/migration`

## `audit-service` — port 8091

Nhật ký kiểm toán và xuất dữ liệu kiểm toán.

- PostgreSQL schema: `audit`
- Controller Java: `AuditController.java`, `InternalAuditController.java`
- API base: `/api/v1/audit`, `/internal/v1/audit`
- Bảng sở hữu: `audit_entries`
- Flyway: `audit-service/src/main/resources/db/migration`

## `notification-service` — port 8092

Thông báo, email outbox, tin tức, template và nhắc hạn.

- PostgreSQL schema: `notification`
- Controller Java: `NewsApi.java`, `NotificationApi.java`, `NotificationAutomationApi.java`
- API base: `/api/v1/news`, `/api/v1/notifications`, `/api/v1/notifications/reminder-rules`, `/api/v1/notifications/templates`
- Bảng sở hữu: `news_articles`, `news_attachments`, `news_receipts`, `notification_reminder_dispatches`, `notification_reminder_rules`, `notification_templates`, `notifications`
- Flyway: `notification-service/src/main/resources/db/migration`

## `certificate-service` — port 8093

Mẫu chứng chỉ, cấp, tra cứu, thu hồi và cấp lại.

- PostgreSQL schema: `certificate`
- Controller Java: `CertificateController.java`, `PublicCertificateController.java`
- API base: `/api/v1/certificates`, `/public/v1/certificates`
- Bảng sở hữu: `certificate_templates`, `certificates`
- Flyway: `certificate-service/src/main/resources/db/migration`

## `ai-service` — port 8094

Model OpenAI-compatible; trích xuất PDF/DOCX; sinh, kiểm tra, review và import câu hỏi theo độ khó.

- PostgreSQL schema: `ai`
- Controller Java: `AiApi.java`, `QuestionGenerationApi.java`
- API base: `/api/v1/ai`
- Bảng sở hữu: `ai_provider_configs`, `question_generation_jobs`, `question_generation_reviews`
- Flyway: `ai-service/src/main/resources/db/migration`

## `configuration-service` — port 8095

Thông tin hệ thống, thương hiệu, logo, ảnh nền đăng nhập và dịch vụ ngoài.

- PostgreSQL schema: `configuration`
- Controller Java: `CustomizationController.java`, `ProductConfigurationController.java`
- API base: `/api/v1/branding`, `/api/v1/configuration`, `/api/v1/external-services`, `/api/v1/external-services/{id}`, `/api/v1/external-services/{id}/test`, `/public/v1/branding`, `/public/v1/branding/assets/{kind}`, `/public/v1/configuration`
- Bảng sở hữu: `branding_profiles`, `external_service_configs`, `product_configuration`
- Flyway: `configuration-service/src/main/resources/db/migration`

## `integration-service` — port 8096

Adapter kết nối SMTP, Redis, S3, ONLYOFFICE/Collabora, họp trực tuyến và dịch vụ ngoài.

- PostgreSQL schema: `integration`
- Controller Java: `IntegrationController.java`
- API base: `/api/v1/integrations`
- Bảng sở hữu: `integration_adapters`
- Flyway: `integration-service/src/main/resources/db/migration`

## `operations-service` — port 8097

Health tổng hợp, job vận hành, lịch chạy và agent lease.

- PostgreSQL schema: `operations`
- Controller Java: `InternalOperationsAgentController.java`, `OperationsController.java`
- API base: `/api/v1/operations`, `/internal/v1/operations/jobs`
- Bảng sở hữu: `operation_jobs`, `operation_schedules`
- Flyway: `operations-service/src/main/resources/db/migration`

## `competency-service` — port 8098

Khung năng lực, hồ sơ, khoảng thiếu và ánh xạ khóa học.

- PostgreSQL schema: `competency`
- Controller Java: `CompetencyController.java`
- API base: `/api/v1/competencies`
- Bảng sở hữu: `competencies`, `competency_profile_requirements`, `competency_profiles`, `course_competency_maps`, `user_competency_assessments`, `user_competency_profiles`
- Flyway: `competency-service/src/main/resources/db/migration`
