# Trạng thái giao diện CLS 0.9.0

## Đã có

- Astral Academy V3 là bản tái thiết kế toàn diện: spatial shell, nền thiên thể, hệ token, typography và surface hierarchy mới.
- Permission-first sidebar nổi, top bar theo ngữ cảnh, Command Atlas `Ctrl/Cmd + K`, keyboard focus, contrast và reduced motion.
- Dashboard, khóa học/lớp, learning player, exam/grading, user import và role-aware pages.
- Login, đổi mật khẩu, dashboard, course/class/learning/exam archive đều có cấu trúc và biểu đạt thị giác mới thay vì chỉ đổi skin.
- Luồng thi theo enrollment, tự làm mới kết quả; chấm exam/assignment theo tab và hàng đợi lớp.
- Tin tức có upload/download attachment và trạng thái tải/lỗi thật.
- Learning Path Center: tạo/sửa/xuất bản/giao lộ trình và xem tiến độ.
- KPI dashboard theo scope và khóa học.
- Notification Automation Center: template, preview, rule, manual run và trạng thái gửi.
- Certificate template, reporting, operation schedule, competency và các trung tâm quản trị nâng cao.

## Đã kiểm tra

- Semantic TypeScript typecheck và Next.js production build đạt.
- Static/contract/UI suite đạt 104/104; npm audit 0 vulnerability.
- CSS có reduced-motion/responsive/accessibility guards.
- Không có HTTP asset, font hoặc image dependency trong lớp giao diện mới.

## Chưa kiểm tra được

- Browser E2E và audit WCAG chính thức do không có runtime/Docker/browser stack.

Chi tiết: `../UI_UX_REDESIGN_0.9.0.md` và `../BUILD_VERIFICATION_0.9.0.md`.
