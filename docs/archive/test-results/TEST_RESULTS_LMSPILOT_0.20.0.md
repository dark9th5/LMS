# Kết quả kiểm tra LMSPilot 0.20.0

## Đã chạy trong môi trường đóng gói

- `python scripts/validate-repository.py`: PASS.
- `python scripts/validate-service-ports.py`: PASS, 19 service dùng port riêng 8080–8098.
- `node scripts/check-typescript-syntax.js`: PASS, toàn bộ tệp TS/TSX được transpile kiểm tra cú pháp.
- `kotlinc` biên dịch `Permissions.kt` và `AccessProfiles.kt`: PASS, xác minh catalog quyền và ba access profile không lỗi cú pháp/type ở mức platform contracts.
- `python -m unittest discover -s tests -p 'test_*.py'`: PASS, **173 test**, trong đó **139 test đã thực thi đạt** và **34 test phụ thuộc môi trường được skip có chủ ý**. Các regression suite cũ đã được cập nhật theo kiến trúc 0.20; suite mới kiểm tra ba role, route riêng, tách permission, course-only, PDF/DOCX/video, assignment, AI generation, login background, dark border và service ports.
- `git diff --check`: PASS.

## Chưa thể chạy trong môi trường đóng gói

- `npm ci`, semantic TypeScript và Next.js production build: registry phụ thuộc trả HTTP 404 cho tarball `undici-types-7.16.0.tgz`; vì `npm ci` không hoàn tất nên không tuyên bố typecheck/build production đã đạt.
- Full Gradle test: Gradle wrapper không tải được distribution từ `services.gradle.org` do `UnknownHostException`; môi trường không có distribution/dependency tương ứng trong cache.
- Docker/API/browser E2E, migration thực, AI provider thật và UAT ba role.

Các bước chưa chạy ở trên là điều kiện bắt buộc trước production; kết quả static không thay thế runtime/E2E.
