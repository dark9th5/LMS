# Trạng thái bàn giao — LMSPilot CLS 0.15.0

## Thay đổi trọng tâm 0.15.0

Bản này chuyển toàn bộ luồng danh tính sang permission-first: hai account type, gói quyền có thể ghép, metadata/risk/scope cho permission, preview trước khi cấp, giải thích nguồn quyền, thu hồi từng assignment và loại bỏ role-name gating khỏi backend service. Không có thay đổi schema bắt buộc; bootstrap đồng bộ các gói hệ thống khi identity-service khởi động. Token phát hành mới có thêm `globalPermissions`, vì vậy người dùng đang đăng nhập cần đăng xuất/đăng nhập lại sau nâng cấp.

Xác minh hiện tại: 127 test Python đạt; 62 tệp TS/TSX parse đạt; platform-contracts Kotlin compile đạt. Full npm build, Gradle multi-module build và Docker E2E chưa thể chạy trong môi trường đóng gói do thiếu artifact dependency/distribution.

## Phạm vi bản bàn giao

Đây là **repository đầy đủ**, không phải overlay. Source hợp nhất LMSPilot gốc, URD/BA và các yêu cầu bổ sung trong cuộc trò chuyện. Trạng thái chính xác là **full-source release candidate**: schema, domain, API, quyền, giao diện, script và test tĩnh đã được hợp nhất; chưa phải chứng nhận production trên mọi hạ tầng.

## Điểm hoàn thiện nổi bật

- Hai account type; 10 gói quyền hệ thống có thể ghép, gói tùy chỉnh, permission trực tiếp và scoped `ALLOW/DENY`.
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
- Theme Studio cho System Admin quản lý 10 giao diện: tìm kiếm/lọc, xem thử toàn trang không ghi dữ liệu, hoàn tác và áp dụng có xác nhận qua API thật.
- Catalog hiện hành phục vụ nhiều tính cách và bối cảnh: 2 doanh nghiệp, 3 giáo dục, 1 tổ chức, 2 sáng tạo và 2 tối giản; chỉ 2 preset tối, 8 preset sáng để ưu tiên khả năng đọc trong môi trường làm việc/học tập.
- 10 preset thay cả palette, font, geometry, density, surface, sidebar, hero và data cards; scenery fantasy/không gian cũ không còn trong runtime hiện hành.
- `themeKey` được lưu ở Configuration Service, kiểm allowlist tại API/Flyway và đọc từ public branding ở SSR; Flyway V5/V6 giữ upgrade path qua hai lần đổi catalog/default.
- Sidebar mới là accordion hai tầng permission-aware: ba nhóm chính Học tập/Đánh giá/Quản trị, tự mở route hiện tại, chỉ một nhóm mở và mục con vẫn lọc theo quyền.
- Command palette là tìm kiếm điều hướng thật bằng bàn phím và vẫn lọc theo quyền; login, dashboard, khóa học, lớp, học tập, kỳ thi, đổi mật khẩu và workspace nghiệp vụ dùng chung contract giao diện mới.
- Responsive mobile được kiểm tra ở 390 px; focus-visible, reduced-motion và semantic ARIA của accordion được giữ.
- Soft Spectrum là default mới: multi-colour chỉ nằm ở content; sidebar và mục lục dùng một palette navy–graphite–xám–trắng trên mọi nhóm và route.
- KPI, summary, course cover, class list và exam scene chuyển sang pastel có viền/tương phản rõ; mảng dark-cosmic còn sót trong các catalog đã được loại bỏ.
- Flyway V6 đổi `enterprise-blue` sang `soft-spectrum` mà không ghi đè custom palette; seed branding fantasy chỉ được neutralize khi còn nguyên tên mặc định cũ.

## Kết quả kiểm tra lại trên source 0.15.0

- Repository validator: **PASS** — 24 JSON, 28 YAML, 19 service, 18 service dùng Flyway.
- Python static/contract/flow/UI regression: **127 passed, 2 subtests passed**.
- Permission catalog: **93/93 permission** có metadata; catalog dùng chung compile bằng `kotlinc`.
- Backend role-name gate scan: không còn `CurrentUser.roles()` trong business services.
- TypeScript/TSX syntax transpilation: **62 tệp, 0 lỗi cú pháp**.
- Shell syntax: **PASS**.

Không tái sử dụng kết quả build 0.14 để khẳng định 0.15 đã build thành công. Semantic TypeScript, Next production build, Gradle multi-module build, Docker smoke và browser E2E phải được chạy lại trên máy đích.

## Phần còn bị giới hạn bởi môi trường

- Gradle wrapper distribution/dependency không tải được trong môi trường đóng gói; chưa chạy full backend compile/test.
- `npm ci` bị registry trả 404 cho `undici-types-7.16.0.tgz`; chưa chạy semantic typecheck hoặc Next production build cho 0.15.
- Docker stack không chạy, nên chưa smoke migration/integration hoặc browser E2E gắn backend thật.

Vì vậy chưa thể tuyên bố đã xác nhận:

- Gradle compile/test cho toàn bộ service.
- PostgreSQL/RabbitMQ/Redis/OnlyOffice/local AI integration.
- Browser E2E gắn backend thật, Firefox/WebKit, Lighthouse/WCAG chính thức, load test, pentest, restore drill và UAT.

Đây là giới hạn xác minh, không phải bằng chứng backend đã build thành công. Chi tiết ở `docs/BUILD_VERIFICATION_0.14.0.md`, `docs/SOFT_SPECTRUM_0.14.0.md` và `docs/AUDIT_0.8.2.md`.

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

Ma trận nghiệp vụ chi tiết: `docs/BA_CLS_TRACEABILITY_0.8.2.md`. Thiết kế quyền 0.15 nằm trong `docs/PERMISSION_FIRST_0.15.0.md`; giao diện nền và migration theme 0.14 được giữ như lịch sử tại `docs/SOFT_SPECTRUM_0.14.0.md`.
