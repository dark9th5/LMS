# BẮT ĐẦU TẠI ĐÂY — LMSPilot CLS 0.16.0

## 1. Giải nén an toàn

Giải nén `LMS-CLS-complete-0.16.0.zip` vào một thư mục mới. Không ghi đè trực tiếp hệ thống đang chạy trước khi tạo branch/tag và backup dữ liệu.

Khi thay source trong repository Git hiện có, giữ nguyên thư mục `.git`, tạo branch riêng và sao chép nội dung bản 0.16.0 vào working tree.

## 2. Chuẩn bị môi trường

- Java 21
- Node.js 22–24 và npm 10+
- Docker/Docker Compose khi chạy full stack
- Python 3.13 và PyYAML/pytest cho kiểm tra repository

## 3. Kiểm tra source

```bash
python scripts/validate-repository.py
pytest -q
cd apps/web
npm ci
npm run typecheck
npm run build
cd ../../backend
./gradlew test
```

## 4. Chạy hệ thống

Linux:

```bash
chmod +x scripts/*.sh
./scripts/setup.sh
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup.ps1
```

Chỉ coi setup thành công khi smoke test báo `SMOKE TEST PASSED`.

## 5. Kiểm tra giao diện 0.16

Sau khi đăng nhập bằng System Admin:

1. Mở `Cài đặt` và chọn giao diện sáng hoặc tối.
2. Chọn màu chủ đạo và màu nền; kiểm tra bản xem trước tự chọn màu chữ.
3. Kiểm tra login ở default, focus, filled, error và browser autofill.
4. Mở một bài học; nội dung phải nằm ở cột lớn, mục lục ở cột phụ.
5. Mở một bài thi; câu hỏi phải nằm ở cột lớn, danh sách câu ở cột phụ.
6. Kiểm tra sidebar admin chỉ còn các khu vực Core.
7. Kiểm tra form câu hỏi có từng dòng phương án và nút `+ / −`.

Đọc `docs/UNIFIED_UI_0.16.0.md`, `TEST_RESULTS_CLS_0.16.0.md` và `DELIVERY_STATUS.md` trước khi merge vào nhánh chính.
