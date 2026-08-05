# Unified UI — LMSPilot 0.17.0

## Mục tiêu

Bản 0.17 loại bỏ mô hình 10 theme thay đổi cả font, hình học, mật độ và màu sắc. Runtime chỉ còn một design system với hai chế độ sáng/tối. Mọi component phải dùng cùng typography, spacing, control states và responsive behavior.

## Token chính

- `--ui-bg`, `--ui-surface`, `--ui-surface-alt`
- `--ui-text`, `--ui-muted`
- `--ui-primary`, `--ui-on-primary`
- `--ui-border`, `--ui-border-strong`, `--ui-focus`
- `--ui-success`, `--ui-warning`, `--ui-danger`, `--ui-info`

Màu thương hiệu chỉ thay `primary` và nền trong giới hạn an toàn. `readableText()` tự chọn chữ trắng hoặc đen cho màu chủ đạo/nền.

## Typography

- Font: system-ui, Segoe UI, Roboto, Helvetica, Arial.
- Nội dung thường: 16 px.
- Nội dung bài học: 18 px (`1.125rem`), line-height 1.75, tối đa 78 ký tự mỗi dòng.
- Label: 14 px.
- Caption: 13 px.
- Không dùng `Arial Black`, monospace hoặc font trang trí cho nội dung nghiệp vụ.

## Form và đăng nhập

Tất cả input/select/textarea khai báo riêng màu chữ, nền, placeholder, caret, border, hover, focus, disabled và Chrome autofill. Màn login bỏ thông tin kỹ thuật không phục vụ đăng nhập; giữ logo, tên hệ thống, tài khoản, mật khẩu, hiện/ẩn mật khẩu và lỗi rõ ràng.

## Workspace học và thi

Desktop dùng cột chính linh hoạt; Learning dùng `minmax(0, 1fr) 310px`, Exam dùng `minmax(0, 1fr) 260px`:

- Nội dung bài học/câu hỏi: cột 1.
- Mục lục/danh sách câu: cột 2.
- Nội dung chính có chiều cao gần viewport.
- Dưới 820 px chuyển một cột; phần phụ không được làm nội dung chính bị thu nhỏ.

Màn nộp bài thi dùng dialog trong ứng dụng, hiển thị số câu đã trả lời, câu trống và thời gian còn lại; không dùng `window.confirm` cho hành động nộp cuối.

## Admin Core

Sidebar giữ ba nhóm cấp cao:

- Học tập
- Thi & đánh giá
- Quản trị

Các module nâng cao không còn route Core. Việc giữ backend là quyết định tương thích dữ liệu, không đồng nghĩa chúng còn là tính năng mặc định của giao diện.

## Component nhập liệu mới

- `RepeatableField`: một giá trị mỗi dòng, thêm/xóa bằng `+ / −`.
- `NumberStepper`: giảm, nhập trực tiếp, tăng.
- Dịch vụ ngoài: form theo loại dịch vụ; JSON thô không xuất hiện trong luồng chuẩn.

## Kiểm thử bắt buộc

- Light/dark; primary sáng và tối.
- Login default/hover/focus/filled/error/autofill.
- Viewport 320, 375, 768, 1024, 1440 và 1920 px.
- Zoom 200%.
- Keyboard only và focus-visible.
- Bài học video, văn bản, PDF/DOCX; bài thi nhiều loại câu hỏi.
- Tên khóa học/người dùng dài, empty/loading/error states.
