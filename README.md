# LMSPilot 0.24.0 — Source UI Redesign

**LMSPilot** là nền tảng quản lý học tập (Learning Management System) kiến trúc microservices doanh nghiệp hiện đại. Repository dạng Monorepo bao gồm frontend Next.js 16 và 19 backend services Java 21 Spring Boot 3.5 có thể phát triển, nâng cấp và mở rộng độc lập.

---

## 🚀 Điểm nổi bật trong phiên bản 0.24.0

- **Giao diện thiết kế lại (Source UI Redesign)**: Đồng bộ giao diện theo chuẩn màu Trắng – Tím (White-Purple Spectrum).
  - **Trang Cơ cấu Tổ chức**: Sơ đồ cây ngang (Horizontal Org-Chart) trực quan kết hợp panel thông tin chi tiết độc lập.
  - **Danh sách & Trình soạn đề 3 cột**: Trình biên soạn câu hỏi/bài thi chuyên nghiệp với xem trước trực tiếp, đáp án, điểm số và chính sách chấm.
  - **Màn hình làm bài mới**: Giao diện chọn ô A/B/C/D rõ ràng, đồng bộ thời gian đếm ngược thực với máy chủ, tính năng tự động lưu (autosave) và heartbeat chống mất bài.
- **Bảo vệ luồng thi an toàn**: Từ chối đề thi rỗng, tự động khóa phiên hết giờ, bảo lưu snapshot đáp án/câu hỏi bất biến cho từng lượt làm bài.
- **Khởi chạy Docker siêu tốc (30s – 60s)**:
  - Tích hợp **GitHub Actions Workflow** tự động đóng gói & đăng tải Docker image pre-built lên **GitHub Container Registry (GHCR)**.
  - Thêm **BuildKit Cache Mount** trong Dockerfile giúp biên dịch local nhanh gấp 3–4 lần.
  - Cung cấp sẵn script khởi động thông minh `start-fast.ps1` (Windows) và `start-fast.sh` (Linux/macOS).

---

## 🛠️ Công nghệ sử dụng

- **Frontend**: Next.js 16 (App Router), React 19, TypeScript 5.9 — Port `3000`.
- **Backend**: **Java 21 LTS + Spring Boot 3.5.16** (bật Spring Virtual Threads).
- **Build System**: Gradle 8.14.5, Kotlin DSL (`*.gradle.kts`). Mã nguồn Java đặt tại `src/main/java`.
- **Database**: PostgreSQL 17 (Mỗi microservice sở hữu schema độc lập và quản lý qua Flyway migrations).
- **Event Bus & Messaging**: RabbitMQ 4; Redis 8 (tùy chọn cho Caching/Rate Limiting).
- **Tích hợp & AI**: S3-compatible storage, SMTP, ONLYOFFICE/Collabora, tích hợp AI Provider (Ollama / Local OpenAI-compatible / API Key riêng).

---

## 👥 Vai trò người dùng

Mỗi tài khoản được phân định đúng một vai trò duy nhất:

| Vai trò | Chức năng chính |
|---|---|
| `ADMIN` | Quản trị tài khoản, tổ chức, thương hiệu, cấu hình kết nối AI, báo cáo, giấy phép và vận hành. |
| `INSTRUCTOR` | Biên soạn khóa học, chương/bài học, tài liệu, bài thực hành, bài kiểm tra khóa học, bài thi độc lập và chấm điểm. |
| `STUDENT` | Học khóa được giao, xem video/PDF/DOCX, nộp bài thực hành, làm bài kiểm tra/bài thi và xem kết quả. |

---

## 📖 Hướng dẫn Clone & Khởi chạy hệ thống bằng Docker

### Yêu cầu tiên quyết
1. **Git** đã được cài đặt trên máy.
2. **Docker Desktop** (hoặc Docker Engine) đang chạy (Khuyến nghị RAM từ 6 GB – 8 GB).

### Bước 1: Clone dự án về máy
```bash
git clone https://github.com/dark9th5/LMSPilot.git
cd LMSPilot
```

### Bước 2: Tạo tệp cấu hình môi trường `.env`
- **Linux / macOS**:
  ```bash
  cp .env.example .env
  ```
- **Windows (PowerShell)**:
  ```powershell
  Copy-Item .env.example .env
  ```

### Bước 3: Khởi chạy toàn bộ hệ thống bằng Docker
Chạy script khởi động nhanh được tích hợp sẵn (tự động kéo image pre-built từ GHCR giúp khởi chạy chỉ trong **30s – 60s**):

- **Windows (PowerShell)**:
  ```powershell
  .\scripts\start-fast.ps1
  ```
- **Linux / macOS**:
  ```bash
  chmod +x ./scripts/start-fast.sh
  ./scripts/start-fast.sh
  ```

👉 Sau khi khởi chạy thành công, mở trình duyệt và truy cập hệ thống tại: **`http://localhost:3000`**

---

### 🔑 Tài khoản mặc định thử nghiệm hệ thống

Dữ liệu mẫu đã được nạp sẵn với các tài khoản tương ứng với từng vai trò:

| Vai trò | Email đăng nhập | Mật khẩu mặc định |
|---|---|---|
| **ADMIN** (Quản trị) | `admin@lmspilot.local` | `Admin123!` |
| **INSTRUCTOR** (Giảng viên) | `instructor@lmspilot.local` | `Instructor123!` |
| **STUDENT** (Học viên) | `student@lmspilot.local` | `Student123!` |

---

### 🛠️ Tùy chọn: Biên dịch lại mã nguồn từ local (Local Build)

Nếu bạn thực hiện chỉnh sửa mã nguồn Java/Next.js và muốn Docker biên dịch lại từ local source code:

- **Windows (PowerShell)**:
  ```powershell
  .\scripts\start-fast.ps1 -Build
  ```
- **Linux / macOS**:
  ```bash
  ./scripts/start-fast.sh --build
  ```

Hoặc dùng lệnh Docker Compose tiêu chuẩn:
```bash
docker compose up -d --build --wait
```

### 🛑 Dừng hệ thống
```bash
docker compose down
```

Các profile bổ sung:
```bash
docker compose --profile observability up -d  # Prometheus + Grafana (:3001)
docker compose --profile redis up -d          # Redis Cache
```

---

## 🏗️ Danh sách Microservices & Ports

| Port | Service | Phạm vi chức năng | Schema PostgreSQL |
|---:|---|---|---|
| 8080 | `api-gateway` | Điểm vào duy nhất; JWT verification, Rate limiting, Router | — |
| 8081 | `identity-service` | Tài khoản, vai trò (ADMIN/INSTRUCTOR/STUDENT), JWT, Phiên | `identity` |
| 8082 | `organization-service` | Sơ đồ cơ cấu tổ chức cây ngang, Đơn vị và Thành viên | `organization` |
| 8083 | `course-service` | Khóa học, Danh mục, Chương/Bài học, Thảo luận | `course` |
| 8084 | `enrollment-service` | Ghi danh, Giao khóa học, Lộ trình học | `enrollment` |
| 8085 | `learning-service` | Tiến độ học bài, Bài thực hành, xAPI Statements | `learning` |
| 8086 | `assessment-service` | Ngân hàng câu hỏi, Bài kiểm tra khóa học, Bài thi độc lập | `assessment` |
| 8087 | `grading-service` | Chấm điểm tự động & thủ công, Phúc khảo | `grading` |
| 8088 | `reporting-service` | Báo cáo dashboard, KPI, Xuất CSV | `reporting` |
| 8089 | `file-storage-service` | Quản lý file, Xem PDF/DOCX/Video | `file_storage` |
| 8090 | `license-service` | Giấy phép bản quyền hệ thống | `license` |
| 8091 | `audit-service` | Nhật ký kiểm toán hệ thống | `audit` |
| 8092 | `notification-service` | Thông báo, Email outbox, Tin tức | `notification` |
| 8093 | `certificate-service` | Mẫu & Cấp chứng chỉ tự động | `certificate` |
| 8094 | `ai-service` | Sinh câu hỏi tự động từ PDF/DOCX, Review & Import | `ai` |
| 8095 | `configuration-service` | Cấu hình hệ thống, Thương hiệu Trắng-Tím, Logo | `configuration` |
| 8096 | `integration-service` | Kết nối SMTP, S3, ONLYOFFICE, AI Provider | `integration` |
| 8097 | `operations-service` | Health check tổng hợp, Job vận hành | `operations` |
| 8098 | `competency-service` | Khung năng lực & Khoảng thiếu | `competency` |

---

## 🤖 Cấu hình AI Provider cho Admin

Admin có thể kết nối AI theo 3 phương thức:
1. **Model Ollama mẫu tự động**: Nhấp nút chọn model (`qwen3:4b`, `qwen3:8b`, v.v.) trong Compose để tải và thiết lập tự động.
2. **AI Local có sẵn**: Nhập Base URL của Ollama/LM Studio/vLLM nội bộ.
3. **API Key riêng**: Cấu hình OpenAI-compatible API Endpoint và Key.

---

## 📁 Cấu trúc Monorepo

```text
apps/web/                         Frontend Next.js 16 (App Router)
backend/platform-contracts/       Shared Java Data Contracts
backend/service-support/          Security, Error Handling, Event Publisher Support
backend/services/<service>/       19 Spring Boot Microservices
contracts/lmspilot/               JSON Schema Contracts
infrastructure/                   PostgreSQL, RabbitMQ, Redis, Prometheus, Grafana
deploy/                           Docker configuration & environment templates
scripts/                          Start scripts, preview renderers, validators
tests/                            Automated regression & contract test suites
```

Chi tiết tài liệu từng service được đặt tại `backend/services/<service_name>/README.md`.
