# LMSPilot 0.20.0 — Three-role Course-only LMS

## Phạm vi phát hành

- Chuyển sang đúng ba role cố định: `ADMIN`, `INSTRUCTOR`, `STUDENT`.
- Mỗi tài khoản chỉ được sở hữu một role; không cho role tùy chỉnh hoặc cấp chức năng chéo.
- Tách ba portal và sidebar riêng; route sai role tự chuyển về cổng của tài khoản đang đăng nhập.
- Loại bỏ giao diện, route và public API lớp học. Khóa học là không gian đào tạo duy nhất; gán học viên trực tiếp bằng course assignment.
- Đặt bài kiểm tra ngay trong khóa học; bài thi chỉ còn kỳ thi độc lập.
- Bổ sung sinh câu hỏi từ PDF/DOCX cho cả bài kiểm tra khóa học và kỳ thi độc lập.
- Hoàn thiện xem video, PDF, DOCX, tải tài liệu và nộp/chấm bài thực hành trực tiếp trong khóa học.
- Cho admin tải ảnh nền đăng nhập cùng logo, tên thương hiệu, giới thiệu và màu chủ đạo; backend kiểm tra chủ sở hữu, purpose và MIME của tệp branding trước khi công bố.
- Giữ style 0.18 dễ đọc; dark mode dùng viền xanh-xám tối thay vì viền trắng.
- Bổ sung danh mục 19 service và validator port duy nhất.
- Bổ sung bộ 18 ảnh QA tĩnh cho đăng nhập, ba portal, khóa học, bài kiểm tra, bài thi, chấm điểm và dark mode.

## Lưu ý tương thích

Một số entity/database nội bộ của cơ chế phân phối khóa học cũ được giữ để migration và dữ liệu nâng cấp không bị phá hủy. Chúng không còn route hoặc giao diện công khai và không được cấp permission cho ba role mới.

## Điều kiện production

Gói này là release candidate. Cần chạy full npm/Next, Gradle, fresh/upgrade migration, Docker smoke, browser E2E, RBAC/UAT ba role và kiểm thử AI provider thật trên hạ tầng đích.
