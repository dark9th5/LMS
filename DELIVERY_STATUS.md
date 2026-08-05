# Trạng thái bàn giao — LMSPilot CLS 0.17.0

## Phạm vi đã hoàn thành

Bản bàn giao là full source repository, không phải patch. Frontend đã được chuyển sang một Unified Design System; shell, dashboard, login, catalog khóa học, workspace học và workspace thi dùng chung typography, token, spacing, control state và responsive behavior.

Đã xóa khỏi runtime toàn bộ component, stylesheet, asset và nội dung Cosmic/Fantasy cũ. Bộ ảnh tại `docs/screenshots/0.17.0` được tạo lại từ source 0.17, không tái sử dụng ảnh giao diện 0.16.

## Admin Core

Giữ trong giao diện mặc định: Tổng quan, Học tập của tôi, Khóa học, Lớp học, Bài kiểm tra & kỳ thi, Chấm điểm, Báo cáo, Người dùng, Tổ chức và Cài đặt.

Loại khỏi Core UI: Learning Path, Live Session, News, Competition, AI Studio, Document Studio, Competency, Certificate, Notification Automation và Operations. Backend được giữ để bảo toàn dữ liệu và có thể phát hành thành module tùy chọn; chưa thực hiện migration xóa bảng/service.

## Kết quả xác minh

- Repository validator: PASS.
- Static/contract/UI: 115 test đạt, 34 test lịch sử skip có chủ ý (149 test tổng cộng).
- TypeScript/TSX syntax: 62 file, 0 lỗi.
- CSS parse: PASS.
- Visual preview: 7 ảnh render đạt.
- Semantic TypeScript/Next build: chưa xác minh do npm dependency không có trong cache.
- Full Gradle: chưa xác minh do Gradle distribution không tải được.
- Docker/API browser E2E: chưa xác minh.

## Điều kiện đưa vào production

1. Chạy `npm ci`, `npm run typecheck`, `npm run build`.
2. Chạy `backend/gradlew test` bằng Java 21.
3. Chạy fresh migration và upgrade migration trên bản sao dữ liệu.
4. Chạy Docker smoke và browser E2E kết nối API thật.
5. UAT login/đổi mật khẩu, quyền, khóa học, lớp, học, thi, chấm, báo cáo và cài đặt.
6. Kiểm tra keyboard, zoom 200%, light/dark, autofill và nội dung tiếng Việt dài.
7. Backup và restore drill trước khi thay hệ thống đang chạy.
