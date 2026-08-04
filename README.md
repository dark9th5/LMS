# LMSPilot CLS 0.15.0

LMSPilot CLS là nền tảng quản lý học tập on-premise cho doanh nghiệp, trường học và trung tâm đào tạo. Source này hợp nhất repository LMSPilot gốc, tài liệu BA/URD 48 trang và các yêu cầu mở rộng: phân quyền theo phạm vi, lộ trình học tập, bài thi độc lập, cuộc thi, AI sinh câu hỏi, tài liệu có phiên bản, tùy biến thương hiệu và Theme Studio đa giao diện.

> Trạng thái: **full-source release candidate**. Đây là repository đầy đủ, không phải overlay. Cần chạy build, Docker smoke test và UAT trên hạ tầng đích trước production.

## Permission-first 0.15

- Tài khoản chỉ có hai loại: `SYSTEM_ADMIN` được bảo vệ và `USER`; không còn dùng `ADMIN/INSTRUCTOR/LEARNER` làm loại người dùng hay điều kiện mở tính năng.
- Role được đổi nghĩa thành **gói quyền có thể ghép**. Hệ thống tự tạo 10 gói mẫu theo công việc: `BASIC_USER`, `COURSE_AUTHOR`, `TRAINING_MANAGER`, `EXAM_MANAGER`, `GRADER`, `ORGANIZATION_MANAGER`, `COMMUNICATIONS_EDITOR`, `ACCOUNT_MANAGER`, `ACCESS_ADMINISTRATOR`, `PLATFORM_CUSTOMIZER`.
- Mỗi permission có tên tiếng Việt, mô tả, nhóm nghiệp vụ, mức rủi ro và danh sách phạm vi hợp lệ. Gói quyền được cấp theo `SYSTEM`, chi nhánh, phòng ban, nhóm, khóa học hoặc kỳ thi.
- Console quản trị hỗ trợ chọn hàng loạt, xem trước quyền mới/quyền trùng/quyền bị DENY/quyền không áp dụng ở phạm vi, cảnh báo quyền `CRITICAL`, đặt thời hạn, giải thích nguồn quyền và thu hồi đúng từng lần cấp.
- Backend service không còn quyết định truy cập bằng tên role. JWT tách capability dùng cho coarse gate và `globalPermissions` dùng cho thao tác phạm vi `SYSTEM`; kiểm tra tài nguyên cụ thể vẫn thực hiện tại service.
- Danh sách người phụ trách lớp và người có thể ghi danh được lọc theo permission thực tế, nên một tài khoản có thể đồng thời học, biên soạn, chấm điểm và quản lý thi.

Chi tiết kiến trúc và luồng nâng cấp: `docs/PERMISSION_FIRST_0.15.0.md`.

## Soft Spectrum 0.14

- Giao diện mặc định mới là **Sắc màu Cân bằng (`soft-spectrum`)**: giữ energy, color blocking và cá tính tươi sáng của Spectrum nhưng giảm saturation, tăng khoảng nghỉ và dùng pastel cho dữ liệu diện tích lớn.
- Sidebar accordion được khóa vào một palette navy–graphite–xám–trắng. Ba nhóm **Học tập / Đánh giá / Quản trị**, icon, count và trạng thái active không còn pha cyan/indigo/violet; chỉ màu trạng thái nghiệp vụ được phép xuất hiện.
- Hero, KPI, summary, catalog khóa học, danh sách lớp, kỳ thi, login và page header dùng coral/rose/aqua/violet/lime/yellow đã tiết chế; chữ và control giữ tương phản rõ.
- Admin vẫn có thể tìm, lọc, xem thử, hoàn tác và áp dụng một trong **10 cá tính giao diện** tại `Thiết lập thương hiệu → Theme Studio`; chín theme còn lại được giữ nguyên.
- Theme được lưu thật vào Configuration Service bằng `themeKey`; SSR đọc public branding để chọn theme ngay từ HTML đầu tiên.
- Flyway V6 ánh xạ `enterprise-blue` sang `soft-spectrum`, chỉ thay palette cũ nếu khớp đúng một default chưa tùy chỉnh và trung hòa seed fantasy cũ; custom branding được bảo toàn.
- Mỗi preset thay cả palette, typography, geometry, radius, shadow, density, sidebar, hero, KPI, form, bảng và trạng thái; đây không chỉ là thay màu chủ đạo.
- Sidebar tiếp tục lọc mục con theo quyền, tự mở nhóm chứa route hiện tại và chỉ mở tối đa một nhóm.
- **Command palette** (`Ctrl/Cmd + K`) và các liên kết con tiếp tục chỉ hiển thị đích người dùng được phép truy cập.
- Bố cục thích ứng từ màn hình rộng đến điện thoại; có focus-visible, tương phản cao tùy hệ điều hành và chế độ giảm chuyển động.
- Toàn bộ hiệu ứng thị giác dùng CSS/SVG nội bộ, không phụ thuộc CDN hình ảnh hoặc font bên ngoài; dynamic branding vẫn được giữ nguyên.

Chi tiết thiết kế, migration và ảnh render: `docs/SOFT_SPECTRUM_0.14.0.md`. Tài liệu 0.13 trở về trước được giữ như lịch sử migration.

## Mô hình tài khoản và quyền

- Account type chỉ gồm `SYSTEM_ADMIN` và `USER`.
- Gói quyền không phải loại tài khoản; một USER có thể nhận nhiều gói theo công việc và nhiều permission riêng lẻ.
- Quyền có hiệu lực theo `SYSTEM`, chi nhánh, phòng ban, nhóm, khóa học hoặc kỳ thi; permission không tương thích sẽ bị loại khỏi gói ở phạm vi đó.
- `DENY` ưu tiên hơn `ALLOW`; quyền toàn hệ thống và quyền theo tài nguyên được phân biệt trong token và tại service.
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
- Branding động: tên, giới thiệu, logo, favicon, màu sắc, ảnh nền, tên miền và 10 theme có thể đổi tại runtime.
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

Bản 0.15.0 đạt **127/127** static/contract/UI regression tests và toàn bộ 62 tệp TypeScript/TSX được TypeScript compiler parse không có lỗi cú pháp. `platform-contracts` mới đã được biên dịch riêng bằng Kotlin compiler. Môi trường đóng gói không tải được một dependency npm từ registry nội bộ và không tải được Gradle distribution, nên chưa thể xác nhận lại Next production build hay toàn bộ backend compile cho thay đổi 0.15. Docker E2E cũng chưa chạy. Xem `TEST_RESULTS_CLS_0.15.0.md`, `DELIVERY_STATUS.md` và `docs/PERMISSION_FIRST_0.15.0.md`.

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
