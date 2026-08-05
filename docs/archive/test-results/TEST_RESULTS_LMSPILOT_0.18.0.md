# Test results — LMSPilot 0.18.0

Ngày kiểm tra: 2026-08-05.

## Đã chạy

- `python -m unittest discover -s tests -p "test_*.py"`: **122 test đạt, 34 test skip (156 test tổng cộng)**.
- `python scripts/validate-repository.py`: **đạt**, kiểm tra 27 JSON, 28 YAML, 19 service, 18 Flyway service, API wiring, wrapper, script và Docker layout.
- TypeScript syntax parse bằng compiler API: **đạt** cho toàn bộ 64 tệp TS/TSX.
- Targeted TypeScript semantic check với framework stubs cục bộ: **đạt**.
- `kotlinc Permissions.kt AccessProfiles.kt`: **đạt**.
- `git diff --check`: **đạt**.

## Chưa chạy đầy đủ trong môi trường đóng gói

- `npm ci`, `npm run typecheck`, `npm run build`: registry nội bộ thiếu một số tarball (`undici-types`, `typescript`, `tslib`, `styled-jsx`).
- Gradle test: wrapper cần tải Gradle 8.14.5 nhưng môi trường đóng gói không có kết nối tới `services.gradle.org`.
- Docker smoke test và browser E2E: chưa chạy vì phụ thuộc các bước build trên.

Các bước này phải được chạy lại trên GitHub Actions hoặc máy phát triển có registry/Gradle cache trước khi triển khai production.
