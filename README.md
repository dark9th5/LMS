# 🚀 LMSPilot 0.17.0 — Nền tảng Quản lý Học tập Enterprise (On-Premise)

> **Trạng thái:** `Full-Source Release Candidate (v0.17.0)`  
> **Phân loại:** Đào tạo Doanh nghiệp & Trường học (LMS / E-Learning Platform)  
> **Mô hình triển khai:** On-Premise / LAN Server (Bảo mật nội bộ, không phụ thuộc cloud bên ngoài)

---

## 📌 1. Giới thiệu Tổng quan

**LMSPilot** là giải pháp phần mềm quản lý học tập (Learning Management System) toàn diện, được thiết kế theo kiến trúc **Microservices** hiện đại, sẵn sàng triển khai trên hạ tầng máy chủ nội bộ (On-Premise) của doanh nghiệp, trường học và các trung tâm đào tạo.

Phiên bản **0.17.0** tập trung vào việc **thống nhất giao diện người dùng (Unified UX/UI)**, đơn giản hóa luồng vận hành của Quản trị viên (Admin Core), tối ưu hóa trải nghiệm học tập và làm bài thi trực tuyến, đồng thời đảm bảo tính toàn vẹn dữ liệu và hiệu năng vận hành cao.

---

## ✨ 2. Các Điểm Nổi bật trên Phiên bản 0.17.0

### 🎨 Giao diện Thống nhất (Unified Design System)
- **Hệ thống thiết kế chuẩn hóa:** Thống nhất toàn bộ font chữ, khoảng cách, màu sắc, nút bấm và component trên toàn portal.
- **Chế độ màu thích ứng:** Hỗ trợ 2 giao diện chuẩn `unified-light` (Sáng) và `unified-dark` (Tối). Hệ thống tự động tính toán màu chữ nổi bật dựa trên độ sáng của màu thương hiệu (Branding) nhằm đạt chuẩn độ tương phản (WCAG AA/AAA).
- **Tối ưu trải nghiệm làm bài & học tập:** Trọng tâm nội dung bài học và câu hỏi bài thi được bố trí ở **cột chính rộng rãi**. Mục lục hoặc danh sách câu hỏi chuyển sang cột phụ và tự động co dãn 1 cột linh hoạt trên thiết bị di động.
- **Chuẩn Accessibility & Focus:** Tất cả các ô nhập liệu (Input), phương án lựa chọn và nút tương tác đạt kích thước tối thiểu **44px**, xử lý chuẩn lỗi trùng màu nền khi autofill tài khoản trên trình duyệt.

### 🛡️ Admin Core Tinh gọn
Hệ thống quản trị được tối ưu hóa để tập trung vào các nghiệp vụ vận hành LMS cốt lõi:

| Phân hệ Quản trị | Chức năng chính |
| :--- | :--- |
| **Dashboard** | Báo cáo tổng quan, chỉ số KPI học tập và chỉ số vận hành real-time. |
| **Học tập của tôi** | Không gian dành riêng cho từng cá nhân theo dõi khóa học và lộ trình. |
| **Quản lý Khóa học** | Khởi tạo bài học, tài liệu, video, bài tập và cây nội dung. |
| **Quản lý Lớp học** | Phân lớp, ghi danh học viên, phân công giảng viên và lịch học. |
| **Bài kiểm tra & Kỳ thi** | Ngân hàng câu hỏi, tạo đề thi, cấu hình thời gian và quy chế thi. |
| **Chấm điểm & Kết quả** | Chấm bài tự động/thủ công, duyệt phúc khảo và xuất bảng điểm. |
| **Báo cáo & Thống kê** | Xuất báo cáo tiến độ, tỷ lệ hoàn thành, điểm số theo CSV/Excel. |
| **Người dùng & Phân quyền** | Quản lý tài khoản, vai trò (RBAC), gói quyền và phạm vi (Scope). |
| **Cơ cấu Tổ chức** | Quản lý phòng ban, chi nhánh, đơn vị trực thuộc. |
| **Cài đặt Hệ thống** | Cấu hình thương hiệu, giao diện, SMTP, LDAP và các tham số vận hành. |

*Lưu ý: Các module nâng cao như AI Studio, Competency, Operations, Notification Automation, News, Live Session, Learning Path được ẩn khỏi menu sidebar chuẩn để đơn giản hóa giao diện nhưng vẫn duy trì backend service tương thích.*

### 🛠️ Trải nghiệm Nhập liệu Trực quan
- Sử dụng danh sách phương án dạng dòng riêng kèm nút bấm `+ / −` trực quan.
- Tích hợp bộ điều chỉnh số `NumberStepper` cho thời lượng, số lần làm bài, điểm đạt và điểm từng câu hỏi.
- Hỗ trợ nhập liệu người dùng hàng loạt (Bulk Import) qua tệp **CSV/XLSX** có màn hình xem trước và xác nhận dữ liệu lỗi.

---

## 🏗️ 3. Kiến trúc Kỹ thuật & Công nghệ

### 💻 Công nghệ Sử dụng

- **Frontend (Web Portal):**  
  - Framework: **Next.js 16** (App Router), **React 19**, **TypeScript 5.9**.
  - Styling: Vanilla CSS Design System (`globals.css`, `unified.css`), HSL Dynamic Color Palette.
- **Backend (Services):**  
  - Core Stack: **Java 21**, **Spring Boot 3.5**, **Kotlin 2.0**.
  - Kiến trúc: **19 Microservices** độc lập + **API Gateway** xử lý định tuyến & xác thực JWT.
- **Cơ sở dữ liệu & Thông điệp:**  
  - **PostgreSQL:** Lưu trữ dữ liệu nghiệp vụ (mỗi service sử dụng schema riêng biệt, quản lý qua Flyway Migration).
  - **RabbitMQ:** Message broker phục vụ kiến trúc hướng sự kiện (Event-Driven AMQP).
  - **Redis (Tùy chọn):** Caching và quản lý phiên làm việc.
- **Dịch vụ Mở rộng (Tùy chọn Profile):**  
  - **Local AI:** Tích hợp Ollama / Qwen3 (phân tích & trợ lý AI học tập nội bộ).
  - **OnlyOffice / Collabora:** Xem & chỉnh sửa tài liệu văn bản trực tiếp.

---

## 📦 4. Danh sách 19 Backend Microservices

Tất cả các truy vấn từ Client đều đi qua **API Gateway** (Port `8080`) để kiểm tra JWT token, Rate Limiting và định tuyến đến service tương ứng:

```text
                                  ┌─── Identity Service (Tài khoản & Phân quyền)
                                  ├─── Organization Service (Phòng ban & Tổ chức)
                                  ├─── Course Service (Khóa học & Nội dung)
                                  ├─── Enrollment Service (Ghi danh & Lớp học)
                                  ├─── Learning Service (Tiến độ & Học tập)
                                  ├─── Assessment Service (Ngân hàng đề & Kỳ thi)
                                  ├─── Grading Service (Chấm điểm & Phúc khảo)
[Client / Web] ──> API Gateway ───┼─── Reporting Service (Báo cáo & KPI)
   (Port 3000)     (Port 8080)    ├─── Certificate Service (Cấp chứng chỉ)
                                  ├─── File Storage Service (Lưu trữ tệp & media)
                                  ├─── Notification Service (Gửi mail & Thông báo)
                                  ├─── Configuration Service (Cấu hình & Branding)
                                  ├─── Audit Service (Nhật ký hệ thống)
                                  ├─── License Service (Giấy phép bản quyền)
                                  └─── AI / Operations / Integration Services...
```

1. `api-gateway`: Cổng định tuyến API, kiểm tra token xác thực, rate limit và correlation ID.
2. `identity-service`: Quản lý tài khoản, đăng nhập, phân quyền RBAC và phiên làm việc.
3. `organization-service`: Quản lý sơ đồ tổ chức, đơn vị, phòng ban và chi nhánh.
4. `course-service`: Quản lý danh mục, khóa học, chương trình và bài học.
5. `enrollment-service`: Quản lý ghi danh học viên, lớp học và phân công giảng dạy.
6. `learning-service`: Ghi nhận tiến độ học tập, theo dõi bài học và xAPI statement.
7. `assessment-service`: Ngân hàng câu hỏi, tạo đề thi, quản lý lượt làm bài thi.
8. `grading-service`: Chấm điểm bài thi tự động/thủ công, xử lý phúc khảo.
9. `reporting-service`: Tổng hợp báo cáo tiến độ, kết quả thi và chỉ số KPI.
10. `certificate-service`: Cấp phát, quản lý và tra cứu chứng chỉ hoàn thành.
11. `file-storage-service`: Quản lý lưu trữ tệp tin, tài liệu và media bài học.
12. `notification-service`: Quản lý mẫu thông báo, gửi email tự động và nhắc nhở.
13. `configuration-service`: Quản lý cài đặt hệ thống, tùy chỉnh giao diện/gói thương hiệu.
14. `audit-service`: Lưu trữ lịch sử thao tác và nhật ký an toàn thông tin.
15. `license-service`: Kiểm tra và xác thực bản quyền phần mềm on-premise.
16. `operations-service`: Giám sát sức khỏe hệ thống, quản lý công việc định kỳ (Jobs/Schedules).
17. `competency-service`: Quản lý khung năng lực và đánh giá kỹ năng.
18. `integration-service`: Tích hợp các hệ thống bên ngoài (LDAP/Active Directory, SSO).
19. `ai-service`: Xử lý các tác vụ trí tuệ nhân tạo local (Tóm tắt bài học, gợi ý câu hỏi).

---

## 📁 5. Cấu trúc Thư mục Mã nguồn

```text
LMS/
├── apps/
│   └── web/                   # Frontend Web (Next.js 16, React 19, Unified Design System)
├── backend/
│   ├── platform-contracts/    # Dữ liệu dùng chung & Event Schemas
│   └── services/              # 19 Spring Boot / Kotlin Microservices
├── contracts/
│   └── cls/                   # JSON Schemas & Mẫu định dạng dữ liệu chuẩn
├── deploy/                    # Cấu hình profile mở rộng (AI Local, OnlyOffice...)
├── docs/                      # Tài liệu kỹ thuật, API Catalog & Operations Runbook
├── infrastructure/            # Cấu hình hạ tầng Docker (PostgreSQL, RabbitMQ, Redis)
├── scripts/                   # Kịch bản tự động (Setup, Backup, Restore, Smoke test)
├── tests/                     # Bộ kiểm thử tự động (Contract, UI Regression, Python tests)
├── .env.example               # Tệp mẫu cấu hình biến môi trường
├── docker-compose.yml         # File khởi chạy Docker Stack
├── Makefile                   # Lệnh tắt điều khiển dự án
├── BAT_DAU_TAI_DAY.md         # Hướng dẫn nhanh cho nhà phát triển / vận hành
└── README.md                  # Tài liệu hướng dẫn tổng quan hệ thống
```

---

## ⚡ 6. Hướng dẫn Khởi chạy Nhanh (Quick Start)

### Yêu cầu Môi trường
- **Java:** JDK 21 trở lên
- **Node.js:** v22 – v24 (kèm npm 10+)
- **Python:** 3.13+ (kèm `PyYAML`, `pytest`)
- **Docker & Docker Compose:** Đã cài đặt và đang chạy

### 🛠️ Cách 1: Khởi chạy Tự động bằng Script

**Trên Windows (PowerShell):**
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup.ps1
```

**Trên Linux / macOS (Bash):**
```bash
chmod +x scripts/*.sh
./scripts/setup.sh
```

> 💡 **Kiểm tra thành công:** Sau khi script hoàn tất và hiển thị thông báo `SMOKE TEST PASSED`, truy cập ứng dụng tại địa chỉ:  
> 🌐 **Frontend Web:** `http://localhost:3000`  
> ⚙️ **API Gateway:** `http://localhost:8080`

---

### 🐳 Cách 2: Khởi chạy thủ công với Docker Compose

1. **Tạo tệp cấu hình môi trường `.env`:**
   ```bash
   cp .env.example .env
   ```
2. **Khởi chạy cụm dịch vụ cơ bản (Core Stack):**
   ```bash
   docker compose up -d --build
   ```
3. **Khởi chạy kèm dịch vụ nâng cao (AI Local & Observability Prometheus/Grafana):**
   ```bash
   docker compose --profile extended --profile observability up -d --build
   ```

---

## 🧪 7. Kiểm tra & Xắc nhận Mã nguồn (Testing & Validation)

Để kiểm tra độ toàn vẹn của mã nguồn trước khi đưa lên môi trường thử nghiệm hoặc sản xuất, chạy các câu lệnh sau:

```bash
# 1. Kiểm tra cấu trúc repository & file cấu hình
python scripts/validate-repository.py

# 2. Chạy bộ kiểm thử tự động Python
pytest -q

# 3. Kiểm tra kiểu dữ liệu TypeScript & Build Frontend
cd apps/web
npm ci
npm run typecheck
npm run build

# 4. Kiểm tra Unit Test phía Backend (Kotlin/Java)
cd ../../backend
./gradlew test
```

---

## 🛡️ 8. Hướng dẫn Vận hành, Sao lưu & Phục hồi

### 💾 Sao lưu Dữ liệu (Backup)
Hệ thống cung cấp kịch bản tự động sao lưu toàn bộ cơ sở dữ liệu PostgreSQL, định nghĩa RabbitMQ, tệp lưu trữ bài học và thông tin giấy phép:

```bash
./scripts/backup.sh
```
*Bản sao lưu sẽ được nén và lưu trong thư mục `backups/<thời-gian-tạo>/` kèm tệp xác thực mã MD5/SHA256.*

### 🔄 Phục hồi Dữ liệu (Restore)
1. Đưa hệ thống về trạng thái bảo trì.
2. Chạy kịch bản phục hồi chỉ định thư mục bản sao lưu:
   ```bash
   ./scripts/restore.sh backups/<thời-gian-tạo>
   ```
3. Khởi động lại cụm dịch vụ và kiểm tra trạng thái hoạt động (`http://<server-ip>:8080/actuator/health`).

---

## 📚 9. Tài liệu Tham khảo

- 📖 [Hướng dẫn Bắt đầu Nhanh](file:///d:/App%20Android/LMS/BAT_DAU_TAI_DAY.md) (`BAT_DAU_TAI_DAY.md`)
- 🏛️ [Kiến trúc chi tiết & Luồng dữ liệu](file:///d:/App%20Android/LMS/docs/architecture.md) (`docs/architecture.md`)
- 🔌 [Danh mục API Catalog & Endpoint Header](file:///d:/App%20Android/LMS/docs/api-catalog.md) (`docs/api-catalog.md`)
- 🔧 [Quy trình Vận hành Runbook](file:///d:/App%20Android/LMS/docs/runbook.md) (`docs/runbook.md`)
- 📄 [Tài liệu Hướng dẫn Dự án LMSPilot](file:///d:/App%20Android/LMS/HUONG_DAN_DU_AN_LMSPILOT.docx) (`HUONG_DAN_DU_AN_LMSPILOT.docx`)

---

© 2026 **LMSPilot Team**. All rights reserved.

