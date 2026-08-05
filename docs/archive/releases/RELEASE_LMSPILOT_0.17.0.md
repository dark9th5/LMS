# LMSPilot 0.17.0 — Accessible Learning Workspace

## Nội dung phát hành

- Viết lại shell, dashboard và login theo một design system hiện đại, sinh động nhưng dễ đọc.
- Xóa component, stylesheet, asset và nội dung Cosmic/Fantasy/Theme Studio cũ khỏi runtime.
- Chỉ còn hai chế độ `unified-light` và `unified-dark`; màu chủ đạo được tùy chỉnh trong giới hạn an toàn.
- Sửa toàn bộ trạng thái input/login: chữ, nền, placeholder, caret, focus, lỗi và browser autofill.
- Chuẩn hóa font hệ thống, body 16 px, bài học 18 px, focus-visible và vùng bấm chính tối thiểu 44 px.
- Đưa nội dung bài học và câu hỏi thi về cột chính; mục lục và danh sách câu là cột phụ.
- Làm lại dashboard theo công việc: hành động chính, KPI, tiến độ học và truy cập nhanh; không còn hero quỹ đạo, chữ neon hoặc thuật ngữ kỹ thuật.
- Giảm sidebar admin về LMS Core; các trung tâm nâng cao không còn route trong giao diện mặc định.
- Thay ô nhập danh sách bằng `RepeatableField`, số lượng bằng `NumberStepper`, cấu hình dịch vụ bằng form có cấu trúc.
- Bổ sung stylesheet cho mọi class literal đang dùng trong TSX và regression test chống tái xuất hiện giao diện legacy.
- Thêm tài liệu nghiên cứu thiết kế, script render tái lập và 7 ảnh giao diện đại diện.

## Tương thích dữ liệu

Các `themeKey` cũ được ánh xạ tự động sang `unified-light` hoặc `unified-dark`. Backend của module nâng cao vẫn được giữ để tránh xóa bảng, migration hoặc dữ liệu của khách hàng; chúng chỉ bị loại khỏi Core UI.

## Xác minh

- Validator: PASS.
- Python test: 115 đạt, 34 test lịch sử skip có chủ ý (149 test tổng cộng).
- TypeScript/TSX syntax: 62 file, 0 lỗi.
- CSS parse: 0 lỗi.
- Render preview: 7 ảnh đạt.
- Semantic TypeScript, Next build, Gradle và Docker E2E chưa được xác nhận do dependency/network không khả dụng trong môi trường đóng gói; bắt buộc chạy lại trước production.
