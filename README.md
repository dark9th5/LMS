# LMSPilot 0.24.0

**LMSPilot** là nền tảng quản lý học tập theo kiến trúc microservice, phục vụ doanh nghiệp, trường học và trung tâm đào tạo. Repository là monorepo gồm frontend Next.js và 19 backend service Java Spring Boot có thể phát triển, nâng cấp và vận hành độc lập.

---

## 🛠️ Công nghệ

- **Frontend**: Next.js 16, React 19, TypeScript 5.9 — Port `3000`.
- **Backend**: Java 21 + Spring Boot 3.5.16.
- **Build Backend**: Gradle 8.14.5, Gradle Kotlin DSL (`*.gradle.kts`). Mã nguồn Java nằm trong `src/main/java`.
- **Database**: PostgreSQL 17 (schema & Flyway migration riêng cho từng service).
- **Messaging & Cache**: RabbitMQ (chính) & Redis.
- **File & Integration**: S3 Storage, SMTP, ONLYOFFICE/Collabora, Local AI (Ollama).

---

## 🔑 Tài khoản khởi tạo mặc định (Demo)

Sau khi khởi chạy hệ thống, bạn có thể sử dụng các tài khoản mặc định dưới đây để đăng nhập:

| Vai trò | Tên đăng nhập (`username`) | Mật khẩu (`password`) | Mô tả |
|---|---|---|---|
| **Quản trị viên (`ADMIN`)** | `admin` | `admin123` | Quản trị tài khoản, phân quyền, tổ chức, dịch vụ ngoài, báo cáo & hệ thống. |
| **Giảng viên (`INSTRUCTOR`)** | `instructor` | `admin123` | Biên soạn khóa học, ngân hàng câu hỏi, bài kiểm tra, tạo bài thi & chấm điểm. |
| **Học viên (`STUDENT`)** | `student` | `admin123` | Tham gia khóa học, học video/PDF/DOCX, làm bài thi/kiểm tra & xem kết quả. |

---

## 🚀 Khởi chạy hệ thống (Quick Start)

### Khởi chạy bằng Docker Compose (Khuyên dùng)

1. **Khởi tạo file cấu hình môi trường**:
   - Windows PowerShell:
     ```powershell
     Copy-Item .env.example .env
     ```
   - Linux / macOS:
     ```bash
     cp .env.example .env
     ```

2. **Khởi chạy toàn bộ hệ thống**:
   ```bash
   docker compose up -d --build
   ```

3. **Truy cập ứng dụng**:
   - Web App (Frontend): [http://localhost:3000](http://localhost:3000)
   - API Gateway: [http://localhost:8080](http://localhost:8080)
   - Quản trị RabbitMQ: [http://localhost:15672](http://localhost:15672)

---

## 🏛️ Kiến trúc 19 Microservices & Gateway

```text
Browser :3000
    │
API Gateway :8080
    ├─ Identity :8081        ├─ Organization :8082
    ├─ Course :8083          ├─ Enrollment :8084
    ├─ Learning :8085        ├─ Assessment :8086
    ├─ Grading :8087         ├─ Reporting :8088
    ├─ File Storage :8089    ├─ License :8090
    ├─ Audit :8091           ├─ Notification :8092
    ├─ Certificate :8093     ├─ AI :8094
    ├─ Configuration :8095   ├─ Integration :8096
    └─ Operations :8097      └─ Competency :8098
```

| Port | Service | Mô tả ngắn | Schema DB |
|---:|---|---|---|
| 8080 | `api-gateway` | Điểm vào duy nhất của frontend; xác thực JWT, rate limit, định tuyến API. | `—` |
| 8081 | `identity-service` | Đăng nhập, JWT/refresh token, quản lý tài khoản, vai trò và phân quyền. | `identity` |
| 8082 | `organization-service` | Cây cơ cấu tổ chức, đơn vị và quan hệ thành viên. | `organization` |
| 8083 | `course-service` | Khóa học, danh mục, chương/bài học, phiên bản xuất bản và thảo luận. | `course` |
| 8084 | `enrollment-service` | Giao khóa học, ghi danh, lộ trình học và phiên học trực tuyến. | `enrollment` |
| 8085 | `learning-service` | Tiến độ học, hoàn thành bài học, nộp/chấm bài thực hành và xAPI. | `learning` |
| 8086 | `assessment-service` | Ngân hàng câu hỏi, bài kiểm tra, bài thi độc lập, phiên làm bài và cuộc thi. | `assessment` |
| 8087 | `grading-service` | Chấm tự động, chấm thủ công, lịch sử điểm và phúc khảo. | `grading` |
| 8088 | `reporting-service` | Read model báo cáo, dashboard, KPI, xuất báo cáo và lịch báo cáo. | `reporting` |
| 8089 | `file-storage-service` | Lưu trữ file, quyền truy cập, phiên bản, xem PDF/DOCX/video. | `file_storage` |
| 8090 | `license-service` | Kích hoạt giấy phép, entitlement và giới hạn tính năng. | `license` |
| 8091 | `audit-service` | Nhật ký kiểm toán và xuất dữ liệu kiểm toán. | `audit` |
| 8092 | `notification-service` | Thông báo, email outbox, tin tức, template và nhắc hạn. | `notification` |
| 8093 | `certificate-service` | Mẫu chứng chỉ, cấp, tra cứu, thu hồi và cấp lại. | `certificate` |
| 8094 | `ai-service` | Kết nối Ollama/OpenAI-compatible; sinh & trích xuất câu hỏi tự động. | `ai` |
| 8095 | `configuration-service` | Cấu hình hệ thống, thương hiệu, logo, ảnh nền đăng nhập. | `configuration` |
| 8096 | `integration-service` | Adapter kết nối SMTP, S3, ONLYOFFICE, dịch vụ ngoài. | `integration` |
| 8097 | `operations-service` | Monitoring health tổng hợp, lịch job và quản lý vận hành. | `operations` |
| 8098 | `competency-service` | Khung năng lực, hồ sơ khoảng thiếu và ánh xạ khóa học. | `competency` |

---

## 📁 Cấu trúc Monorepo

```text
d:\App Android\LMS
├── .github/              Workflows CI/CD (GitHub Actions & GHCR Docker publish)
├── apps/                 Next.js Frontend & BFF
├── backend/              19 Spring Boot Microservices (Java 21)
├── contracts/            Schema / Contracts API ngoài tiến trình
├── deploy/               K8s / Deployment manifests & Docker environment
├── infrastructure/       PostgreSQL init, Grafana, Prometheus configs
├── scripts/              Script khởi chạy & preflight
├── .env.example          File mẫu cấu hình biến môi trường
├── docker-compose.yml    Docker Compose file khởi chạy 19 services + Web + Infra
├── Makefile              Lệnh tắt cho phát triển
├── README.md             Tài liệu hướng dẫn hệ thống
└── VERSION               Phiên bản hệ thống (0.24.0)
```

---

## 💻 Chạy ở môi trường Local Development

### 1. Backend Microservice (Gradle)
```bash
cd backend
./gradlew :services:course-service:bootRun
```

### 2. Frontend Web App (Next.js)
```bash
cd apps/web
npm ci
npm run dev
```

---

## 🐳 Pre-built Container Images (GHCR)

Pre-built Docker images tự động đóng gói và xuất bản lên **GitHub Container Registry (GHCR)**:
- `ghcr.io/dark9th5/lmspilot-backend-bundle:0.24.0`
- `ghcr.io/dark9th5/lmspilot-web:0.24.0`
