# Ma trận truy vết BA + yêu cầu CLS — LMSPilot 0.7.0

Ngày chốt bản mã: **03/08/2026**.

Quy ước:

- **IMPLEMENTED**: có schema/domain, API, kiểm tra quyền và luồng giao diện hoặc client tương ứng trong source.
- **CORE IMPLEMENTED**: luồng cốt lõi đã có, nhưng còn phụ thuộc cấu hình/provider hoặc cần kiểm chứng Docker E2E.
- **PARTIAL**: đã có nền tảng an toàn nhưng chưa đủ toàn bộ tiêu chí BA.
- **CUSTOMER POLICY**: BA để TBD hoặc phải chốt theo hạ tầng khách hàng.

Yêu cầu bổ sung trong cuộc trò chuyện có độ ưu tiên cao hơn khi khác với URD. Vì vậy account type chỉ còn `SYSTEM_ADMIN` và `USER`; Admin/Giảng viên/Học viên là ba role mặc định có thể gán đồng thời, không phải ba loại tài khoản cứng.

## 1. Tài khoản, cơ cấu và phân quyền

| Mã/yêu cầu | Hiện thực chính | Trạng thái | Ghi chú còn lại |
|---|---|---|---|
| UC-AUTH-01 | `identity-service`, JWT/refresh token, audit login, local + LDAP bind | IMPLEMENTED | MFA và chính sách vòng đời token là CUSTOMER POLICY |
| UC-ORG-01 | `organization-service`, cây nhiều cấp, materialized path, membership | IMPLEMENTED | Không xóa cứng đơn vị đang được tham chiếu |
| UC-USER-01 | API/quản trị tài khoản, khóa/ngừng dùng/reset password | IMPLEMENTED | Tài khoản bootstrap được bảo vệ; không có đăng ký công khai |
| UC-USER-02 | `UserImportService`, `UserImportWizard`, CSV/XLSX, mapping, preview, lỗi theo dòng | IMPLEMENTED | Có `CREATE_ONLY/UPSERT`, `ATOMIC/PARTIAL`, operation id và giới hạn giải nén XLSX |
| UC-RBAC-01 | role tùy chỉnh, bulk grants, `ALLOW/DENY`, scope SYSTEM/ORG/COURSE/EXAM | IMPLEMENTED | Backend kiểm tra quyền; UI ẩn nút chỉ là lớp trải nghiệm |
| UC-LDAP-01 | JNDI bind, user DN pattern hoặc manager search, local bootstrap fallback | PARTIAL | Xác thực đã có; đồng bộ tự động OU/group và xử lý xung đột theo lịch cần adapter khách hàng |
| Yêu cầu CLS | Hai account type, ba role mặc định, nhiều role và cấp hàng loạt | IMPLEMENTED | `SYSTEM_ADMIN` duy nhất được bảo vệ |

## 2. Khóa học, nội dung, lớp và học tập

| Mã/yêu cầu | Hiện thực chính | Trạng thái | Ghi chú còn lại |
|---|---|---|---|
| UC-COURSE-01 | danh mục/trạng thái/bộ lọc khóa học | CORE IMPLEMENTED | Taxonomy chi tiết có thể cấu hình thêm theo khách hàng |
| UC-COURSE-02 | tạo/sửa khóa học, mục tiêu, thời lượng, điểm đạt, completion policy | IMPLEMENTED | Có kiểm tra scope tạo/sửa |
| UC-CONTENT-01 | chương/bài, file đa định dạng, upload/version, trình xem | IMPLEMENTED | DOCX/PDF chỉnh trực tiếp phụ thuộc editor được cấu hình |
| UC-COURSE-03 | DRAFT/PUBLISHED/HIDDEN/ARCHIVED, audit | IMPLEMENTED | Khi xuất bản tạo snapshot bất biến |
| UC-CLASS-01 | nhiều lớp/cohort cho một khóa, thời gian, giảng viên, deadline | IMPLEMENTED | Lớp ghim `courseVersion` đã xuất bản |
| UC-ENROLL-01 | ghi danh cá nhân/đơn vị, bulk, idempotency, event | IMPLEMENTED | File import ghi danh riêng có thể bổ sung theo mẫu khách hàng |
| UC-LEARN-01 | trang học cá nhân, trạng thái và tìm kiếm | IMPLEMENTED | Chỉ lấy ghi danh của người hiện tại |
| UC-LEARN-02 | tiến độ server-side, resume position, time spent, completion event | IMPLEMENTED | Có `courseVersion` để lịch sử không đổi theo bản nháp mới |
| UC-ASSIGN-01 | aggregate `assignment_submissions`, file owner validation, attempt/late/grade/feedback | IMPLEMENTED | Rubric nâng cao và phúc khảo là CUSTOMER POLICY |
| Lớp trực tuyến | lịch, provider URL, người tham gia và giảng viên phụ trách | CORE IMPLEMENTED | Provider cụ thể do khách hàng cấu hình |

## 3. Kiểm tra, kỳ thi, cuộc thi và AI

| Mã/yêu cầu | Hiện thực chính | Trạng thái | Ghi chú còn lại |
|---|---|---|---|
| UC-QBANK-01 | ngân hàng câu hỏi, metadata, loại câu, version | IMPLEMENTED | Câu đã dùng được snapshot theo phiên đề |
| UC-EXAM-01 | thời lượng, lịch, lượt thi, điểm đạt, trộn, công bố | IMPLEMENTED | Có quiz trong khóa và standalone exam |
| UC-EXAM-02 | exam session, autosave, idempotent submit, timer | IMPLEMENTED | Grace/reconnect dài và chống gian lận nâng cao là CUSTOMER POLICY |
| UC-GRADE-01 | tự chấm khách quan, queue chấm tay, điểm/nhận xét | IMPLEMENTED | Lịch sử chỉnh điểm và approval nâng cao cần chốt chính sách |
| UC-RETAKE-01 | số lần, khoảng chờ, HIGHEST/LATEST/AVERAGE | CORE IMPLEMENTED | Cần UAT theo chính sách điểm khách hàng |
| UC-AI-01 | provider local/remote, API key mã hóa, job, JSON Schema, review/import | IMPLEMENTED | Model cụ thể và tài nguyên GPU/CPU do khách hàng cung cấp |
| Standalone exam | bài thi không có `courseId`, giao user/group/department/branch | IMPLEMENTED | Khi có audience chỉ đúng đối tượng mới được bắt đầu |
| Competition | đăng ký, leaderboard, tie-break, reward ledger | IMPLEMENTED | Hình thức thưởng thực tế cần adapter nghiệp vụ khách hàng |

## 4. Hoàn thành, chứng chỉ, thông báo và báo cáo

| Mã/yêu cầu | Hiện thực chính | Trạng thái | Ghi chú còn lại |
|---|---|---|---|
| UC-COMP-01 | completion policy + `CourseCompleted` | IMPLEMENTED | Chính sách chi tiết cấu hình theo khóa |
| UC-CERT-01 | cấp/thu hồi/cấp lại, verify code, file chứng chỉ | CORE IMPLEMENTED | Designer template, chữ ký số/QR tùy chuẩn khách hàng |
| UC-NOTI-01 | in-app + email outbox bền vững, retry/backoff/dead state | IMPLEMENTED | SMTP phải được cấu hình; danh sách template/sự kiện theo khách hàng |
| Tin tức tập thể | publish/archive, system/org scope, pin, acknowledge/read | IMPLEMENTED | Đáp ứng phần yêu cầu bổ sung ngoài BA |
| UC-REP-01 | read model event-driven, dashboard và báo cáo theo scope | CORE IMPLEMENTED | KPI chính thức và retention cần chốt |
| UC-REP-02 | CSV UTF-8, chống spreadsheet formula injection, license guard | IMPLEMENTED | Export nền định dạng khác là phần mở rộng |

## 5. License, audit, vận hành và tích hợp

| Mã/yêu cầu | Hiện thực chính | Trạng thái | Ghi chú còn lại |
|---|---|---|---|
| UC-LIC-01 | Ed25519 offline license, machine binding, max users, features, grace/read-only | IMPLEMENTED | Khóa phát hành license thật do đơn vị phát triển quản lý ngoài source |
| UC-AUDIT-01 | audit event/read model, correlation data, quyền xem | CORE IMPLEMENTED | Retention/export audit là CUSTOMER POLICY |
| UC-OPS-01 | health endpoints, Prometheus/Grafana profile, service health UI | PARTIAL | Log tập trung, alerting và distributed tracing đầy đủ cần stack quan sát của khách hàng |
| UC-OPS-02 | backup/restore scripts, job, loopback host agent, heartbeat/lease/result | CORE IMPLEMENTED | Lịch, mã hóa, remote copy, RPO/RTO và restore drill phải chốt |
| UC-OPS-03 | maintenance job và khung update/rollback | PARTIAL | Cố ý **không** chạy lệnh package tùy ý; cần signed-release adapter trước production |
| UC-INTEG-01 | registry, secret reference, HTTP/TCP/TLS/file probe, generic REST | PARTIAL | Mapping/sync thực tế HRM/ERP/NAS theo API của khách hàng |
| UC-CUSTOM-01 | branding, logo/background/colors/domain, feature flags, preview | IMPLEMENTED | Feature flag không được vượt license |
| Redis/dịch vụ ngoài | compose profile tùy chọn và external-service registry | IMPLEMENTED | Stack lõi không phụ thuộc Redis |

## 6. NFR

| Nhóm | Trạng thái source | Điều kiện nghiệm thu còn lại |
|---|---|---|
| On-premise/LAN/container | Có Compose, cấu hình không bắt buộc Internet khi image/cache đã sẵn sàng | Chạy setup trên OS/container runtime mục tiêu |
| Service/data ownership | Mỗi service sở hữu schema; REST/event nội bộ | Kiểm tra migration và contract trong môi trường tích hợp |
| Security | hash password, runtime secret, internal token, scoped RBAC, file validation | Threat model, TLS, at-rest encryption, pentest, malware scanner |
| Reliability | idempotency, retry có giới hạn, outbox/lease ở các luồng mới | Chaos/failure test và dead-letter operations thực tế |
| Observability | health/metrics/correlation, Prometheus/Grafana profile | Central logs/traces/alerts và ngưỡng SLA |
| Backup/DR | script backup/restore + integrity/smoke workflow | RPO/RTO, retention, encryption, offsite copy và restore drill |
| Performance/capacity | license max users, resource limits cơ bản | Load profile, concurrent exams/uploads/reports phải định lượng |
| Accessibility | keyboard focus, contrast mode, reduced motion, responsive | Audit WCAG bằng browser/tool chính thức |

## 7. Kết quả xác minh trong môi trường tạo bản

- `bash scripts/test-static.sh`: **50/50** test đạt.
- JSON/YAML, migration version, gateway route, internal token, permission trace và TypeScript/TSX syntax: đạt.
- Unit test Python cho operations agent allowlist: đạt.
- `git diff --check`: đạt.

Không thể xác nhận Gradle build, npm production build hoặc Docker E2E vì môi trường tạo bản không truy cập đủ dependency registry và không có Docker Engine. Do đó đây là **full-source release candidate**, không phải chứng nhận production.
