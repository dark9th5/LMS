# LMSPilot 0.20.3 — AI difficulty & review

Bản 0.20.3 giữ nguyên ba portal, mô hình course-only, style giao diện và kiến trúc 19 service của 0.20.2. Phạm vi thay đổi tập trung vào chất lượng câu hỏi AI từ PDF/DOCX.

## Hoàn thiện

- Preset độ khó Cơ bản, Cân bằng, Nâng cao và tùy chỉnh.
- Hiển thị số câu Dễ/Trung bình/Khó theo thời gian thực.
- Backend bắt buộc tỷ lệ 100%, đúng số câu và đúng phân bố.
- Kiểm tra câu/phương án trùng, lời giải, loại câu hỏi và citation nguyên văn.
- Một lần tự sửa kết quả AI dựa trên lỗi validator.
- Modal xem trước từng câu, đáp án đúng, lời giải, nguồn và trang/mục.
- Chỉ câu được giảng viên chọn mới được duyệt và nhập.
- Áp dụng đồng nhất cho bài kiểm tra trong khóa học và bài thi độc lập.

## Kiểm thử

- Python regression: 186 test, 152 đạt, 34 skip có chủ ý.
- Repository validator: PASS.
- TypeScript/TSX syntax: 89 file PASS.
- OpenAI-compatible local mock: 10 câu, phân bố 3/5/2, citation 10/10, phương án duy nhất 10/10, lời giải đầy đủ 10/10, không trùng stem.
- Kiểm thử âm phát hiện đúng phân bố sai và citation không có trong nguồn.

Full Gradle test chưa chạy được trong môi trường đóng gói vì Gradle distribution không thể tải từ `services.gradle.org`. Model AI thật chưa được gọi vì không có API key của khách hàng; cần UAT với provider/model được cấu hình trên hạ tầng đích.
