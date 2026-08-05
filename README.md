# LMSPilot 0.20.4

**LMSPilot** là nền tảng quản lý học tập on-premise theo kiến trúc microservice, dành cho doanh nghiệp, trường học và trung tâm đào tạo. Repository này là **full-source release candidate**, không phải bản vá và chưa được xem là production-certified cho đến khi hoàn tất build, migration, Docker smoke test, browser E2E, kiểm thử tải và UAT trên hạ tầng đích.

## 1. Mô hình người dùng

Mỗi tài khoản chỉ có đúng một vai trò:

| Vai trò | Phạm vi chức năng |
|---|---|
| `ADMIN` | Quản lý tài khoản, tổ chức, cấu hình hệ thống, thương hiệu, dịch vụ ngoài, báo cáo và vận hành. |
| `INSTRUCTOR` | Tạo và xuất bản khóa học, xây dựng bài học, tạo bài kiểm tra trong khóa học, tạo bài thi độc lập, giao học viên và chấm bài. |
| `STUDENT` | Học khóa được giao, xem video/PDF/DOCX, nộp bài thực hành, làm bài kiểm tra/bài thi và xem kết quả/chứng chỉ. |

Vai trò không dùng chung chức năng. Muốn thao tác ở cổng khác phải đăng nhập bằng tài khoản có vai trò tương ứng.

## 2. Mô hình nghiệp vụ

```text
Khóa học
├── Chương
│   ├── Video / âm thanh / nội dung đọc
│   ├── PDF / DOCX / tệp tải xuống
│   ├── Bài thực hành nộp trực tiếp
│   └── Bài kiểm tra thuộc khóa học
└── Học viên được giao trực tiếp

Bài thi
└── Kỳ thi độc lập, không thuộc khóa học
```

Hệ thống không có giao diện **Lớp học**. Một số bảng tương thích dữ liệu cũ vẫn tồn tại trong `enrollment-service`, nhưng không được coi là mô hình sản phẩm công khai.

## 3. Chức năng chính

- Quản trị tài khoản ba vai trò, phiên đăng nhập, chính sách mật khẩu và khóa tài khoản quản trị hệ thống.
- Cơ cấu tổ chức dạng cây, thành viên đơn vị và phạm vi dữ liệu.
- Biên soạn khóa học, chương, bài học, phiên bản nội dung và xuất bản.
- Xem video, PDF, DOCX; tải tệp; nộp và chấm bài thực hành trực tiếp trong khóa học.
- Bài kiểm tra nằm trong khóa học; bài thi độc lập có khu vực quản lý riêng.
- Ngân hàng câu hỏi, phiên làm bài có heartbeat/autosave, chấm tự động và chấm thủ công.
- AI sinh câu hỏi từ PDF/DOCX với lựa chọn Dễ/Trung bình/Khó/Hỗn hợp, kiểm tra citation, đáp án, lời giải và bước giảng viên duyệt trước khi import.
- Báo cáo quản trị, giảng dạy và học tập; xuất CSV và lịch xuất báo cáo.
- Chứng chỉ, thông báo, email, tin tức và nhắc hạn.
- Tùy chỉnh logo, tên **LMSPilot**, màu thương hiệu và ảnh nền đăng nhập.
- Tích hợp Redis, SMTP, S3, ONLYOFFICE/Collabora, model AI OpenAI-compatible và nền tảng họp trực tuyến.

## 4. Kiến trúc kỹ thuật

```text
Browser
  │
  ├── Next.js Web :3000
  │
  └── API Gateway :8080
        ├── Identity :8081
        ├── Organization :8082
        ├── Course :8083
        ├── Enrollment :8084
        ├── Learning :8085
        ├── Assessment :8086
        ├── Grading :8087
        ├── Reporting :8088
        ├── File Storage :8089
        ├── License :8090
        ├── Audit :8091
        ├── Notification :8092
        ├── Certificate :8093
        ├── AI :8094
        ├── Configuration :8095
        ├── Integration :8096
        ├── Operations :8097
        └── Competency :8098
```

- Frontend: Next.js 16, React 19, TypeScript 5.9.
- Backend: Kotlin 2.0, Spring Boot 3.5, Java 21.
- Database: một PostgreSQL database mặc định tên `lmspilot`, tách schema theo service.
- Messaging: RabbitMQ cho sự kiện và tác vụ bất đồng bộ.
- Redis: rate limit/cache tùy cấu hình.
- Mỗi service sở hữu API, schema, Flyway migration và test của chính nó; không truy cập trực tiếp database của service khác.

## 5. Danh mục service, port, API và database

| Port | Thành phần | Trách nhiệm | DB schema | API chính |
|---:|---|---|---|---|
| 3000 | `apps/web` | Giao diện Next.js cho ba cổng Admin, Giảng viên và Học viên. | `—` | Chỉ gọi API Gateway qua `/api/**` hoặc URL gateway cấu hình. |
| 8080 | `api-gateway` | Điểm vào duy nhất của frontend; xác thực JWT, rate limit, correlation ID và định tuyến. | `—` | `/api/v1/**`, `/public/v1/**` → các service nội bộ |
| 8081 | `identity-service` | Đăng nhập, phiên, tài khoản và mô hình một tài khoản–một vai trò `ADMIN`/`INSTRUCTOR`/`STUDENT`. | `identity` | `/api/v1/auth`, `/api/v1/users`, `/api/v1/roles`, `/api/v1/authorization`, `/api/v1/directory` |
| 8082 | `organization-service` | Cây cơ cấu tổ chức, đơn vị, quan hệ thành viên và phạm vi dữ liệu. | `organization` | `/api/v1/organization/units`, `/api/v1/organization/memberships` |
| 8083 | `course-service` | Khóa học, chương, bài học, tài liệu, video, bài thực hành, thảo luận và phiên bản xuất bản. | `course` | `/api/v1/categories`, `/api/v1/courses`, `/api/v1/discussions` |
| 8084 | `enrollment-service` | Giao khóa học trực tiếp cho học viên, ghi danh, hạn học, phiên học trực tuyến và lộ trình học. | `enrollment` | `/api/v1/enrollments`, `/api/v1/course-assignments`, `/api/v1/live-sessions`, `/api/v1/learning-paths` |
| 8085 | `learning-service` | Tiến độ học, mở nội dung, hoàn thành bài học, nộp/chấm bài thực hành và xAPI. | `learning` | `/api/v1/learning`, `/api/v1/learning/assignments`, `/api/v1/xapi/statements` |
| 8086 | `assessment-service` | Ngân hàng câu hỏi, bài kiểm tra trong khóa học, bài thi độc lập, phiên làm bài và cuộc thi. | `assessment` | `/api/v1/questions`, `/api/v1/exams`, `/api/v1/exam-sessions`, `/api/v1/competitions`, `/api/v1/assessment-assignments` |
| 8087 | `grading-service` | Chấm tự động, chấm thủ công, lịch sử điểm, phản hồi và phúc khảo. | `grading` | `/api/v1/grades`, `/api/v1/grading` |
| 8088 | `reporting-service` | Read model, dashboard, KPI, báo cáo học tập và xuất báo cáo theo lịch. | `reporting` | `/api/v1/reports`, `/api/v1/dashboard`, `/internal/v1/reports/reminders` |
| 8089 | `file-storage-service` | Tải lên/tải xuống, quyền truy cập, phiên bản, xem PDF/DOCX/video và phiên chỉnh sửa. | `file_storage` | `/api/v1/files`, `/internal/v1/files`, `/public/v1/file-edit` |
| 8090 | `license-service` | Kích hoạt giấy phép, entitlement và giới hạn tính năng triển khai. | `license` | `/api/v1/license` |
| 8091 | `audit-service` | Nhật ký kiểm toán bất biến và xuất dữ liệu kiểm toán. | `audit` | `/api/v1/audit` |
| 8092 | `notification-service` | Thông báo trong hệ thống, email, tin tức, mẫu và nhắc hạn. | `notification` | `/api/v1/notifications`, `/api/v1/news` |
| 8093 | `certificate-service` | Mẫu chứng chỉ, cấp, tra cứu, in, thu hồi và cấp lại chứng chỉ. | `certificate` | `/api/v1/certificates`, `/public/v1/certificates` |
| 8094 | `ai-service` | Cấu hình model OpenAI-compatible; trích xuất PDF/DOCX; sinh, kiểm tra và duyệt câu hỏi theo độ khó. | `ai` | `/api/v1/ai` |
| 8095 | `configuration-service` | Thông tin hệ thống, thương hiệu, logo, màu sắc, ảnh nền đăng nhập và cấu hình dịch vụ ngoài. | `configuration` | `/api/v1/configuration`, `/api/v1/branding`, `/api/v1/external-services`, `/public/v1/configuration`, `/public/v1/branding` |
| 8096 | `integration-service` | Adapter cho Redis, SMTP, S3, ONLYOFFICE, họp trực tuyến và dịch vụ bên thứ ba. | `integration` | `/api/v1/integrations` |
| 8097 | `operations-service` | Health tổng hợp, tác vụ vận hành, agent lease và lịch chạy nội bộ. | `operations` | `/api/v1/operations` |
| 8098 | `competency-service` | Khung năng lực, hồ sơ năng lực, đánh giá khoảng thiếu và ánh xạ khóa học. | `competency` | `/api/v1/competencies` |

Chi tiết endpoint, bảng dữ liệu, migration và controller xem tại:

- [`docs/SERVICE_CATALOG.md`](docs/SERVICE_CATALOG.md)
- [`docs/API_DATABASE_MAP.md`](docs/API_DATABASE_MAP.md)
- [`docs/TEAM_SERVICE_ASSIGNMENT.md`](docs/TEAM_SERVICE_ASSIGNMENT.md)
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)

## 6. Cấu trúc repository

```text
apps/web/                         Frontend cho ba vai trò
backend/platform-contracts/       Contract dùng chung giữa các service
backend/service-support/          Security, error model và hỗ trợ dùng chung
backend/services/<service>/       Mã nguồn, API, DB migration và test của từng service
contracts/lmspilot/               JSON Schema cho dữ liệu trao đổi, gồm QuestionSet AI
docs/                             Kiến trúc, API/DB, service catalog và hướng dẫn nhóm
deploy/                           Compose profile và cấu hình triển khai
infrastructure/                   PostgreSQL, RabbitMQ, Grafana, Prometheus
scripts/                          Setup, validate, smoke test, backup/restore
tests/                            Contract, security, repository và UI regression
```

## 7. Chạy nhanh

### Windows

```powershell
Copy-Item .env.example .env
powershell -ExecutionPolicy Bypass -File .\scripts\setup.ps1
```

### Linux/macOS

```bash
cp .env.example .env
chmod +x scripts/*.sh
./scripts/setup.sh
```

Mở `http://localhost:3000`. Demo seed có ba tài khoản `admin`, `instructor`, `student`; mật khẩu lấy từ `LMSPILOT_DEFAULT_ADMIN_PASSWORD`. Không bật demo seed ở staging hoặc production.

## 8. Chạy và test một service

Ví dụ với `course-service`:

```bash
cd backend
./gradlew :services:course-service:test --no-daemon
./gradlew :services:course-service:bootRun
```

Chạy toàn bộ kiểm tra repository:

```bash
python scripts/validate-repository.py
python scripts/validate-service-ports.py
node scripts/check-typescript-syntax.js
python -m unittest discover -s tests -p "test_*.py"

cd apps/web
npm ci
npm run typecheck
npm run build

cd ../../backend
./gradlew test --no-daemon
```

## 9. Quy tắc phân công service

1. Mỗi thành viên chọn một service tại `backend/services/<service-name>` và ghi owner vào [`docs/TEAM_SERVICE_ASSIGNMENT.md`](docs/TEAM_SERVICE_ASSIGNMENT.md).
2. Owner chịu trách nhiệm nghiệp vụ, API, DB migration, validation, unit/integration test, tài liệu và Docker/config của service đó.
3. Không code trực tiếp trên `main`; mỗi Issue dùng branch riêng như `feature/course-publishing` hoặc `test/assessment-resume-session`.
4. Không tự approve pull request của chính mình; thay đổi auth, API contract hoặc DB ownership phải được Tech Lead duyệt.
5. Không gọi database của service khác. Dùng API nội bộ có `X-Service-Token` hoặc RabbitMQ.
6. Trước pull request phải chạy test module, validator port và `git diff --check`.

## 10. Tài liệu nên đọc theo thứ tự

1. [`BAT_DAU_TAI_DAY.md`](BAT_DAU_TAI_DAY.md)
2. [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
3. [`docs/SERVICE_CATALOG.md`](docs/SERVICE_CATALOG.md)
4. [`docs/API_DATABASE_MAP.md`](docs/API_DATABASE_MAP.md)
5. [`docs/TEAM_SERVICE_ASSIGNMENT.md`](docs/TEAM_SERVICE_ASSIGNMENT.md)
6. README riêng trong từng `backend/services/<service-name>/README.md`
7. [`DELIVERY_STATUS.md`](DELIVERY_STATUS.md) và [`TEST_RESULTS_LMSPILOT_0.20.4.md`](TEST_RESULTS_LMSPILOT_0.20.4.md)

## 11. Trước khi triển khai production

Bắt buộc chạy fresh/upgrade migration trên bản sao dữ liệu, full frontend/backend build, Docker smoke, browser E2E cho cả ba vai trò, kiểm thử video/PDF/DOCX/bài thực hành/bài kiểm tra/bài thi, UAT với model AI thật, accessibility, tải đồng thời kỳ thi, backup/restore, TLS/domain, SMTP và pentest.
