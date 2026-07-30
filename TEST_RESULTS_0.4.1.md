# Test results — LMSPilot 0.4.1

Ngày kiểm tra: 2026-07-30

## Đã chạy và đạt

- `git diff --check`: không còn whitespace error.
- `node scripts/check-production-real-mode.mjs`: PASS.
- `bash scripts/test-static.sh`: PASS, 16 repository contract tests.
- TypeScript transpile syntax cho các route/component đã sửa: PASS.
- `node --check scripts/smoke-lms-crud.mjs`: PASS.
- MP4 mẫu: `ffprobe` đọc được, H.264/YUV420p, thời lượng 18 giây.
- PDF mẫu: 3 trang A4, render thành ảnh và kiểm tra trực quan, không tràn/cắt nội dung.
- DOCX mẫu: 3 trang, ZIP integrity PASS, render thành ảnh và kiểm tra trực quan.
- Kiểm tra tĩnh xác nhận có API DELETE/PUT cho khóa học, bài học, câu hỏi và bài kiểm tra; frontend đã nối tới các API này.

## Chưa thể chạy trong môi trường tạo bản sửa

- `npm ci` không hoàn tất vì registry nội bộ trả 404 cho `undici-types-7.16.0.tgz`; vì vậy chưa chạy được `next build` bằng dependency cài mới.
- Gradle Wrapper không tải được Gradle 8.14.5 do môi trường không phân giải được `services.gradle.org`; vì vậy chưa chạy compile/test Spring Boot thực tế.
- Docker không được cài trong môi trường, nên chưa chạy toàn bộ 18 service và E2E trực tiếp.

## Cách xác nhận cuối trên máy có mạng/Docker

```bash
cd apps/web
npm ci
npm run typecheck
npm run build

cd ../../
bash backend/gradlew -p backend test --no-daemon
docker compose config
docker compose build
docker compose up -d
```

Sau khi hệ thống sẵn sàng:

```bash
LMSPILOT_SMOKE_USERNAME=admin \
LMSPILOT_SMOKE_PASSWORD='your-password' \
node scripts/smoke-lms-crud.mjs
```

Smoke test sẽ xác nhận Bài 0 và tài nguyên mẫu, sau đó tạo–đọc–sửa–xóa dữ liệu tạm thật và tự dọn dữ liệu.
