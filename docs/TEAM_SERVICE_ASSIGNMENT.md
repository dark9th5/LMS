# Bảng phân công service LMSPilot

Điền tên thành viên vào cột Owner trước khi bắt đầu sprint. Một người có thể nhận nhiều service nhỏ, nhưng một service chỉ nên có một owner chính trong cùng sprint.

| Service | Port | Owner | Reviewer | Mục tiêu sprint | Branch/Issue | Trạng thái |
|---|---:|---|---|---|---|---|
| `apps/web` | 3000 |  |  | Frontend/UI/UX, accessibility, browser E2E |  | Chưa nhận |
| `api-gateway` | 8080 |  |  | Security, routing, rate limit, integration test |  | Chưa nhận |
| `identity-service` | 8081 |  |  | Auth, account security, session, role isolation |  | Chưa nhận |
| `organization-service` | 8082 |  |  | Organization tree, membership, scope query |  | Chưa nhận |
| `course-service` | 8083 |  |  | Course authoring, publishing, content versioning |  | Chưa nhận |
| `enrollment-service` | 8084 |  |  | Assignment, enrollment, deadline, learning path |  | Chưa nhận |
| `learning-service` | 8085 |  |  | Learning player, progress, assignment submission |  | Chưa nhận |
| `assessment-service` | 8086 |  |  | Exam lifecycle, autosave, anti-loss, objective scoring |  | Chưa nhận |
| `grading-service` | 8087 |  |  | Grading queue, revisions, appeal workflow |  | Chưa nhận |
| `reporting-service` | 8088 |  |  | Projection, KPI, export, performance |  | Chưa nhận |
| `file-storage-service` | 8089 |  |  | Upload security, preview, versioning, editor integration |  | Chưa nhận |
| `license-service` | 8090 |  |  | License validation, grace period, tamper resistance |  | Chưa nhận |
| `audit-service` | 8091 |  |  | Immutable audit, retention, export |  | Chưa nhận |
| `notification-service` | 8092 |  |  | Outbox, retry, templates, reminders |  | Chưa nhận |
| `certificate-service` | 8093 |  |  | Certificate lifecycle, verification, template rendering |  | Chưa nhận |
| `ai-service` | 8094 |  |  | Prompt/schema, grounding, quality validator, provider adapters |  | Chưa nhận |
| `configuration-service` | 8095 |  |  | Branding, public config, secret-safe settings |  | Chưa nhận |
| `integration-service` | 8096 |  |  | Adapters, health test, credentials, resilience |  | Chưa nhận |
| `operations-service` | 8097 |  |  | Health, scheduled jobs, backup/restore orchestration |  | Chưa nhận |
| `competency-service` | 8098 |  |  | Competency framework, gap analysis, course mapping |  | Chưa nhận |

## Gợi ý chia nhóm

- **Nhóm nền tảng:** API Gateway, Identity, Configuration, Integration, Operations, Audit, License.
- **Nhóm học tập:** Course, Enrollment, Learning, File Storage, Certificate.
- **Nhóm đánh giá:** Assessment, Grading, AI.
- **Nhóm dữ liệu:** Reporting, Notification, Organization, Competency.
- **Frontend:** `apps/web`, phối hợp với owner service để chốt API contract.

## Checklist khi nhận service

- Đọc README của service và toàn bộ migration.
- Liệt kê endpoint đang có và quyền yêu cầu.
- Chạy test module trước khi sửa.
- Tạo Issue/Epic rõ phạm vi.
- Thêm test cho bug hoặc tính năng mới.
- Cập nhật README, API/DB map khi contract thay đổi.
