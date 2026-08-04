# Báo cáo audit và hardening — LMSPilot CLS 0.8.1

Ngày kiểm tra: **03/08/2026**.

## Kết luận điều hành

Bản 0.8.0 ban đầu **chưa đạt 100%**. Bộ kiểm thử cũ chủ yếu đối chiếu chuỗi/source và chỉ transpile từng file TypeScript, nên đã bỏ sót một lỗi hợp đồng dữ liệu làm `tsc` thất bại. Bản 0.8.1 đã sửa lỗi đó, nâng kiểm tra TypeScript thành kiểm tra ngữ nghĩa toàn project, build được frontend production và gia cố một số điểm bảo mật/idempotency có thể chứng minh trực tiếp.

Trạng thái đúng của 0.8.1 vẫn là **full-source hardened release candidate**, chưa phải chứng nhận production. Không thể hợp lý tuyên bố “100%” khi backend chưa compile/test bằng Java 21, migration và 19 service chưa chạy trên PostgreSQL/RabbitMQ thật, Docker/browser/load/pentest/restore/UAT chưa được thực hiện.

## Sai sót đã tìm thấy và đã sửa

| Mức | Vấn đề ở 0.8.0 | Hoàn thiện trong 0.8.1 |
|---|---|---|
| Cao | `LearningPathCenter` dùng `itemId` nhưng type khai báo kế thừa trường `id`; `tsc` thất bại | Đồng bộ `MyPathItem` với DTO backend; `tsc` và Next production build đạt |
| Cao | Script TypeScript chỉ dùng `transpileModule`, không phát hiện lỗi type/cross-file | Dùng `createProgram` + `getPreEmitDiagnostics` trên `tsconfig.json` |
| Cao | `operationId` bulk có thể replay giữa người dùng hoặc loại thao tác; các bulk mutation có race | Thêm DB pessimistic lock chung, kiểm tra `operationType` và `requestedBy`, migration V5 và test hồi quy |
| Cao | Cookie-backed BFF thiếu chặn request cross-origin rõ ràng | Kiểm tra `Origin`/`Sec-Fetch-Site` cho login, logout, đổi mật khẩu và mọi mutation proxy |
| Cao | Frontend có 3 advisory mức high qua `postcss`/`sharp` | Nâng Next, khóa override đã vá; `npm audit` hiện 0 vulnerability |
| Cao | Public file route chỉ kiểm quyền chung, chưa kiểm object/purpose; edit history có thể mở quá rộng | Thêm owner/admin/purpose policy; giới hạn edit/version history; internal route vẫn bắt buộc service token |
| Cao | Callback editor có thể tải response không giới hạn, theo redirect và chỉ so hostname | Khóa scheme/host/effective port, cấm userinfo/redirect, timeout, kiểm `Content-Length` và giới hạn stream |
| Trung bình | Spring Boot 3.4.1 lệch Spring Cloud 2025.0.x; compatibility verifier bị tắt | Đồng bộ Boot 3.5.16, Cloud 2025.0.3 và bật lại verifier mặc định |
| Trung bình | Docker frontend dùng `npm install`, không tái lập tuyệt đối | Chuyển sang `npm ci` và đã xác minh cài sạch từ lockfile |
| Trung bình | IP LAN cá nhân và log debug login nằm trong source production | Loại bỏ IP/log; thêm security headers cơ bản |
| Thấp | Validator quét `node_modules`/`.next`; `tsbuildinfo` có thể lọt vào release | Loại generated directories, ignore `*.tsbuildinfo` và làm sạch gói phát hành |

## Kết quả xác minh trực tiếp

| Hạng mục | Kết quả |
|---|---|
| ZIP 0.8.0 đầu vào | Kiểm tra cấu trúc an toàn, không path traversal/symlink; SHA-256 khớp file đi kèm |
| Repository validator | PASS — 11 JSON, 28 YAML, 19 service, 18 Flyway service |
| Static/contract regression | **80/80 PASS** |
| TypeScript semantic typecheck | PASS — 65 TS/TSX source files |
| `npm ci` từ lockfile | PASS |
| Next.js production build | PASS — Next 16.2.12 |
| `npm audit` | PASS — 0 known vulnerability ở mọi mức |
| Shell syntax | PASS |
| Backend Gradle compile/test | **CHƯA XÁC MINH** — host chỉ có Java 17, không có Gradle cache và không tải được distribution |
| Docker Compose + database/broker | **CHƯA XÁC MINH** — Docker Engine không có trong môi trường audit |
| Browser E2E/load/pentest/restore/UAT | **CHƯA XÁC MINH** |

## Blocker trước production

1. Chạy `./gradlew test` bằng Java 21 và xử lý mọi lỗi compile/test thực tế; hiện không có bằng chứng backend build thành công.
2. Chạy fresh install và upgrade migration trên PostgreSQL thật; kiểm tra RabbitMQ event, retry, idempotency và read-model convergence.
3. Hoàn thiện authorization cấp tài nguyên cho file course/news/assignment bằng quan hệ enrollment/class/resource, thay vì chỉ owner/purpose/coarse permission. Bản 0.8.1 đã chặn các truy cập tùy tiện rõ ràng nhưng chưa thay thế một ACL tài nguyên đầy đủ.
4. Rà soát toàn bộ endpoint chỉ dựa vào `@PreAuthorize`: JWT hiện chứa cả permission scoped để làm coarse gate; mọi resource endpoint phải có exact-scope check tại service.
5. Completion của lesson loại `ASSIGNMENT`/`EXAM` cần được phát từ kết quả nộp/chấm/thi đã xác minh. API progress hiện vẫn nhận cờ hoàn thành từ client; chưa nên dùng sự kiện này để cấp chứng chỉ có giá trị cao.
6. Tích hợp malware scanner/magic-byte validation, TLS, at-rest encryption, secret rotation và pentest. Extension/MIME check hiện không tương đương quét mã độc.
7. Chạy browser E2E cho ba role, concurrent exam/load test, backup/restore drill, accessibility audit và UAT có biên bản.

## Tiêu chí được phép gắn nhãn production

Chỉ đổi trạng thái khỏi release candidate khi toàn bộ blocker trên có bằng chứng, mọi container healthy, `SMOKE TEST PASSED`, restore drill thành công và PO/BA/Security/Ops cùng ký nghiệm thu. “80/80 PASS” là bằng chứng regression source/frontend, không phải chứng nhận toàn hệ thống.
