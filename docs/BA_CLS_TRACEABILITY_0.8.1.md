# Ma trận truy vết BA + yêu cầu mở rộng — LMSPilot CLS 0.8.1

Ngày chốt source: **03/08/2026**.

## Quy ước

- **IMPLEMENTED**: có schema/domain, API, kiểm tra quyền và UI/client tương ứng trong source.
- **CORE IMPLEMENTED**: luồng lõi đã có nhưng còn phụ thuộc provider/cấu hình hoặc cần Docker E2E.
- **PARTIAL**: có nền tảng an toàn nhưng chưa đủ toàn bộ tiêu chí URD.
- **CUSTOMER POLICY**: URD để TBD hoặc phải chốt theo hạ tầng khách hàng.
- **NOT IN 1.0 BASELINE**: không có trong phạm vi hiện tại hoặc cần dự án tích hợp riêng.

Yêu cầu bổ sung trong cuộc trò chuyện được ưu tiên khi khác URD: account type chỉ còn `SYSTEM_ADMIN` và `USER`; Admin/Giảng viên/Học viên là role mặc định có thể gán đồng thời.

## 1. Tài khoản, cơ cấu và phân quyền

| Mã/yêu cầu | Hiện thực | Trạng thái | Còn lại |
|---|---|---|---|
| UC-AUTH-01 | JWT/refresh session, login/logout audit, password policy, forced change, lockout, revoke session, local/LDAP | IMPLEMENTED | MFA và thời hạn token cuối cùng là CUSTOMER POLICY |
| UC-ORG-01 | Cây tổ chức nhiều cấp, materialized path, membership nhiều đơn vị, scope resolution | IMPLEMENTED | Quy tắc merge/chuyển đơn vị hàng loạt cần UAT dữ liệu thật |
| UC-USER-01 | Tạo/sửa/khóa/vô hiệu/reset password; protected bootstrap account | IMPLEMENTED | Không xóa lịch sử; không có public registration |
| UC-USER-02 | CSV/XLSX upload, mapping, preview, row validation, error report, create/upsert, atomic/partial | IMPLEMENTED | File cực lớn nên chuyển background job theo capacity thực tế |
| UC-RBAC-01 | Role tùy chỉnh, bulk grant/revoke, `ALLOW/DENY`, scope SYSTEM/ORG/COURSE/CLASS/EXAM; bulk replay khóa theo actor/type | CORE IMPLEMENTED | Audit mọi endpoint chỉ dùng coarse JWT gate và UAT token/session invalidation |
| UC-LDAP-01 | Bind bằng DN pattern hoặc manager search, timeout, filter escaping, local admin fallback | PARTIAL | OU/group sync, conflict workflow và scheduler theo AD thật |
| Yêu cầu mở rộng | Hai account type, ba default role, nhiều role, cấp đồng thời nhiều user | IMPLEMENTED | — |

## 2. Khóa học, nội dung, lớp và lộ trình

| Mã/yêu cầu | Hiện thực | Trạng thái | Còn lại |
|---|---|---|---|
| UC-COURSE-01 | Danh mục, trạng thái, tìm/lọc khóa học | CORE IMPLEMENTED | Taxonomy sâu theo khách hàng |
| UC-COURSE-02 | Metadata, mục tiêu, thời lượng, điểm đạt, completion rule, scope owner/instructor | IMPLEMENTED | UAT rule phức tạp |
| UC-CONTENT-01 | Chương/bài, file đa định dạng, inline viewer, file version, owner/purpose guard | CORE IMPLEMENTED | Cần ACL theo enrollment/class/resource; SCORM chưa phải baseline |
| UC-COURSE-03 | Draft/publish/hide/archive; snapshot bất biến | IMPLEMENTED | Upgrade migration cần kiểm chứng database thật |
| UC-CLASS-01 | Nhiều lớp trên một khóa; thời gian, instructor, deadline; pin course version | IMPLEMENTED | Điểm danh là extension |
| UC-ENROLL-01 | Ghi danh user/đơn vị, bulk/idempotency/event | IMPLEMENTED | File enrollment riêng có thể bổ sung theo mẫu khách hàng |
| UC-LEARN-01 | Trang học cá nhân, phân nhóm trạng thái, resume | IMPLEMENTED | Browser E2E |
| UC-LEARN-02 | Server kiểm enrollment/course/lesson, position, time spent, completion event, xAPI | PARTIAL | Completion ASSIGNMENT/EXAM còn nhận cờ client; phải nối outcome đã xác minh |
| UC-ASSIGN-01 | Assignment/submission/attempt/file/late/grade/feedback | IMPLEMENTED | Rubric chi tiết theo tiêu chí có thể mở rộng |
| Lộ trình học tập | Path/items/assignment/participants, sequential unlock, auto-enroll, progress | IMPLEMENTED | Rule prerequisite phức tạp ngoài `AFTER_PREVIOUS` là extension |
| Lớp trực tuyến | Schedule/provider URL/participants | CORE IMPLEMENTED | Adapter Zoom/Teams/Jitsi cụ thể theo khách hàng |
| Discussion | Thread/reply/moderation theo course scope | IMPLEMENTED | Realtime websocket là extension |

## 3. Kiểm tra, chấm điểm, cuộc thi và AI

| Mã/yêu cầu | Hiện thực | Trạng thái | Còn lại |
|---|---|---|---|
| UC-QBANK-01 | Loại câu hỏi, metadata, provenance, version/snapshot | IMPLEMENTED | Matching/drag-drop nếu khách hàng yêu cầu |
| UC-EXAM-01 | Blueprint/câu hỏi, duration, window, attempts, pass score, shuffle, result policy | IMPLEMENTED | Proctoring nâng cao ngoài phạm vi |
| UC-EXAM-02 | Session, autosave, heartbeat, resume, grace, idempotent submit, security events | IMPLEMENTED | Load test và mất mạng dài cần UAT |
| UC-GRADE-01 | Auto-grade khách quan, manual queue, detail/total, feedback | IMPLEMENTED | Approval nhiều cấp là CUSTOMER POLICY |
| UC-RETAKE-01 | Limit/wait và highest/latest/average | CORE IMPLEMENTED | UAT cách làm tròn/công bố |
| Grade appeal | Lịch sử chỉnh điểm, appeal/resolution/audit | IMPLEMENTED | SLA phúc khảo là CUSTOMER POLICY |
| Standalone exam | Không cần course; audience user/group/department/branch | IMPLEMENTED | — |
| Competition | Registration, leaderboard, tie-break, reward ledger | IMPLEMENTED | Adapter thưởng vật chất/HRM theo khách hàng |
| UC-AI-01 | Local/remote provider, encrypted key, JSON Schema, provenance, review/import | IMPLEMENTED | Model/hardware/quota và privacy policy theo khách hàng |

## 4. Hoàn thành, năng lực, chứng chỉ, thông báo và báo cáo

| Mã/yêu cầu | Hiện thực | Trạng thái | Còn lại |
|---|---|---|---|
| UC-COMP-01 | Completion policy/event và progress read model | PARTIAL | Không cấp chứng chỉ giá trị cao trước khi completion ASSIGNMENT/EXAM đến từ kết quả server |
| Competency | Catalog, proficiency level, profile, assessment/gap/readiness | CORE IMPLEMENTED | Mapping tự động từ course outcome cần chính sách |
| UC-CERT-01 | Template, issue/revoke/reissue, immutable snapshot, verify/print | CORE IMPLEMENTED | PKI/chữ ký số và QR chuẩn pháp lý theo khách hàng |
| UC-NOTI-01 | In-app/email outbox, retry/backoff/lease/dead state | IMPLEMENTED | SMTP thật và deliverability test |
| Notification template | Template theo event, runtime preview/edit, audit | IMPLEMENTED | Localization nhiều ngôn ngữ là extension |
| Due reminder | Rule trước/sau hạn, UTC scheduler, read model, idempotent claim, retry/timeout | IMPLEMENTED | Calendar/business-day rule là extension |
| Tin tức | System/org audience, pin, publish/archive, read/ack | IMPLEMENTED | — |
| UC-REP-01 | Read model, dashboard, scoped KPI/course KPI | IMPLEMENTED | KPI cuối cùng cần PO/BA phê duyệt |
| UC-REP-02 | CSV/export guard và scheduled background export | CORE IMPLEMENTED | XLSX/PDF rendering lớn cần integration/load test |

## 5. License, audit, vận hành, tích hợp và tùy biến

| Mã/yêu cầu | Hiện thực | Trạng thái | Còn lại |
|---|---|---|---|
| UC-LIC-01 | Signed Ed25519 offline license, binding, limits, features, grace/read-only | IMPLEMENTED | Private signing key/issuing tool quản lý ngoài source |
| UC-AUDIT-01 | Event/read model, filters, correlation, export permission | CORE IMPLEMENTED | Tamper-evident/WORM storage và retention theo khách hàng |
| UC-OPS-01 | Health endpoints/UI, version inventory, metrics profile, correlation ID | PARTIAL | Central logs/tracing/alerts cần observability stack thật |
| UC-OPS-02 | Backup/restore scripts, schedule, host agent, lease, fixed allowlist | CORE IMPLEMENTED | Encryption/offsite/retention/RPO/RTO/restore drill |
| UC-OPS-03 | Maintenance mode, job framework, fail-closed update/rollback | PARTIAL | Signed release bundle adapter và rollback drill |
| UC-INTEG-01 | Registry, encrypted secret, HTTP/TCP/TLS/file probe, generic REST | PARTIAL | Mapping/sync HRM/ERP/NAS/SMTP cụ thể |
| UC-CUSTOM-01 | Dynamic branding/theme/domain/feature flags | IMPLEMENTED | License entitlement vẫn là upper bound |
| Redis/service ngoài | Optional profiles; core không phụ thuộc Redis | IMPLEMENTED | Sizing/HA theo hạ tầng |

## 6. NFR

| Nhóm | Source hiện có | Nghiệm thu còn lại |
|---|---|---|
| On-premise/container | Compose, generated runtime secrets, LAN-oriented config | Build/run trên OS/container runtime mục tiêu |
| Service ownership | 19 service, schema riêng, REST/event/read model | Contract/integration tests thật |
| Security | Password hash, scoped RBAC, service token, same-origin BFF guard, file owner/purpose validation, secret encryption | Exact resource ACL, TLS, at-rest encryption, threat model, SAST/DAST/pentest, malware adapter |
| Reliability | Idempotency, outbox/lease, bounded retry/timeout | Failure/chaos test, broker DLQ operations |
| Observability | Health, metrics profile, correlation ID | Central logs, OpenTelemetry trace và alert thresholds |
| Backup/DR | Backup/restore/manifest/agent/schedule | Restore drill, encryption, offsite copy, RPO/RTO |
| Performance | Capacity enforcement và resource limits cơ bản | Concurrent exams/uploads/reports/load profile |
| Accessibility | Focus, responsive, contrast, reduced motion | WCAG audit bằng browser/tool chính thức |

## 7. Xác minh bản 0.8.1

- Repository validator: 11 JSON, 28 YAML, 19 service, 18 Flyway service — PASS.
- Static/contract regression suite: **80/80 PASS**.
- Semantic TypeScript typecheck, clean npm ci, Next production build và shell syntax: PASS.
- `npm audit`: 0 known vulnerability.
- Archive cuối được giải nén và chạy lại validator/test.
- Backend Gradle/Docker E2E chưa thể chạy trong môi trường tạo bản; xem `BUILD_VERIFICATION_0.8.1.md` và `AUDIT_0.8.1.md`.

## 8. Quyết định còn phải chốt

Các mục URD TBD vẫn phải được phê duyệt: SLA/quy mô, mật khẩu/MFA/token, import policy, versioning, assignment late policy, exam disconnect/grace/result disclosure, rounding/appeal, certificate/PKI, notification events, KPI/retention, license grace, integration mappings, encryption/scanning, OS/database/broker/container version matrix.
