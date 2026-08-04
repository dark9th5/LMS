# Ma trận truy vết BA + luồng thực tế — LMSPilot CLS 0.8.2

Ngày chốt source: **03/08/2026**. Các mục không đổi kế thừa chi tiết từ ma trận 0.8.1; bảng dưới đây là trạng thái hiện hành sau audit luồng 0.8.2.

## Quy ước

- **IMPLEMENTED IN SOURCE**: có domain/schema/API/auth/UI hoặc consumer tương ứng; vẫn cần runtime integration/UAT.
- **CORE IMPLEMENTED**: lõi có thật nhưng còn policy/provider/gate nghiệm thu.
- **PARTIAL**: chưa đủ tiêu chí production hoặc còn phần nghiệp vụ chưa chốt.
- **CUSTOMER POLICY**: phụ thuộc hạ tầng/quy định khách hàng.

## Ma trận hiện hành

| Nhóm | Luồng được truy vết | Trạng thái | Còn lại trước production |
|---|---|---|---|
| Account/RBAC | SYSTEM_ADMIN/USER, default/custom multi-role, scoped ALLOW/DENY, protected admin | CORE IMPLEMENTED | Full endpoint scope audit, token invalidation và UAT dữ liệu thật |
| User import | CSV/XLSX, mapping, preview, row policy, atomic/partial, idempotency | IMPLEMENTED IN SOURCE | Load test file lớn/background policy |
| Organization | Cây nhiều cấp, membership nhiều đơn vị, audience/scope resolution | CORE IMPLEMENTED | Scale test và quy tắc merge/chuyển đơn vị |
| Course/version | Draft/publish/hide/archive, immutable snapshot, class/enrollment pin version | IMPLEMENTED IN SOURCE | Fresh/upgrade migration và browser UAT |
| Course content/file | Lesson resource, inline viewer, exact file grant sau course/enrollment scope | CORE IMPLEMENTED | Malware/magic-byte scan, storage encryption/retention |
| Learning progress | Enrollment/course/lesson validation, resume/time/xAPI, server-only Assignment/Exam completion | IMPLEMENTED IN SOURCE | Runtime event convergence/load test |
| Assignment | Upload owner/purpose, attempts/late, scoped queue, grade/feedback/return, completion after acceptance | IMPLEMENTED IN SOURCE | Rubric/per-question/grade-revision policy nếu yêu cầu |
| Exam session | Exact enrollment/course/lesson, deterministic shuffle, autosave/heartbeat/resume/grace, locked/idempotent submit | IMPLEMENTED IN SOURCE | Java compile, PostgreSQL/RabbitMQ integration, concurrent load/browser E2E |
| Grading/retake | Objective/manual grading, prompt+answer detail, HIGHEST/LATEST/AVERAGE effective result, appeal/history | CORE IMPLEMENTED | Rounding/UAT; multi-level approval theo policy |
| Standalone exam | Audience user/group/department/branch, independent attempt scope | IMPLEMENTED IN SOURCE | Runtime audience/expiry UAT |
| Competition | Dedicated permission, registration window, deterministic ranking, reward ledger | CORE IMPLEMENTED | Provider/reward integration; broker failure test |
| Reporting | Event read model; EXAM_GRADED projects exact enrollment/effective score | IMPLEMENTED IN SOURCE | Pagination/load and read-model rebuild/reconciliation drill |
| Notification/news | In-app/email retry; effective exam result; safe HTML; audience-scoped attachments/read/ack | CORE IMPLEMENTED | SMTP/deliverability, DLQ operations, localization policy |
| Certificate | Template/version, issue/revoke/reissue/verify from course completion event | CORE IMPLEMENTED | PKI/legal template; only enable high-value issuance after runtime gates |
| AI | Local/OpenAI-compatible provider, schema/provenance/review/import | CORE IMPLEMENTED | Model, quota, privacy/hardware policy |
| Document editing | File revisions, DOCX callback controls, PDF annotation revision | CORE IMPLEMENTED | OnlyOffice/Collabora integration and malware scan |
| License | Offline Ed25519, binding/capacity/features/grace/read-only | IMPLEMENTED IN SOURCE | Signing/issuing operation outside source; UAT clock/hardware change |
| Operations/DR | Health/metrics, backup/restore, host-agent allowlist, maintenance | PARTIAL | Docker drill, signed update bundle, restore/rollback/RPO/RTO |
| Reliability | Idempotency, DB locks, after-commit event dispatch, consumer dedup ở các read model | PARTIAL | Transactional outbox/publisher confirms/replay/DLQ/chaos test |
| Security | Same-origin BFF, fail-closed gateway/auth/scope, object file ACL, strict news sanitizer | CORE IMPLEMENTED | SAST/DAST/pentest, TLS, per-service identity, malware scan |
| Performance/accessibility | Bounded inputs/queues, responsive/reduced-motion UI | PARTIAL | Paginate remaining `findAll`, load profile, browser/WCAG audit |

## Bằng chứng 0.8.2

- 96/96 static/contract/flow tests — PASS.
- 65 TS/TSX semantic typecheck và Next.js production build — PASS.
- Repository/production-real-mode validators — PASS.
- npm production audit — 0 vulnerability.
- Backend Java 21 compile, Docker E2E, migration upgrade, load, restore, pentest và UAT — **chưa xác minh trong môi trường này**.

Xem `AUDIT_0.8.2.md`, `BUILD_VERIFICATION_0.8.2.md` và `../DELIVERY_STATUS.md`.
