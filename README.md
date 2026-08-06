# LMSPilot 0.23.0

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

## Điểm mới trong 0.23.0

- Đồng bộ giao diện thật theo bộ thiết kế trắng–tím đã duyệt: tiêu đề trang gọn, panel thoáng, danh sách bài thi, trình soạn đề ba cột và màn hình làm bài A/B/C/D.
- Chuẩn hóa popup/form bằng portal, khóa cuộn nền, focus trap, giới hạn theo `100dvh`, nội dung cuộn độc lập và vùng thao tác bám đáy; form không còn tự nhảy khỏi cửa sổ ở màn hình thấp.
- Ghim cụm **Tìm nhanh – hồ sơ – Đăng xuất** ở đáy trái sidebar, không bị đẩy xa khi nội dung menu ngắn hoặc dài.
- Sửa luồng làm bài: từ chối đề rỗng, không khôi phục attempt hết giờ, đồng bộ thời gian với server và không tự nộp phiên chưa tải được câu hỏi.
- Mỗi attempt lưu snapshot bất biến của câu hỏi, đáp án chấm, điểm đạt và chính sách chấm; giảng viên sửa đề sau khi học viên bắt đầu không làm thay đổi phiên đang thi.
- Sửa hợp đồng database/API của Learning và Notification sau chuyển đổi Java; hoàn thành bài kiểm tra/bài thực hành chỉ qua sự kiện điểm hợp lệ, không qua nút đánh dấu thủ công.
- Giảm độ trễ bằng cache GET ngắn hạn, gộp request đang chạy, timeout rõ ràng, truy vấn batch thay N+1, connection pool gateway và Hikari phù hợp 19 service.
- Docker build một backend bundle dùng chung, khởi động có health dependency và script `start-fast` chờ đến khi hệ thống thật sự dùng được.
- Admin có trung tâm **Kết nối model AI**: tải model Ollama mẫu bằng nút bấm, kết nối local OpenAI-compatible hoặc cấu hình endpoint/API key riêng.

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

Các thông tin chi tiết về từng dịch vụ được mô tả chi tiết tại tệp README.md trong mỗi thư mục dịch vụ tương ứng tại `backend/services/<service_name>/README.md`.

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

## Chạy nhanh

## Hướng dẫn Clone & Khởi chạy hệ thống bằng Docker

### 📋 Yêu cầu tiên quyết
1. **Git**: Đã cài đặt Git.
2. **Docker Desktop / Docker Engine**: Đã cài đặt Docker và Docker Engine đang chạy (Khuyến nghị cấp tối thiểu 6GB–8GB RAM).

---

### 🚀 Các bước khởi chạy nhanh (30s – 1 phút)

#### Bước 1: Clone dự án về máy
```bash
git clone https://github.com/dark9th5/LMSPilot.git
cd LMSPilot
```

#### Bước 2: Tạo tệp cấu hình môi trường `.env`
- Trên **Linux / macOS**:
  ```bash
  cp .env.example .env
  ```
- Trên **Windows (PowerShell)**:
  ```powershell
  Copy-Item .env.example .env
  ```

#### Bước 3: Khởi chạy toàn bộ hệ thống bằng Docker
Chạy script khởi động nhanh được tích hợp sẵn:

- Trên **Windows (PowerShell)**:
  ```powershell
  .\scripts\start-fast.ps1
  ```
- Trên **Linux / macOS**:
  ```bash
  chmod +x ./scripts/start-fast.sh
  ./scripts/start-fast.sh
  ```

👉 Sau khi khởi chạy thành công, mở trình duyệt và truy cập hệ thống tại: **`http://localhost:3000`**

---

### 🛠️ Tùy chọn: Biên dịch lại mã nguồn từ local (Local Build)

Nếu bạn vừa chỉnh sửa mã nguồn Java/Next.js và muốn Docker biên dịch lại hoàn toàn từ local source code:

- **Windows (PowerShell)**:
  ```powershell
  .\scripts\start-fast.ps1 -Build
  ```
- **Linux / macOS**:
  ```bash
  ./scripts/start-fast.sh --build
  ```

Hoặc dùng câu lệnh Docker Compose chuẩn:
```bash
docker compose up -d --build --wait
```

---

### 🔑 Tài khoản mặc định hệ thống

Sau khi khởi chạy, hệ thống đã nạp sẵn dữ liệu demo ban đầu với 3 tài khoản thử nghiệm tương ứng 3 vai trò:

| Vai trò | Email đăng nhập | Mật khẩu mặc định |
|---|---|---|
| **ADMIN** (Quản trị) | `admin@lmspilot.local` | `Admin123!` |
| **INSTRUCTOR** (Giảng viên) | `instructor@lmspilot.local` | `Instructor123!` |
| **STUDENT** (Học viên) | `student@lmspilot.local` | `Student123!` |

---

### 🛑 Tắt hoặc dừng hệ thống

Để dừng toàn bộ container:
```bash
docker compose down
```

Các profile tùy chọn khác (Metrics/Observability, Cache):
```bash
docker compose --profile observability up -d  # Prometheus + Grafana (:3001)
docker compose --profile redis up -d          # Redis
```

### Chạy một backend service

```bash
cd backend
./gradlew :services:course-service:bootRun
```

### Test một backend service

```bash
cd backend
./gradlew :services:course-service:test
```

### Test toàn backend

```bash
cd backend
./gradlew test
```

### Frontend

```bash
cd apps/web
npm ci
npm run dev
```

## Luồng làm bài được bảo vệ

```text
Học viên bấm Làm bài
    → Assessment kiểm tra bài thi đang mở và có câu hỏi
    → tạo/khôi phục attempt IN_PROGRESS còn thời gian
    → đóng băng snapshot câu hỏi + đáp án chấm + chính sách điểm
    → frontend chỉ mở trình làm bài khi nhận đủ câu hỏi và thời gian server
    → autosave đáp án + heartbeat
    → nộp chủ động hoặc hết giờ đã được server xác nhận
    → Grading phát sự kiện điểm
    → Learning cập nhật bài học/khóa học khi đạt điều kiện
```

Không có đường tắt “đánh dấu hoàn thành” cho bài thi hoặc bài thực hành. Nếu đề rỗng, attempt hết hạn hoặc dịch vụ chưa trả đủ dữ liệu, UI hiển thị trạng thái lỗi có thể thử lại thay vì tự hoàn thành.

## Tài liệu kiểm toán 0.23.0

- [`docs/PERFORMANCE_AND_FLOW_AUDIT_0.23.0.md`](docs/PERFORMANCE_AND_FLOW_AUDIT_0.23.0.md)
- [`docs/UI_QA_CHECKLIST_0.23.0.md`](docs/UI_QA_CHECKLIST_0.23.0.md)
- [`docs/LOGIN_AUDIT_0.23.0.md`](docs/LOGIN_AUDIT_0.23.0.md)


## Kiểm tra đăng nhập và hiệu năng sau khi chạy

```bash
node scripts/smoke-login-roles.mjs
node scripts/performance-smoke.mjs
```

Các script kiểm tra ba tài khoản demo, role trả về, thời gian phản hồi và một số API dữ liệu chính. Không dùng tài khoản demo ở staging/production.

## Cấu hình AI cho Admin

Ba cách kết nối được hỗ trợ:

1. **Tải model mẫu tự động:** Ollama chạy trong Compose; Admin chọn `qwen3:4b`, `qwen3:8b` hoặc `llama3.1:8b`. Backend tải bất đồng bộ, trả tiến độ và tự tạo provider.
2. **AI local có sẵn:** nhập Base URL của Ollama, LM Studio, vLLM hoặc máy chủ OpenAI-compatible trong mạng nội bộ.
3. **API key riêng:** nhập Base URL, model và API key của nhà cung cấp tương thích; key được mã hóa ở backend.

API quản trị liên quan:

- `GET /api/v1/ai/local-runtime`
- `POST /api/v1/ai/local-runtime/pull`
- `GET /api/v1/ai/local-runtime/pull/{jobId}`
- `GET|POST|PUT /api/v1/ai/providers`
- `POST /api/v1/ai/providers/{id}/test`

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
