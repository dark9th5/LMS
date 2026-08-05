# Danh mục service LMSPilot

Mỗi service có một thư mục, một port và một schema dữ liệu riêng. Cột **Owner** được để trống để nhóm điền người phụ trách.

| Port | Service | Nhóm | Owner | Phạm vi | API | DB schema | Phụ thuộc |
|---:|---|---|---|---|---|---|---|
| 3000 | `apps/web` | Frontend | `TBD` | Giao diện Next.js cho ba cổng Admin, Giảng viên và Học viên. | Chỉ gọi API Gateway qua `/api/**` hoặc URL gateway cấu hình. | `—` | API Gateway |
| 8080 | `api-gateway` | Gateway | `TBD` | Điểm vào duy nhất của frontend; xác thực JWT, rate limit, correlation ID và định tuyến. | `/api/v1/**`, `/public/v1/**` → các service nội bộ | `—` | Identity, Redis và toàn bộ service backend |
| 8081 | `identity-service` | Core | `TBD` | Đăng nhập, phiên, tài khoản và mô hình một tài khoản–một vai trò `ADMIN`/`INSTRUCTOR`/`STUDENT`. | `/api/v1/auth`, `/api/v1/users`, `/api/v1/roles`, `/api/v1/authorization`, `/api/v1/directory` | `identity` | PostgreSQL, RabbitMQ |
| 8082 | `organization-service` | Core | `TBD` | Cây cơ cấu tổ chức, đơn vị, quan hệ thành viên và phạm vi dữ liệu. | `/api/v1/organization/units`, `/api/v1/organization/memberships` | `organization` | PostgreSQL |
| 8083 | `course-service` | Learning | `TBD` | Khóa học, chương, bài học, tài liệu, video, bài thực hành, thảo luận và phiên bản xuất bản. | `/api/v1/categories`, `/api/v1/courses`, `/api/v1/discussions` | `course` | PostgreSQL, File Storage, Enrollment |
| 8084 | `enrollment-service` | Learning | `TBD` | Giao khóa học trực tiếp cho học viên, ghi danh, hạn học, phiên học trực tuyến và lộ trình học. | `/api/v1/enrollments`, `/api/v1/course-assignments`, `/api/v1/live-sessions`, `/api/v1/learning-paths` | `enrollment` | PostgreSQL, Course, Organization, Learning |
| 8085 | `learning-service` | Learning | `TBD` | Tiến độ học, mở nội dung, hoàn thành bài học, nộp/chấm bài thực hành và xAPI. | `/api/v1/learning`, `/api/v1/learning/assignments`, `/api/v1/xapi/statements` | `learning` | PostgreSQL, Course, Enrollment, File Storage |
| 8086 | `assessment-service` | Assessment | `TBD` | Ngân hàng câu hỏi, bài kiểm tra trong khóa học, bài thi độc lập, phiên làm bài và cuộc thi. | `/api/v1/questions`, `/api/v1/exams`, `/api/v1/exam-sessions`, `/api/v1/competitions`, `/api/v1/assessment-assignments` | `assessment` | PostgreSQL, Course, Enrollment, Organization |
| 8087 | `grading-service` | Assessment | `TBD` | Chấm tự động, chấm thủ công, lịch sử điểm, phản hồi và phúc khảo. | `/api/v1/grades`, `/api/v1/grading` | `grading` | PostgreSQL, Assessment |
| 8088 | `reporting-service` | Analytics | `TBD` | Read model, dashboard, KPI, báo cáo học tập và xuất báo cáo theo lịch. | `/api/v1/reports`, `/api/v1/dashboard`, `/internal/v1/reports/reminders` | `reporting` | PostgreSQL, RabbitMQ, Enrollment |
| 8089 | `file-storage-service` | Platform | `TBD` | Tải lên/tải xuống, quyền truy cập, phiên bản, xem PDF/DOCX/video và phiên chỉnh sửa. | `/api/v1/files`, `/internal/v1/files`, `/public/v1/file-edit` | `file_storage` | PostgreSQL, S3 tùy chọn, ONLYOFFICE/Collabora tùy chọn |
| 8090 | `license-service` | Platform | `TBD` | Kích hoạt giấy phép, entitlement và giới hạn tính năng triển khai. | `/api/v1/license` | `license` | PostgreSQL |
| 8091 | `audit-service` | Platform | `TBD` | Nhật ký kiểm toán bất biến và xuất dữ liệu kiểm toán. | `/api/v1/audit` | `audit` | PostgreSQL, RabbitMQ |
| 8092 | `notification-service` | Platform | `TBD` | Thông báo trong hệ thống, email, tin tức, mẫu và nhắc hạn. | `/api/v1/notifications`, `/api/v1/news` | `notification` | PostgreSQL, RabbitMQ, SMTP tùy chọn |
| 8093 | `certificate-service` | Learning | `TBD` | Mẫu chứng chỉ, cấp, tra cứu, in, thu hồi và cấp lại chứng chỉ. | `/api/v1/certificates`, `/public/v1/certificates` | `certificate` | PostgreSQL |
| 8094 | `ai-service` | AI | `TBD` | Cấu hình model OpenAI-compatible; trích xuất PDF/DOCX; sinh, kiểm tra và duyệt câu hỏi theo độ khó. | `/api/v1/ai` | `ai` | PostgreSQL, File Storage, Course, Assessment, AI provider |
| 8095 | `configuration-service` | Platform | `TBD` | Thông tin hệ thống, thương hiệu, logo, màu sắc, ảnh nền đăng nhập và cấu hình dịch vụ ngoài. | `/api/v1/configuration`, `/api/v1/branding`, `/api/v1/external-services`, `/public/v1/configuration`, `/public/v1/branding` | `configuration` | PostgreSQL, File Storage |
| 8096 | `integration-service` | Platform | `TBD` | Adapter cho Redis, SMTP, S3, ONLYOFFICE, họp trực tuyến và dịch vụ bên thứ ba. | `/api/v1/integrations` | `integration` | PostgreSQL và nhà cung cấp bên thứ ba |
| 8097 | `operations-service` | Operations | `TBD` | Health tổng hợp, tác vụ vận hành, agent lease và lịch chạy nội bộ. | `/api/v1/operations` | `operations` | PostgreSQL và các service nghiệp vụ |
| 8098 | `competency-service` | Optional | `TBD` | Khung năng lực, hồ sơ năng lực, đánh giá khoảng thiếu và ánh xạ khóa học. | `/api/v1/competencies` | `competency` | PostgreSQL, License |

## Quy tắc ownership

- Owner chịu trách nhiệm từ API đến database, migration, test, config và tài liệu.
- Mỗi thay đổi DB phải có Flyway migration mới; không sửa migration đã phát hành.
- Mọi API mới phải có authorization, validation, error contract và test.
- Không truy cập database service khác.
- PR phải được người không phải tác giả review.

## Lệnh kiểm tra

```bash
python scripts/validate-service-ports.py
cd backend && ./gradlew :services:<service-name>:test --no-daemon
```
