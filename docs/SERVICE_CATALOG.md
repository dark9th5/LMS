# Danh mục service LMSPilot 0.21.0

Bảng này là nguồn tham chiếu để chia việc, nâng cấp và kiểm thử.

| Port | Service | Trách nhiệm | API base | Schema | Controller Java | Dependency module |
|---:|---|---|---|---|---|---|
| 8080 | `api-gateway` | Điểm vào duy nhất của frontend; xác thực JWT, rate limit, correlation ID và định tuyến API. | `—` | `—` | `—` | `:platform-contracts` |
| 8081 | `identity-service` | Đăng nhập, JWT/refresh token, phiên, tài khoản, vai trò độc quyền ADMIN/INSTRUCTOR/STUDENT và quyền theo phạm vi. | `/api/v1/auth`, `/api/v1/authorization`, `/api/v1/directory`, `/api/v1/roles`, `/api/v1/users`, `/api/v1/users/{userId}/sessions`, `/internal/v1/authorization`, `/internal/v1/users` | `identity` | `IdentityControllers.java` | `:service-support` |
| 8082 | `organization-service` | Cây cơ cấu tổ chức, đơn vị và quan hệ thành viên. | `/api/v1/organization/memberships`, `/api/v1/organization/units`, `/internal/v1/organization`, `/internal/v1/organization/units` | `organization` | `InternalOrganizationScopeController.java`, `InternalOrganizationUnitController.java`, `OrganizationController.java`, `OrganizationMembershipController.java` | `:service-support` |
| 8083 | `course-service` | Khóa học, danh mục, chương/bài học, phiên bản xuất bản và thảo luận. | `/api/v1/categories`, `/api/v1/courses`, `/api/v1/discussions`, `/internal/v1/courses` | `course` | `CategoryController.java`, `CourseController.java`, `DiscussionApi.java`, `InternalCourseController.java` | `:service-support` |
| 8084 | `enrollment-service` | Giao khóa học, ghi danh, lộ trình học và phiên học trực tuyến. | `/api/v1/course-assignments`, `/api/v1/enrollments`, `/api/v1/learning-paths`, `/api/v1/live-sessions`, `/internal/v1/course-access`, `/internal/v1/enrollments` | `enrollment` | `AssignmentAndLiveApi.java`, `EnrollmentApi.java`, `LearningPathApi.java` | `:service-support` |
| 8085 | `learning-service` | Tiến độ học, hoàn thành bài học, nộp/chấm bài thực hành và xAPI. | `/api/v1/learning`, `/api/v1/learning/assignments`, `/api/v1/xapi/statements`, `/internal/v1/learning` | `learning` | `AssignmentSubmissionApi.java`, `LearningApi.java`, `XapiApi.java` | `:service-support` |
| 8086 | `assessment-service` | Ngân hàng câu hỏi, bài kiểm tra trong khóa học, bài thi độc lập, phiên làm bài và cuộc thi. | `/api/v1/assessment-assignments`, `/api/v1/competitions`, `/api/v1/exam-sessions`, `/api/v1/exams`, `/api/v1/questions`, `/internal/v1/assessment` | `assessment` | `AssessmentControllers.java` | `:service-support` |
| 8087 | `grading-service` | Chấm tự động, chấm thủ công, lịch sử điểm và phúc khảo. | `/api/v1/grades` | `grading` | `GradeController.java` | `:service-support` |
| 8088 | `reporting-service` | Read model báo cáo, dashboard, KPI, xuất báo cáo và lịch báo cáo. | `/api/v1/reports`, `/api/v1/reports/kpis`, `/internal/v1/reports/reminders` | `reporting` | `InternalReminderReportingController.java`, `KpiReportingController.java`, `ReportingController.java`, `ScheduledReportingController.java` | `:service-support` |
| 8089 | `file-storage-service` | Lưu trữ file, quyền truy cập, phiên bản, xem PDF/DOCX/video và phiên chỉnh sửa. | `/api/v1/files`, `/internal/v1/files`, `/public/v1/file-edit` | `file_storage` | `FileEditingApi.java`, `FileStorageApi.java` | `:service-support` |
| 8090 | `license-service` | Kích hoạt giấy phép, entitlement và giới hạn tính năng. | `/api/v1/license`, `/internal/v1/license` | `license` | `InternalLicenseController.java`, `LicenseController.java` | `:service-support` |
| 8091 | `audit-service` | Nhật ký kiểm toán và xuất dữ liệu kiểm toán. | `/api/v1/audit`, `/internal/v1/audit` | `audit` | `AuditController.java`, `InternalAuditController.java` | `:service-support` |
| 8092 | `notification-service` | Thông báo, email outbox, tin tức, template và nhắc hạn. | `/api/v1/news`, `/api/v1/notifications`, `/api/v1/notifications/reminder-rules`, `/api/v1/notifications/templates` | `notification` | `NewsApi.java`, `NotificationApi.java`, `NotificationAutomationApi.java` | `:service-support` |
| 8093 | `certificate-service` | Mẫu chứng chỉ, cấp, tra cứu, thu hồi và cấp lại. | `/api/v1/certificates`, `/public/v1/certificates` | `certificate` | `CertificateController.java`, `PublicCertificateController.java` | `:service-support` |
| 8094 | `ai-service` | Model OpenAI-compatible; trích xuất PDF/DOCX; sinh, kiểm tra, review và import câu hỏi theo độ khó. | `/api/v1/ai` | `ai` | `AiApi.java`, `QuestionGenerationApi.java` | `:service-support` |
| 8095 | `configuration-service` | Thông tin hệ thống, thương hiệu, logo, ảnh nền đăng nhập và dịch vụ ngoài. | `/api/v1/branding`, `/api/v1/configuration`, `/api/v1/external-services`, `/api/v1/external-services/{id}`, `/api/v1/external-services/{id}/test`, `/public/v1/branding`, `/public/v1/branding/assets/{kind}`, `/public/v1/configuration` | `configuration` | `CustomizationController.java`, `ProductConfigurationController.java` | `:service-support` |
| 8096 | `integration-service` | Adapter kết nối SMTP, Redis, S3, ONLYOFFICE/Collabora, họp trực tuyến và dịch vụ ngoài. | `/api/v1/integrations` | `integration` | `IntegrationController.java` | `:service-support` |
| 8097 | `operations-service` | Health tổng hợp, job vận hành, lịch chạy và agent lease. | `/api/v1/operations`, `/internal/v1/operations/jobs` | `operations` | `InternalOperationsAgentController.java`, `OperationsController.java` | `:service-support` |
| 8098 | `competency-service` | Khung năng lực, hồ sơ, khoảng thiếu và ánh xạ khóa học. | `/api/v1/competencies` | `competency` | `CompetencyController.java` | `:service-support` |

## Quy tắc ownership

- Một owner chính, một reviewer dự phòng cho mỗi service.
- Owner chịu trách nhiệm API, migration, unit test, integration test và tài liệu của service.
- Thay đổi contract liên service cần owner của cả producer và consumer review.
- Không giao cùng một bảng database cho hai service.
