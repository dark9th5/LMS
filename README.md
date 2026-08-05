# LMSPilot 0.21.0

**LMSPilot** là nền tảng quản lý học tập theo kiến trúc microservice, phục vụ doanh nghiệp, trường học và trung tâm đào tạo. Repository là monorepo gồm frontend Next.js và 19 backend service Java Spring Boot có thể phát triển, nâng cấp và kiểm thử độc lập.

## Công nghệ

- Frontend: Next.js 16, React 19, TypeScript 5.9 — port `3000`.
- Backend: **Java 21 + Spring Boot 3.5.16**.
- Build backend: Gradle 8.14.5, Gradle Kotlin DSL (`*.gradle.kts`). Các file `.kts` chỉ là cấu hình build; mã ứng dụng backend nằm hoàn toàn trong `src/main/java`.
- Database: PostgreSQL; mỗi service sở hữu schema và Flyway migration riêng.
- Messaging: RabbitMQ; Redis là thành phần tùy chọn cho cache/rate limit.
- File/integration: S3-compatible storage, SMTP, ONLYOFFICE/Collabora, OpenAI-compatible AI provider và meeting provider.

## Vai trò sản phẩm

Mỗi tài khoản chỉ có đúng một vai trò:

| Vai trò | Chức năng chính |
|---|---|
| `ADMIN` | Quản trị tài khoản, tổ chức, thương hiệu, dịch vụ ngoài, báo cáo, giấy phép và vận hành. |
| `INSTRUCTOR` | Biên soạn khóa học, tài liệu, bài thực hành, bài kiểm tra trong khóa học, bài thi độc lập và chấm điểm. |
| `STUDENT` | Học khóa được giao, xem video/PDF/DOCX, nộp bài thực hành, làm bài kiểm tra/bài thi và xem kết quả. |

Không có giao diện **Lớp học**. Học viên được giao trực tiếp vào khóa học. Bài kiểm tra thuộc khóa học; bài thi là nghiệp vụ độc lập.

## Chức năng chính

- Quản lý tài khoản ba vai trò, JWT/refresh token, phiên đăng nhập, khóa tài khoản và chính sách mật khẩu.
- Cơ cấu tổ chức dạng cây, thành viên và phạm vi dữ liệu.
- Khóa học, chương/bài học, video, PDF, DOCX, file tải xuống, bài thực hành và thảo luận.
- Tiến độ học, hoàn thành bài học, nộp/chấm bài thực hành và xAPI.
- Ngân hàng câu hỏi, bài kiểm tra trong khóa học, bài thi độc lập, autosave/heartbeat và chấm điểm.
- AI sinh câu hỏi từ PDF/DOCX theo Dễ/Trung bình/Khó/Hỗn hợp; kiểm tra schema, đáp án, trùng lặp, phân bố độ khó và citation; giảng viên review trước khi import.
- Báo cáo, KPI, CSV, lịch báo cáo, thông báo, email, chứng chỉ và audit.
- Logo, tên hệ thống, màu thương hiệu, tên miền và ảnh nền đăng nhập.

## Sơ đồ runtime

```text
Browser :3000
    |
API Gateway :8080
    |-- Identity :8081        |-- Organization :8082
    |-- Course :8083          |-- Enrollment :8084
    |-- Learning :8085        |-- Assessment :8086
    |-- Grading :8087         |-- Reporting :8088
    |-- File Storage :8089    |-- License :8090
    |-- Audit :8091           |-- Notification :8092
    |-- Certificate :8093     |-- AI :8094
    |-- Configuration :8095   |-- Integration :8096
    |-- Operations :8097      `-- Competency :8098
```

## Service, port, API và database

| Port | Service | Phạm vi sở hữu | PostgreSQL schema | API base |
|---:|---|---|---|---|
| 8080 | `api-gateway` | Điểm vào duy nhất của frontend; xác thực JWT, rate limit, correlation ID và định tuyến API. | `—` | `—` |
| 8081 | `identity-service` | Đăng nhập, JWT/refresh token, phiên, tài khoản, vai trò độc quyền ADMIN/INSTRUCTOR/STUDENT và quyền theo phạm vi. | `identity` | `/api/v1/auth`, `/api/v1/authorization`, `/api/v1/directory`, `/api/v1/roles`, `/api/v1/users`, `/api/v1/users/{userId}/sessions`, `/internal/v1/authorization`, `/internal/v1/users` |
| 8082 | `organization-service` | Cây cơ cấu tổ chức, đơn vị và quan hệ thành viên. | `organization` | `/api/v1/organization/memberships`, `/api/v1/organization/units`, `/internal/v1/organization`, `/internal/v1/organization/units` |
| 8083 | `course-service` | Khóa học, danh mục, chương/bài học, phiên bản xuất bản và thảo luận. | `course` | `/api/v1/categories`, `/api/v1/courses`, `/api/v1/discussions`, `/internal/v1/courses` |
| 8084 | `enrollment-service` | Giao khóa học, ghi danh, lộ trình học và phiên học trực tuyến. | `enrollment` | `/api/v1/course-assignments`, `/api/v1/enrollments`, `/api/v1/learning-paths`, `/api/v1/live-sessions`, `/internal/v1/course-access`, `/internal/v1/enrollments` |
| 8085 | `learning-service` | Tiến độ học, hoàn thành bài học, nộp/chấm bài thực hành và xAPI. | `learning` | `/api/v1/learning`, `/api/v1/learning/assignments`, `/api/v1/xapi/statements`, `/internal/v1/learning` |
| 8086 | `assessment-service` | Ngân hàng câu hỏi, bài kiểm tra trong khóa học, bài thi độc lập, phiên làm bài và cuộc thi. | `assessment` | `/api/v1/assessment-assignments`, `/api/v1/competitions`, `/api/v1/exam-sessions`, `/api/v1/exams`, `/api/v1/questions`, `/internal/v1/assessment` |
| 8087 | `grading-service` | Chấm tự động, chấm thủ công, lịch sử điểm và phúc khảo. | `grading` | `/api/v1/grades` |
| 8088 | `reporting-service` | Read model báo cáo, dashboard, KPI, xuất báo cáo và lịch báo cáo. | `reporting` | `/api/v1/reports`, `/api/v1/reports/kpis`, `/internal/v1/reports/reminders` |
| 8089 | `file-storage-service` | Lưu trữ file, quyền truy cập, phiên bản, xem PDF/DOCX/video và phiên chỉnh sửa. | `file_storage` | `/api/v1/files`, `/internal/v1/files`, `/public/v1/file-edit` |
| 8090 | `license-service` | Kích hoạt giấy phép, entitlement và giới hạn tính năng. | `license` | `/api/v1/license`, `/internal/v1/license` |
| 8091 | `audit-service` | Nhật ký kiểm toán và xuất dữ liệu kiểm toán. | `audit` | `/api/v1/audit`, `/internal/v1/audit` |
| 8092 | `notification-service` | Thông báo, email outbox, tin tức, template và nhắc hạn. | `notification` | `/api/v1/news`, `/api/v1/notifications`, `/api/v1/notifications/reminder-rules`, `/api/v1/notifications/templates` |
| 8093 | `certificate-service` | Mẫu chứng chỉ, cấp, tra cứu, thu hồi và cấp lại. | `certificate` | `/api/v1/certificates`, `/public/v1/certificates` |
| 8094 | `ai-service` | Model OpenAI-compatible; trích xuất PDF/DOCX; sinh, kiểm tra, review và import câu hỏi theo độ khó. | `ai` | `/api/v1/ai` |
| 8095 | `configuration-service` | Thông tin hệ thống, thương hiệu, logo, ảnh nền đăng nhập và dịch vụ ngoài. | `configuration` | `/api/v1/branding`, `/api/v1/configuration`, `/api/v1/external-services`, `/api/v1/external-services/{id}`, `/api/v1/external-services/{id}/test`, `/public/v1/branding`, `/public/v1/branding/assets/{kind}`, `/public/v1/configuration` |
| 8096 | `integration-service` | Adapter kết nối SMTP, Redis, S3, ONLYOFFICE/Collabora, họp trực tuyến và dịch vụ ngoài. | `integration` | `/api/v1/integrations` |
| 8097 | `operations-service` | Health tổng hợp, job vận hành, lịch chạy và agent lease. | `operations` | `/api/v1/operations`, `/internal/v1/operations/jobs` |
| 8098 | `competency-service` | Khung năng lực, hồ sơ, khoảng thiếu và ánh xạ khóa học. | `competency` | `/api/v1/competencies` |

Chi tiết đầy đủ:

- [`docs/SERVICE_CATALOG.md`](docs/SERVICE_CATALOG.md): phạm vi, dependency, controller, port và cách chạy từng service.
- [`docs/API_DATABASE_MAP.md`](docs/API_DATABASE_MAP.md): API base, controller, schema, bảng và migration tương ứng.
- [`docs/TEAM_SERVICE_ASSIGNMENT.md`](docs/TEAM_SERVICE_ASSIGNMENT.md): bảng để thành viên đăng ký owner/reviewer và phạm vi nâng cấp/test.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): nguyên tắc microservice và giao tiếp liên service.
- [`docs/JAVA_SPRING_MIGRATION_0.21.0.md`](docs/JAVA_SPRING_MIGRATION_0.21.0.md): phạm vi chuyển đổi Kotlin sang Java.

## Cấu trúc repository

```text
apps/web/                         Frontend Next.js
backend/platform-contracts/       Contract Java dùng chung
backend/service-support/          Security, error model, event và web support
backend/services/<service>/       19 Spring Boot service
contracts/lmspilot/               JSON Schema và contract ngoài tiến trình
infrastructure/                   PostgreSQL, RabbitMQ, Redis, monitoring
deploy/                           Docker và biến môi trường mẫu
docs/                             Tài liệu hệ thống hiện hành
scripts/                          Validator, backup/restore và operations agent
tests/                            Regression/contract tests
```

Mỗi service có cấu trúc:

```text
backend/services/<service>/
├── README.md
├── build.gradle.kts
└── src
    ├── main
    │   ├── java/com/lmspilot/...
    │   └── resources
    │       ├── application.yml
    │       └── db/migration/V*__*.sql
    └── test/java/com/lmspilot/...
```

## Hướng dẫn cài đặt và khởi chạy hệ thống (Dành cho người mới / Clone repository)

### 1. Yêu cầu môi trường (Prerequisites)

- **Git** (đã cài đặt trên máy).
- **Docker Desktop** (Windows/macOS) hoặc **Docker Engine & Docker Compose v2** (Linux).
- *(Tùy chọn cho Developer)*: Java 21 JDK và Node.js 20+ nếu muốn phát triển/chạy từng microservice cục bộ mà không dùng Docker.

---

### 2. Các bước khởi chạy toàn bộ hệ thống bằng Docker Compose (Khuyên dùng)

#### Bước 1: Clone Repository về máy cục bộ
```bash
git clone https://github.com/dark9th5/LMS.git LMSPilot
cd LMSPilot
```

#### Bước 2: Tạo tệp cấu hình môi trường `.env`
Sao chép tệp mẫu `.env.example` thành `.env`:
- Trên Linux / macOS / Git Bash:
  ```bash
  cp .env.example .env
  ```
- Trên Windows PowerShell:
  ```powershell
  Copy-Item .env.example .env
  ```

#### Bước 3: Khởi chạy Docker Desktop
Đảm bảo ứng dụng **Docker Desktop** đã được mở và chuyển sang trạng thái **Engine Running** (màu xanh).

#### Bước 4: Build và khởi chạy toàn bộ 22 container dịch vụ
```bash
docker compose up -d --build
```
> **Lưu ý**: Nếu bạn từng chạy các phiên bản cũ và gặp lỗi dữ liệu database cũ xung đột, hãy reset sạch volume và rebuild lại bằng lệnh:
> ```bash
> docker compose down -v
> docker compose up -d --build
> ```

#### Bước 5: Kiểm tra trạng thái container
```bash
docker compose ps
```
Khi toàn bộ các dịch vụ hiển thị trạng thái `Up` hoặc `healthy` là hệ thống đã sẵn sàng sử dụng.

---

### 3. Địa chỉ truy cập & Tài khoản đăng nhập thử nghiệm

- **Giao diện người dùng Web Frontend**: [http://localhost:3000](http://localhost:3000)
- **API Gateway**: [http://localhost:8080](http://localhost:8080)
- **RabbitMQ Management**: [http://localhost:15672](http://localhost:15672) *(Mặc định: guest / guest hoặc theo `.env`)*

#### Tài khoản đăng nhập mặc định:
Hệ thống áp dụng mô hình một tài khoản–một vai trò độc quyền:

| Cổng đăng nhập | Username | Mật khẩu mặc định | Vai trò |
|---|---|---|---|
| **Quản trị (Admin)** | `admin` | `admin123` | `ADMIN` |
| **Giảng viên (Instructor)** | `instructor` | `instructor123` | `INSTRUCTOR` |
| **Học viên (Student)** | `student` | `student123` | `STUDENT` |

---

### 4. Hướng dẫn dành cho Developer (Chạy & kiểm thử cục bộ)

#### Chạy toàn bộ backend JARs bằng Gradle:
```bash
cd backend
./gradlew bootJar -x test
```

#### Chạy một backend microservice độc lập:
```bash
cd backend
./gradlew :services:course-service:bootRun
```

#### Chạy tests cho toàn bộ backend:
```bash
cd backend
./gradlew test
```

#### Chạy frontend Next.js ở chế độ Development:
```bash
cd apps/web
npm ci
npm run dev
```
Giao diện dev sẽ lắng nghe tại [http://localhost:3000](http://localhost:3000).


## Quy tắc làm việc theo service

1. Mỗi service có một owner chính và ít nhất một reviewer.
2. Owner chỉ thay đổi schema của service mình; không đọc/ghi trực tiếp bảng của service khác.
3. Giao tiếp đồng bộ qua API Gateway hoặc internal API có `X-Service-Token`; giao tiếp bất đồng bộ qua event contract.
4. Thay đổi API phải cập nhật controller/DTO, `docs/API_DATABASE_MAP.md`, contract và consumer test.
5. Thay đổi database phải thêm Flyway migration mới; không sửa migration đã phát hành.
6. Pull request phải có unit test, test API chính, migration test và mô tả ảnh hưởng liên service.

## Kiểm tra repository

```bash
python scripts/validate-repository.py
python -m unittest discover -s tests -p "test_*.py"
```

Full Gradle/Next/Docker E2E phải chạy trên CI trước khi merge vào `main`.
