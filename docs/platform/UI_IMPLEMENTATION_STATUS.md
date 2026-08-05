# Trạng thái giao diện LMSPilot 0.15.0

## Cập nhật 0.15

- Console phân quyền mới dùng gói quyền theo công việc, metadata tiếng Việt, risk, preview, thời hạn, explain và revoke.
- Điều hướng và action gate dùng permission; danh sách người phụ trách lớp/người ghi danh không còn truy vấn role cứng.
- Syntax parse 62 tệp TS/TSX đạt; semantic typecheck/build phải chạy lại vì môi trường đóng gói không cài được dependency.

## Nền giao diện đã có từ 0.14

- Catalog 10 cá tính độc lập cho doanh nghiệp, trường học và tổ chức thay visual runtime Cosmic/fantasy của 0.12.
- Permission-first accordion rail chỉ có ba nhóm chính Học tập/Đánh giá/Quản trị; Soft Spectrum khóa rail vào navy–graphite–xám–trắng, không pha màu theo group.
- Dashboard, khóa học/lớp, learning player, exam/grading, user import và role-aware pages.
- Login, đổi mật khẩu, dashboard, course/class/learning/exam archive đều có cấu trúc và biểu đạt thị giác mới thay vì chỉ đổi skin.
- Luồng thi theo enrollment, tự làm mới kết quả; chấm exam/assignment theo tab và hàng đợi lớp.
- Tin tức có upload/download attachment và trạng thái tải/lỗi thật.
- Learning Path Center: tạo/sửa/xuất bản/giao lộ trình và xem tiến độ.
- KPI dashboard theo scope và khóa học.
- Notification Automation Center: template, preview, rule, manual run và trạng thái gửi.
- Certificate template, reporting, operation schedule, competency và các trung tâm quản trị nâng cao.
- Theme Studio có 10 preset, tìm kiếm/lọc mode/category, live preview toàn trang, hoàn tác và áp dụng bằng API thật.
- Theme được SSR từ public branding; 8 preset sáng và 2 preset tối phủ token, shell, hero, form, bảng, KPI và responsive surface.
- Soft Spectrum phủ pastel tiết chế lên hero, KPI, summary, course/class/exam/login trong khi giữ canvas và control trung tính.
- Flyway V5/V6 chuyển dữ liệu qua các catalog; default hiện hành là `soft-spectrum` và custom palette không bị V6 ghi đè.

## Bằng chứng lịch sử của nền giao diện 0.14

- Semantic TypeScript typecheck và Next.js production build đạt.
- Static/contract/UI suite đạt 121/121; npm audit 0 vulnerability.
- CSS có reduced-motion/responsive/accessibility guards.
- Không có HTTP asset, font hoặc image dependency trong lớp giao diện mới.
- Chromium production build đã chụp 23 ảnh desktop/mobile/theme; 0 page/console/server error trong lượt chụp.

## Chưa kiểm tra lại cho thay đổi 0.15

- Semantic TypeScript, Next production build, browser E2E gắn backend thật, Firefox/WebKit và audit WCAG chính thức.

Chi tiết quyền/UI 0.15: `../PERMISSION_FIRST_0.15.0.md`. Tài liệu render/build 0.14 được giữ như lịch sử tại `../SOFT_SPECTRUM_0.14.0.md` và `../BUILD_VERIFICATION_0.14.0.md`.
