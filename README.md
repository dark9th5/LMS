# LMSPilot CLS 0.9.0

LMSPilot CLS là nền tảng quản lý học tập on-premise cho doanh nghiệp, trường học và trung tâm đào tạo. Source này hợp nhất repository LMSPilot gốc, tài liệu BA/URD 48 trang và các yêu cầu mở rộng: phân quyền theo phạm vi, lộ trình học tập, bài thi độc lập, cuộc thi, AI sinh câu hỏi, tài liệu có phiên bản, tùy biến thương hiệu và giao diện “Học viện Huyền Tri”.

> Trạng thái: **full-source release candidate**. Đây là repository đầy đủ, không phải overlay. Cần chạy build, Docker smoke test và UAT trên hạ tầng đích trước production.

## Trải nghiệm Astral Academy V3

- Giao diện được tái thiết kế toàn diện thành một học viện số có chiều sâu: nền thiên thể nhiều lớp, bản đồ chòm sao, kính mờ có kiểm soát và hệ phân cấp nội dung rõ ràng.
- Shell điều hướng nổi, top bar theo ngữ cảnh, chỉ báo hệ thống và **Command Atlas** (`Ctrl/Cmd + K`) chỉ hiển thị các đích người dùng có quyền truy cập.
- Login, dashboard, khóa học, lớp học, lộ trình, kỳ thi, đổi mật khẩu và các trung tâm quản trị đều dùng chung một hệ design token, component và trạng thái tương tác.
- Bố cục thích ứng từ màn hình rộng đến điện thoại; có focus-visible, tương phản cao tùy hệ điều hành và chế độ giảm chuyển động.
- Toàn bộ hiệu ứng thị giác dùng CSS/SVG nội bộ, không phụ thuộc CDN hình ảnh hoặc font bên ngoài; dynamic branding vẫn được giữ nguyên.

Chi tiết thiết kế và phạm vi thay đổi: `docs/UI_UX_REDESIGN_0.9.0.md`.

## Mô hình tài khoản và quyền

- Account type chỉ gồm `SYSTEM_ADMIN` và `USER`.
- `ADMIN`, `INSTRUCTOR`, `LEARNER` là ba role mặc định; Admin có thể tạo role tùy chỉnh.
- Một tài khoản có thể nhận nhiều role/quyền đồng thời.
- Quyền có hiệu lực theo `SYSTEM`, chi nhánh, phòng ban, nhóm, khóa học, lớp hoặc kỳ thi.
- `DENY` ưu tiên hơn `ALLOW`; Gateway và service đều kiểm tra quyền.
- Không có đăng ký công khai; quản trị viên tạo tài khoản đơn lẻ hoặc hàng loạt.
- Tài khoản quản trị bootstrap được bảo vệ và luôn có đường đăng nhập local dự phòng.

## Năng lực chính

### Đào tạo và lộ trình

- Khóa học, chương/bài, lớp/cohort, ghi danh, giảng viên phụ trách và lớp trực tuyến.
- Snapshot khóa học bất biến khi xuất bản; lớp, ghi danh và tiến độ ghim đúng phiên bản.
- Giao khóa học theo cá nhân, nhóm, phòng ban hoặc chi nhánh.
- Lộ trình học tập nhiều chặng, mở khóa tuần tự, giao theo user/đơn vị, tự động ghi danh và tổng hợp tiến độ.
- Deadline, grace period, vị trí học gần nhất, lịch sử thời gian học và trạng thái quá hạn.
- Discussion theo khóa học có kiểm duyệt.
- xAPI Learning Record Store có idempotency.

### Bài tập, thi và cuộc thi

- Assignment submission riêng: nhiều lần nộp, file owner validation, trạng thái nộp muộn, hàng chờ chấm theo lớp, trả chỉnh sửa, điểm và phản hồi; chỉ hoàn thành sau khi giảng viên chấp nhận.
- Quiz trong bài học, bài kiểm tra trong khóa học, standalone exam và competition là bốn ngữ cảnh riêng.
- Ngân hàng câu hỏi, đề/phiên bản đề, autosave, heartbeat, resume phiên thi, grace period, enrollment context và nộp idempotent.
- Tự chấm câu khách quan; queue chấm tự luận; HIGHEST/LATEST/AVERAGE; lịch sử điểm và phúc khảo.
- Giao bài thi theo user/nhóm/phòng ban/chi nhánh.
- Leaderboard có tie-break xác định và reward ledger chống trao thưởng trùng.

### Tài khoản, tổ chức và năng lực

- Cây công ty, chi nhánh, phòng ban, nhóm; một người có thể thuộc nhiều đơn vị.
- Import CSV/XLSX có mapping, preview, lỗi từng dòng, `CREATE_ONLY/UPSERT`, `ATOMIC/PARTIAL`, idempotency và giới hạn giải nén XLSX.
- Chính sách mật khẩu, buộc đổi mật khẩu, khóa đăng nhập và quản lý/thu hồi phiên.
- LDAP/AD bind tùy chọn với timeout, filter escaping và local protected-admin fallback.
- Competency framework: danh mục năng lực, profile yêu cầu, đánh giá khoảng cách và mức sẵn sàng.

### AI, file và tài liệu

- AI local hoặc API OpenAI-compatible do khách hàng cấu hình endpoint/API key.
- JSON Schema chung cho câu hỏi, provenance theo file/trang/mục/đoạn trích/phiên bản, review và import.
- File Storage quản lý metadata, owner/purpose, grant chính xác theo file-người dùng và phiên bản; không thực thi file tải lên.
- DOCX edit session/callback qua editor tương thích; PDF annotation/upload thành revision mới.
- Redis, OnlyOffice/Collabora và local AI là profile tùy chọn, không bắt buộc cho lõi LMS.

### Báo cáo, thông báo và chứng chỉ

- Reporting read model theo sự kiện; dashboard cá nhân, phạm vi được giao và toàn hệ thống.
- KPI tổng hợp: completion, pass rate, overdue, due soon, active learner, progress và score theo khóa học.
- Export có license guard; lịch xuất báo cáo nền.
- Mẫu thông báo cấu hình không cần build lại; in-app/email outbox có retry, lease và dead state.
- Quy tắc nhắc hạn trước/sau deadline, chạy theo UTC, đọc từ Reporting read model và chống gửi trùng.
- Tin tức toàn hệ thống hoặc theo đơn vị, ghim, lịch xuất bản và xác nhận đã đọc.
- Mẫu chứng chỉ, cấp/thu hồi/cấp lại và mã xác minh.

### License, vận hành và tùy biến

- License offline ký Ed25519, machine/org binding, giới hạn active user, feature entitlement, grace period và read-only mode.
- Branding động: tên, giới thiệu, logo, favicon, màu sắc, ảnh nền và tên miền.
- Registry dịch vụ ngoài có secret mã hóa, enable/disable, health/probe và timeout.
- Health dashboard, Prometheus/Grafana profile, correlation ID và service inventory.
- Backup/restore scripts; operation schedule; host agent có claim token, heartbeat, lease và command allowlist.
- `UPDATE`/`ROLLBACK` cố ý fail-closed cho tới khi tích hợp gói phát hành ký số.

## Chạy nhanh

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup.ps1
```

### Linux

```bash
chmod +x scripts/*.sh
./scripts/setup.sh
```

Setup tạo `.env`, sinh secret/mật khẩu, chạy preflight, build container, chờ health và chạy smoke test. Chỉ coi cài đặt runtime thành công khi xuất hiện:

```text
SMOKE TEST PASSED
```

Sau đó mở `http://localhost:3000`. Mật khẩu demo nằm trong `.env`, biến `LMSPILOT_DEFAULT_ADMIN_PASSWORD`; username mặc định gồm `admin`, `instructor`, `learner`.

## Kiểm tra source

```bash
./scripts/test-static.sh
./scripts/preflight.sh
```

Bản 0.9.0 đạt **104/104** static/contract/UI regression tests, semantic TypeScript typecheck trên 65 file, Next.js production build và `npm audit` 0 vulnerability. Backend Gradle và Docker E2E chưa chạy được vì host chỉ có Java 17, không tải được Gradle distribution và không có Docker Engine. Xem `DELIVERY_STATUS.md`, `docs/UI_UX_REDESIGN_0.9.0.md`, `docs/BUILD_VERIFICATION_0.9.0.md`, `docs/AUDIT_0.8.2.md` và `docs/BA_CLS_TRACEABILITY_0.8.2.md`.

## Cấu trúc

```text
apps/web/                 Next.js 16 / React 19 portal
backend/platform-*        contracts và support dùng chung
backend/services/*        19 Spring/Kotlin service, gồm API Gateway
contracts/cls/            JSON Schema và mẫu dữ liệu AI/CLS
deploy/                    profile Redis, document editor và local AI
infrastructure/            PostgreSQL, RabbitMQ, Prometheus, Grafana
tests/                     contract/requirement/unit tests
scripts/                   setup, preflight, smoke, backup/restore, agent
docs/                      kiến trúc, traceability, runbook và verification
```

## Trước production

Cần hoàn tất trên máy đích: fresh-install migration, upgrade migration từ bản đang chạy, browser E2E, RBAC/UAT, tải đồng thời kỳ thi, backup/restore drill, update/rollback drill, TLS/domain, SMTP, LDAP/AD mapping, retention, RPO/RTO, pentest và accessibility audit.
