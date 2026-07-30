# LMSPilot 0.4.1 — Real CRUD & Demo Course

Target baseline: `de4184f` (`0.4.0`).

## Chức năng đã hoàn thiện

- Login và Gateway fail-closed, không còn đăng nhập hoặc CRUD giả khi backend lỗi.
- Khóa học: tạo, xem, sửa, xuất bản, ẩn, khôi phục và xóa an toàn bằng lưu trữ.
- Bài học: thêm, xem, sửa, tải tài nguyên và xóa thật; hỗ trợ TEXT, VIDEO, PDF, DOCX, AUDIO, FILE, ASSIGNMENT và EXAM.
- Ngân hàng câu hỏi: tạo, xem, sửa và lưu trữ. Đề đã tạo giữ bản chụp để không bị thay đổi ngược lịch sử.
- Bài kiểm tra: tạo, xem, sửa cấu hình khi chưa có lượt làm và lưu trữ an toàn.
- Sau khi đề có lượt làm, cấu trúc đề bị khóa; tạo đề mới thay vì sửa lịch sử.

## Bài 0 mẫu

Khóa `LMS-000 — Bài 0 - Làm quen với LMSPilot` gồm:

1. Nội dung giới thiệu.
2. Video MP4 giới thiệu hành trình học.
3. PDF hướng dẫn nhanh cho học viên.
4. DOCX checklist dành cho giảng viên.
5. Điểm neo bài thực hành.
6. Điểm neo bài kiểm tra.

Đề `Bài 0 - Kiểm tra làm quen LMSPilot` có 5 câu khách quan, 10 điểm, thời lượng 10 phút và 3 lượt làm.

Seed v2 có lịch sử riêng và chỉ chạy một lần. Sau lần seed đầu tiên, người dùng có thể sửa, xóa hoặc lưu trữ mẫu mà dữ liệu không tự sinh lại khi service khởi động.

## Kiểm tra

```bash
node scripts/check-production-real-mode.mjs
bash scripts/test-static.sh
```

Sau khi toàn bộ stack đang chạy, dùng tài khoản Admin hoặc Giảng viên:

```bash
LMSPILOT_SMOKE_USERNAME=admin \
LMSPILOT_SMOKE_PASSWORD='your-password' \
node scripts/smoke-lms-crud.mjs
```

Script smoke tạo dữ liệu tạm, đọc lại để xác nhận persistence, sửa, xóa/lưu trữ và tự dọn dữ liệu.
