# Changelog

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
