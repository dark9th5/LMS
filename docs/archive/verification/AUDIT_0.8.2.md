# Báo cáo audit và hardening — LMSPilot 0.8.2

Ngày kiểm tra: **03/08/2026**.

## Kết luận

0.8.2 là **full-source release candidate đã hardening**, không phải chứng nhận production và không được mô tả là “100%”. Các luồng học–bài tập–bài thi–chấm điểm–tiến độ–báo cáo đã được nối lại theo dữ liệu thật và exact resource scope. Frontend đã typecheck/build; 96 kiểm thử source/contract/flow đều đạt.

Backend chưa được compile/test bằng Java 21 và chưa chạy cùng PostgreSQL/RabbitMQ trong Docker ở môi trường audit. Vì vậy trạng thái đúng là: **code hoàn thiện đáng kể, đủ để chuyển sang vòng build/integration/UAT trên hạ tầng đích; chưa production-ready**.

## Phát hiện đã sửa

| Mức | Phát hiện | Hoàn thiện trong 0.8.2 |
|---|---|---|
| Nghiêm trọng | Client có thể gửi `completed=true` cho bài Assignment/Exam | Learning Service từ chối; chỉ outcome đã xác minh từ submission/grading/event mới đổi tiến độ |
| Nghiêm trọng | Assignment có thể hoàn thành khóa học ngay lúc nộp, trước khi giảng viên chấp nhận | Lần nộp giữ trạng thái chờ; chỉ lần chấm không trả sửa mới hoàn thành; bài đã chấm bị khóa nộp đè |
| Cao | Course exam không gắn exact enrollment; nhiều lớp cùng khóa có thể trộn attempt/progress | Session/grade/event lưu enrollment, course, lesson; bắt buộc chọn enrollment khi mơ hồ; migration có unique index theo đúng scope |
| Cao | Start/save/submit có race làm trùng attempt hoặc nộp khi đáp án cuối chưa khóa | Advisory lock cho attempt; pessimistic lock cho session khi save/submit; idempotency key được ràng buộc |
| Cao | Phiên hết grace vẫn có thể đi vào grading | Phiên chuyển `EXPIRED` và không phát `EXAM_SUBMITTED`; grading payload từ chối session chưa nộp |
| Cao | `shuffleQuestions`, `shuffleAnswers`, `autoGrade`, score strategy tồn tại nhưng chưa chạy đủ | Shuffle deterministic theo session; manual/auto policy; HIGHEST/LATEST/AVERAGE tính effective score/pass |
| Cao | Competition có thể bắt đầu bằng quyền thi thường | Bắt buộc permission tham gia competition chuyên biệt và vẫn kiểm audience/window |
| Cao | Reporting ghép điểm vào lớp theo course/user mơ hồ | Dùng exact enrollment từ event; event cũ chỉ project khi còn một ứng viên duy nhất |
| Cao | Domain event có thể phát trước DB commit, tạo sự kiện “ma” khi rollback | Publisher trì hoãn gửi tới `afterCommit`; retry grading có thể phát lại event hoàn tất theo hướng idempotent |
| Cao | File course/news/assignment dựa vào purpose/quyền thô; media thiếu byte-range | Grant bền vững theo đúng `(file_id,user_id)` có TTL; owner/purpose/status được xác minh trước khi gắn file; video/audio/PDF hỗ trợ HTTP 206 |
| Cao | Internal content route dùng nhầm policy public/CurrentUser | Tách `internalDownload` sau service-token; public route chỉ owner/admin hoặc exact grant |
| Cao | Sanitizer HTML tin tức kiểu regex blacklist có thể bị bypass và double-escape | Escape-first, restore allowlist tag không thuộc tính, canonical hóa idempotent; response cũ cũng được lọc lại |
| Trung bình | Queue chấm chỉ có exam; assignment thiếu luồng quản trị dùng được | Queue theo tối đa 100 class scope, download grant, chấm/trả sửa và UI hai tab |
| Trung bình | Chấm tự luận không cho thấy đề và câu trả lời | Snapshot prompt/answer đi cùng grade detail và hiển thị trong màn hình chấm |
| Trung bình | Sau nộp exam UI chỉ thử lấy điểm một lần | Poll có backoff/giới hạn và nút làm mới thủ công; xử lý riêng `EXPIRED`/`GRADED` |

## Bằng chứng đã chạy

| Hạng mục | Kết quả |
|---|---|
| Repository validator | PASS — 12 JSON, 28 YAML, 19 service, 18 Flyway service |
| Static/contract/flow regression | **96/96 PASS** |
| Semantic TypeScript | PASS — 65 TS/TSX source files |
| Next.js 16.2.12 production build | PASS |
| Production real-mode guard | PASS — không mock/token fallback, BFF/gateway fail-closed |
| `npm audit --omit=dev` | PASS — 0 vulnerability |
| Backend Gradle compile/test | **CHƯA XÁC MINH** — Java 17; Gradle 8.14.5 không tải được do network |
| Docker/database/broker/E2E | **CHƯA XÁC MINH** — không có Docker CLI/Engine |

## Blocker trước production

1. Chạy `./gradlew clean test` bằng Java 21; xử lý mọi lỗi Kotlin/Spring thực tế.
2. Chạy fresh install và upgrade từ 0.8.1 trên PostgreSQL thật; kiểm dữ liệu cũ trước unique index attempt.
3. Chạy RabbitMQ integration/failure test. Publisher đã tránh pre-commit event nhưng chưa có transactional outbox chung, publisher confirm/replay ledger và DLQ/retry policy hoàn chỉnh cho mọi consumer.
4. Chạy browser E2E ba role, concurrent exam/load test, race/retry/chaos test và accessibility audit.
5. Chạy backup/restore, update/rollback và disaster-recovery drill; chốt RPO/RTO, retention, encryption và offsite copy.
6. Tích hợp malware scanner/magic-byte validation; extension/MIME check hiện không thay thế quét mã độc.
7. Rà soát/paginate các truy vấn `findAll()` ở assessment/reporting/organization cho quy mô production.
8. Hoàn tất pentest, TLS/domain, secret rotation, SMTP/LDAP/provider thật và UAT nghiệp vụ.
9. Nếu yêu cầu chấm theo rubric/per-question hoặc chỉnh điểm assignment có lịch sử phê duyệt, cần bổ sung policy/schema/UI tương ứng.

Chỉ đổi nhãn khỏi release candidate khi các gate trên có log/bằng chứng và PO/BA/Security/Ops cùng nghiệm thu.
