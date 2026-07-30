# Áp dụng LMSPilot 0.4.1 lên commit de4184f

Bản này được xây dựng từ nội dung mã nguồn của commit:

`de4184f - feat: complete LMS platform release 0.4.0 with interactive detail modal, role routing, and full static test pass`

## Cách 1 — dùng toàn bộ source

Giải nén `LMSPilot_0.4.1_real_crud_demo_complete.zip`, sau đó cấu hình secret và khởi động như README của project.

## Cách 2 — áp dụng patch vào repository hiện có

Tại repository đang checkout đúng commit `de4184f`:

```bash
git status
git apply --check LMSPilot_0.4.1_real_crud_demo.patch
git apply --binary LMSPilot_0.4.1_real_crud_demo.patch
```

Sau đó kiểm tra:

```bash
node scripts/check-production-real-mode.mjs
bash scripts/test-static.sh
```

Khi toàn bộ stack đã chạy, dùng tài khoản Admin hoặc Giảng viên:

```bash
LMSPILOT_SMOKE_USERNAME=admin \
LMSPILOT_SMOKE_PASSWORD='your-password' \
node scripts/smoke-lms-crud.mjs
```

## Quy tắc xóa dữ liệu

- Khóa học: `DELETE` chuyển sang `ARCHIVED` để không phá lịch sử học tập.
- Bài kiểm tra: `DELETE` chuyển sang `ARCHIVED` để giữ lượt làm và điểm.
- Câu hỏi: `DELETE` chuyển sang `ARCHIVED`; bản chụp trong đề cũ vẫn được giữ.
- Bài học: xóa thật khỏi khóa học sau khi xác nhận.

## Khóa mẫu Bài 0

Seed một lần khóa `LMS-000 — Bài 0 - Làm quen với LMSPilot`, gồm nội dung chữ, video MP4, PDF, DOCX, bài thực hành và bài kiểm tra 5 câu. Sau lần seed đầu, người dùng có thể sửa/xóa/lưu trữ mà dữ liệu không tự sinh lại khi service khởi động.
