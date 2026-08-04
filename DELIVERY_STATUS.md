# Trạng thái bàn giao — LMSPilot CLS 0.9.0

## Phạm vi bản bàn giao

Đây là **repository đầy đủ**, không phải overlay. Source hợp nhất LMSPilot gốc, URD/BA và các yêu cầu bổ sung trong cuộc trò chuyện. Trạng thái chính xác là **full-source release candidate**: schema, domain, API, quyền, giao diện, script và test tĩnh đã được hợp nhất; chưa phải chứng nhận production trên mọi hạ tầng.

## Điểm hoàn thiện nổi bật

- Hai account type; ba role mặc định, role tùy chỉnh, nhiều role và scoped `ALLOW/DENY`.
- CSV/XLSX import có preview, mapping, row errors, atomic/partial và upsert.
- Password policy, forced change, lockout và session management.
- License offline cưỡng chế feature, capacity, grace/read-only.
- LDAP bind có local bootstrap fallback.
- Khóa học bất biến theo version; lớp/tiến độ pin version.
- Lộ trình học tập có nhiều chặng, mở khóa tuần tự, giao theo cá nhân/đơn vị, auto-enroll và tổng hợp tiến độ.
- Assignment aggregate, exam resume/heartbeat/grace, grade history/appeal.
- Standalone exam, competition, leaderboard/reward.
- xAPI LRS, competency framework, course discussions.
- AI local/API, JSON Schema, provenance, review/import.
- DOCX/PDF revision workflow, branding và news.
- KPI theo scope/khóa học; report schedule.
- Notification template và due reminder có idempotent claim, timeout, retry, email outbox lease/backoff.
- Certificate template/version, audit export, operation schedules và allowlisted host agent.
- Semantic TypeScript gate, BFF same-origin mutation guard, bulk operation DB lock và file object-level grant hardening.
- Assignment/Exam completion do server xác minh; exam session/grade/report gắn đúng enrollment, course và lesson.
- Shuffle/auto-grade/score strategy hoạt động thật; hàng đợi chấm assignment theo lớp và news attachment có ACL chính xác.
- Domain event chỉ phát sau commit DB để tránh sự kiện “ma” khi transaction rollback.
- Astral Academy V3 thay toàn bộ ngôn ngữ giao diện: shell nổi, nền thiên thể nhiều lớp, hierarchy mới, responsive sâu và hệ component thống nhất.
- Command Atlas là tìm kiếm điều hướng thật bằng bàn phím, lọc theo quyền; login, dashboard và các workspace nghiệp vụ đều được dựng lại, không phải đổi màu giao diện cũ.

## Kết quả kiểm tra trong môi trường tạo bản

Đã chạy trên working tree:

- Repository validator: **OK** — 13 JSON, 28 YAML, 19 service, 18 service dùng Flyway.
- Python static/contract/UI regression suite: **104/104 đạt**.
- Semantic TypeScript typecheck trên 65 file: đạt.
- Clean `npm ci`, Next.js 16.2.12 production build: đạt.
- `npm audit`: 0 vulnerability.
- Shell syntax check: đạt.
- Flyway duplicate/destructive migration checks: đạt.
- Internal API service-token, gateway routes, permission trace và runtime-secret checks: đạt.
- Operations-agent command allowlist unit tests: đạt.

Archive cuối được kiểm tra checksum và danh sách file cấm trước khi bàn giao. Bộ validator, 104 test, semantic typecheck và production build đều chạy trên source tree dùng để đóng gói.

## Phần còn bị giới hạn bởi môi trường

- Host chỉ có OpenJDK 17 trong khi project dùng Java toolchain 21.
- Gradle wrapper không tải được distribution vì network tới `services.gradle.org` không khả dụng.
- Node 24/npm 11/Python 3.12 có sẵn; frontend đã cài sạch và build thành công.
- Docker CLI/Engine không có.

Vì vậy chưa thể tuyên bố đã xác nhận:

- Gradle compile/test cho toàn bộ service.
- PostgreSQL/RabbitMQ/Redis/OnlyOffice/local AI integration.
- Browser E2E, load test, pentest, restore drill và UAT.

Đây là giới hạn xác minh, không phải bằng chứng backend đã build thành công. Chi tiết ở `docs/BUILD_VERIFICATION_0.9.0.md`, `docs/UI_UX_REDESIGN_0.9.0.md` và `docs/AUDIT_0.8.2.md`.

## Phần phụ thuộc chính sách/hạ tầng khách hàng

- LDAP/AD OU/group sync và mapping cụ thể.
- HRM/ERP/NAS/SMTP/video provider adapters theo API thật.
- Signed offline release bundle cho update/rollback.
- RPO/RTO, retention, backup encryption/offsite copy và restore drill.
- Chữ ký số chứng chỉ/PKI và mẫu pháp lý.
- SLA, tải đồng thời, dung lượng, browser support và WCAG target.
- Model AI, tài nguyên GPU/CPU, API quota và chính sách dữ liệu.

## Điều kiện merge vào `main`

1. Chạy `scripts/setup.*` trên branch thử nghiệm.
2. Tất cả container healthy và `SMOKE TEST PASSED`.
3. Fresh migration và upgrade migration đều đạt.
4. UAT các luồng: tài khoản/RBAC, khóa học/lộ trình, học, assignment, exam, grading, report, license và backup.
5. Chạy restore drill và xác nhận dữ liệu.
6. Chốt các mục TBD trong `docs/tbd-decisions.md`.

Ma trận nghiệp vụ chi tiết: `docs/BA_CLS_TRACEABILITY_0.8.2.md`. Bản 0.9.0 không đổi contract nghiệp vụ; phạm vi thay đổi giao diện nằm trong `docs/UI_UX_REDESIGN_0.9.0.md`.
