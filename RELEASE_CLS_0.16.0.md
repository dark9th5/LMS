# LMSPilot CLS 0.16.0 — Unified UI and Core Admin

## Nội dung phát hành

- Thay 10 theme bằng một design system với chế độ sáng/tối.
- Loại bỏ bốn stylesheet theme legacy khỏi runtime.
- Sửa xung đột màu input/login, focus và browser autofill.
- Dùng font hệ thống, tăng cỡ chữ và vùng tương tác.
- Đưa nội dung bài học/câu hỏi thi về cột chính.
- Thay xác nhận nộp bài thi bằng dialog có thống kê.
- Giảm sidebar về các chức năng LMS Core.
- Route các trung tâm nâng cao trả 404 trong giao diện Core.
- Thay input phân tách bằng dấu phẩy/dòng bằng `RepeatableField` ở các luồng trọng yếu.
- Thay trường số bằng `NumberStepper` ở câu hỏi và cấu hình bài thi.
- Thay JSON cấu hình dịch vụ bằng form có cấu trúc.
- Màu chữ thương hiệu được tính tự động để tránh chữ chìm vào nền.

## Tương thích

Các `themeKey` cũ được ánh xạ tự động: theme tối cũ sang `unified-dark`, các theme còn lại sang `unified-light`. Backend của module nâng cao chưa bị xóa nhằm bảo toàn dữ liệu và tránh migration phá hủy; chúng không còn xuất hiện trong giao diện Core.

## Xác minh

111 test đạt, 25 test lịch sử bị skip có chủ ý, 2 subtest đạt; repository validator, CSS parse và TypeScript syntax đạt. Semantic TypeScript, Next build, full Gradle và Docker E2E cần chạy lại ở môi trường có dependency đầy đủ.
