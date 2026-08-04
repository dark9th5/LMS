# Changelog

## 0.9.0 - 2026-08-04

- Tái thiết kế toàn bộ portal thành Astral Academy V3 với hệ token, chiều sâu không gian, typography, glass surface, bản đồ thiên thể và chuyển động có chủ đích.
- Thay mới shell điều hướng, sidebar phân nhóm/đánh số, top bar theo ngữ cảnh, trạng thái realm và Command Atlas mở bằng `Ctrl/Cmd + K` với kết quả lọc theo quyền thật.
- Dựng lại hoàn toàn login, dashboard, khóa học, lớp học, hành trình học tập, kỳ thi, đổi mật khẩu và header dùng chung; làm giàu trạng thái, số liệu, hành động và phân cấp thông tin.
- Phủ lại các workspace nghiệp vụ, bảng, form, modal và console quản trị bằng design system mới nhưng giữ nguyên API, quyền và luồng dữ liệu thật.
- Bổ sung responsive cho sáu dải màn hình, focus-visible, high-contrast và `prefers-reduced-motion`; không dùng asset thị giác ngoài hệ thống.
- Thêm 8 UI regression test; tổng suite đạt 104 test, semantic TypeScript, Next.js production build và npm audit 0 vulnerability.

## 0.8.2 - 2026-08-03

- Gắn phiên thi, lượt làm, chấm điểm và reporting vào đúng enrollment/course/lesson; khóa race khi bắt đầu, lưu đáp án và nộp bài.
- Chuyển hoàn thành Assignment/Exam sang kết quả do máy chủ xác minh; bài thực hành chỉ hoàn thành sau khi giảng viên chấp nhận.
- Hiện thực shuffle, auto/manual grade, HIGHEST/LATEST/AVERAGE, kết quả hiệu lực và cập nhật tiến độ/report/notification nhất quán.
- Bổ sung hàng đợi chấm bài thực hành theo đúng lớp, xem file, trả chỉnh sửa và giao diện chấm câu tự luận có đề/câu trả lời.
- Thay quyền file theo purpose bằng grant chính xác theo file-người dùng cho course, assignment và news; kiểm owner/purpose khi gắn file; hỗ trợ HTTP byte-range cho video/audio/PDF.
- Gia cố HTML tin tức bằng sanitizer allowlist idempotent và bổ sung upload/download tệp đính kèm theo audience.
- Trì hoãn phát domain event đến sau DB commit, nối kết quả chấm sang Learning và đồng bộ trạng thái phiên thi.
- Mở rộng regression suite lên 96 test; frontend typecheck/build và npm audit vẫn đạt.

## 0.8.1 - 2026-08-03

- Sửa lỗi type contract Learning Path và thay TypeScript syntax-only checker bằng semantic project typecheck.
- Xác minh clean `npm ci`, Next.js production build và đưa `npm audit` về 0 known vulnerability.
- Đồng bộ Spring Boot 3.5.16/Spring Cloud 2025.0.3, bật lại compatibility verifier và dùng `npm ci` trong Dockerfile.
- Khóa bulk idempotency bằng DB pessimistic lock; ràng buộc replay theo operation type và người thực hiện.
- Chặn cross-origin mutation trong BFF, thêm security headers, loại debug login và IP LAN hard-code.
- Gia cố File Storage theo owner/purpose, giới hạn edit/version access và tải callback editor có timeout/size/no-redirect.
- Mở rộng regression suite lên 80 test; bổ sung báo cáo audit trung thực và production blockers.

## 0.8.0 - 2026-08-03

- Thêm Learning Path nhiều chặng: pin course version, unlock tuần tự, giao theo user/đơn vị, auto-enroll và tổng hợp tiến độ từ Learning Service.
- Thêm KPI reporting theo SELF/ASSIGNED/SYSTEM và thống kê theo khóa học: completion, pass rate, overdue, due soon, activity, progress và score.
- Thêm Notification Template quản trị runtime theo event, preview, audit và kênh in-app/email.
- Thêm due reminder scheduler trước/sau hạn: Reporting read model, service token, idempotent claim, timeout, failure retry và manual run.
- Bổ sung permission/UI/navigation cho learning paths, KPI và notification automation; gia cố xử lý lỗi thao tác xóa.
- Chuẩn hóa source version 0.8.0, Node 22 và `npm ci` trong CI.
- Mở rộng static/contract/unit suite lên 73 test và cập nhật ma trận truy vết/tài liệu build.

## 0.7.0 - 2026-08-03

- Hoàn thiện import tài khoản CSV/XLSX với mapping, preview, lỗi từng dòng, create/upsert, atomic/partial, idempotency, capacity lock và chống XLSX zip expansion.
- Cưỡng chế license offline trên feature, export, tác vụ ghi và giới hạn active users; bổ sung grace period/read-only.
- Thêm snapshot khóa học bất biến và ghim course version xuyên lớp, tiến độ và trình học.
- Tách bài thực hành thành aggregate submission riêng, kiểm tra file owner/purpose, attempt, late status, queue chấm và feedback.
- Bổ sung LDAP/AD authentication an toàn với local protected-admin fallback, timeout và LDAP filter escaping.
- Bổ sung email outbox có lease/retry/dead-letter state và operations host agent có allowlist, claim token, heartbeat/lease.
- Nâng test connection của integration registry theo HTTP/TCP/TLS/file; cấm credentials trong endpoint.
- Nối trình nhập file thật vào giao diện quản trị; mở rộng static/contract suite lên 50 test.
- Thêm ma trận truy vết BA+CLS và operations runbook; giữ trạng thái release candidate do chưa chạy full build/Docker E2E trong môi trường tạo bản.

## 0.5.0 - 2026-08-03

- Chuyển account model sang `SYSTEM_ADMIN`/`USER`, giữ Admin/Giảng viên/Học viên làm role mặc định và hỗ trợ role tùy chỉnh.
- Thêm scoped RBAC, bulk account/grant, protected bootstrap admin và cơ cấu tổ chức nhiều cấp.
- Thêm giao khóa học/bài thi theo user hoặc đơn vị, deadline/progress/live session, standalone exam và competition/reward.
- Thêm branding động, tin tức, external-service registry, Redis tùy chọn và gia cố secret runtime.
- Thêm AI local/API với JSON Schema chung, provenance, review/import câu hỏi.
- Thêm file version, DOCX edit session/callback và PDF annotation/version.
- Thiết kế lại portal theo phong cách Học viện Huyền Tri, permission-first, responsive và reduced-motion.
- Mở rộng static suite lên 30 requirement/contract tests và kiểm tra Flyway trùng version.

## 0.4.0 - 2026-07-30

- Thiết kế lại portal theo luồng LMS nhiều trang, tăng kích thước chữ, khoảng cách và khả năng đọc trên desktop/mobile.
- Tách riêng danh sách/chi tiết khóa học, lớp, trình học, bài kiểm tra và hàng chờ chấm; loại bỏ trải nghiệm dồn mọi dữ liệu trong một trang.
- Nối giao diện với API thật cho tạo khóa học, thêm/sửa bài học hoặc tệp, xuất bản, tạo lớp, ghi danh, tiến độ, nộp bài thực hành, thi và chấm thủ công.
- Thêm trình thi có tự lưu định kỳ, đồng hồ, tự nộp khi hết giờ, idempotency và trạng thái kết quả.
- Sửa lỗi dùng nhầm giao diện vai trò bằng phản hồi user sau login, cookie refresh dùng toàn site, điều hướng theo role và render động không cache.
- Ẩn thanh cuộn của sidebar và mục lục khóa học nhưng vẫn giữ cuộn bằng chuột/bàn phím; bổ sung xem PDF và phát video/audio trực tiếp từ File Storage Service.
- Bổ sung smoke test API/web theo ba vai trò và 14 test contract cho role routing, API thật, lesson editor, inline file, idempotency và quy tắc scrollbar.

## 0.3.0 - 2026-07-30

- Gia cố quy trình cài đặt một lệnh với setup, preflight và smoke test cho Windows/Linux.
- Sửa dependency/version, Gradle Wrapper/checksum, AI YAML và endpoint Organization.
- Loại bỏ secret runtime mặc định, tách mật khẩu database và bổ sung kiểm tra cấu hình bắt buộc.
- Nâng React/React DOM lên 19.2.6 và khóa chính xác version frontend.
- Gia cố file upload, CSV export, SMTP, exception logging và trạng thái khóa học khi cập nhật tiến độ.
- Thêm BuildKit cache, graceful shutdown, log rotation và bộ test contract của repository.
- Viết lại README, trạng thái bàn giao và checklist UAT/production theo đúng mức đã xác minh.

## 0.2.0 - 2026-07-30

- Thay dashboard và màn hình danh sách tĩnh bằng dữ liệu API thật, trạng thái tải/lỗi/rỗng và biểu mẫu cốt lõi.
- Đồng bộ seed demo xuyên suốt Identity, Course, Enrollment, Learning, Assessment, Reporting và Notification.
- Giới hạn khóa học, lớp, tiến độ, câu hỏi, bài thi, hàng chờ chấm và báo cáo theo vai trò/phạm vi; học viên chỉ thấy khóa đã ghi danh, giảng viên chỉ thấy lớp được phân công.
- Chặn học viên xem hoặc bắt đầu bài thi ngoài khóa học được giao; không tạo thêm phiên thi khi vẫn còn phiên đang hoạt động.
- Siết idempotency cho ghi danh, cập nhật tiến độ và nộp bài thi để không trả nhầm dữ liệu khi khóa bị tái sử dụng.
- Sửa tính tiến độ: server tự xác minh ghi danh và số bài bắt buộc, không tin tổng số bài do client gửi lên.
- Sửa dashboard để tách chính xác Chưa bắt đầu, Đang học, Hoàn thành và Quá hạn; dữ liệu tham chiếu phụ không còn làm hỏng toàn bộ màn hình.
- Truyền ngữ cảnh khóa học sang kết quả chấm và chỉ cập nhật báo cáo khi xác định được duy nhất ghi danh phù hợp, không tự gán điểm vào lớp gần nhất.
- Sửa phiên web trong LAN: refresh được cả khi access cookie đã hết hạn, gom các yêu cầu refresh song song, logout giữ đúng hostname/IP, cookie Secure điều khiển riêng cho HTTP/HTTPS; phiên vẫn bị xóa khi token mới tiếp tục bị từ chối.
- Thêm thông báo trong portal, xử lý login lỗi mạng và hoàn thiện kiểu hiển thị responsive/modal.
- Sửa mẫu HTML in chứng chỉ để tạo tài liệu hợp lệ, không còn ký tự escape thừa trong thuộc tính.
- Đưa Audit, Notification, Certificate, Configuration và Operations vào stack mặc định; AI/Integration vẫn tùy chọn.
- Mở rộng validation repository cho version, Flyway migration và service-token của internal API.

## 0.1.0 - 2026-07-30

- Khởi tạo monorepo LMSPilot On-Premise.
- Thêm portal, API Gateway, 18 Kotlin/Spring service, PostgreSQL schema isolation, RabbitMQ, Redis, file storage, observability, backup/restore và tài liệu traceability.
