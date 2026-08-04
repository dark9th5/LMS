# Test results — LMSPilot CLS 0.15.0

Ngày kiểm tra: 2026-08-04.

## Đã đạt

| Kiểm tra | Kết quả |
|---|---|
| Python static/contract/UI regression | `127 passed, 2 subtests passed` |
| Permission catalog coverage | 93/93 permission có metadata |
| Backend role-name gate scan | Không còn `CurrentUser.roles()` trong business services |
| Platform contracts Kotlin compile | PASS bằng `kotlinc` |
| TypeScript/TSX syntax parse | 62 tệp, 0 lỗi cú pháp |
| Legacy instructor/learner query scan | Không còn `role=INSTRUCTOR` / `role=LEARNER` ở FE |
| Archive ZIP độc lập | Safe paths; checksum, 127 test, validator và shell syntax đều đạt sau giải nén |

## Chưa xác minh trong môi trường này

| Kiểm tra | Lý do |
|---|---|
| `npm ci`, semantic typecheck, Next production build | Registry nội bộ trả 404 cho `undici-types-7.16.0.tgz` |
| Gradle multi-module backend build | Gradle wrapper distribution không tải được |
| Flyway/PostgreSQL upgrade smoke | Không có stack Docker đang chạy |
| Browser E2E gắn backend thật | Phụ thuộc build/runtime ở trên |
| Load/security/UAT | Cần môi trường đích và dữ liệu kiểm thử |

Các kết quả build/render của 0.14 vẫn là bằng chứng lịch sử cho nền giao diện, không được diễn giải thành xác nhận build cho thay đổi 0.15.

## Lệnh cần chạy lại trên máy đích

```bash
cd apps/web
npm ci
npm run typecheck
npm run build

cd ../../backend
./gradlew clean test

cd ..
docker compose up -d --build
./scripts/smoke-test.sh
```
