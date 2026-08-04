# Trạng thái bàn giao — LMSPilot CLS 0.16.0

## Phạm vi đã hoàn thành

Bản bàn giao là full source repository. Frontend đã chuyển sang Unified Design System, sửa login/input, tăng khả năng đọc, sửa bố cục bài học và bài thi, giảm menu admin, thay nhiều trường nhập danh sách/số và bổ sung test 0.16.

## Chức năng admin mặc định

Giữ: người dùng & quyền, tổ chức, khóa học, lớp học, bài thi, chấm điểm, kết quả, báo cáo cơ bản và cài đặt.

Loại khỏi Core UI: Learning Path, Live Session, News, Competition, AI Studio, Document Studio, Competency, Certificate, Notification Automation và Operations. Route giao diện của các khu vực này trả 404. Backend được giữ để bảo toàn dữ liệu và làm module tùy chọn; chưa thực hiện migration xóa bảng/service.

## Kết quả xác minh

- Repository validator: PASS.
- Static/contract/UI: 111 passed, 25 skipped, 2 subtests passed.
- TypeScript/TSX syntax: 64 file, 0 lỗi.
- CSS parse: PASS.
- Semantic TypeScript/Next build: chưa xác minh do thiếu npm dependency.
- Full Gradle/Docker/browser E2E: chưa xác minh.

## Điều kiện đưa vào production

1. Chạy `npm ci`, `npm run typecheck`, `npm run build`.
2. Chạy `backend/gradlew test`.
3. Chạy fresh migration và upgrade migration trên bản sao dữ liệu.
4. Chạy Docker smoke và browser E2E.
5. UAT login/đổi mật khẩu, quyền, khóa học, lớp, học, thi, chấm, báo cáo và cài đặt.
6. Kiểm tra accessibility bằng keyboard, zoom 200%, light/dark và browser autofill.
7. Backup và restore drill trước khi thay hệ thống đang chạy.
