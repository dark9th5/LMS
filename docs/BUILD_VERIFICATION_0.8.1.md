# Build verification — LMSPilot CLS 0.8.1

Ngày kiểm tra: **03/08/2026**.

## Môi trường audit

- OpenJDK 17.0.19; project yêu cầu Java toolchain 21.
- Node.js 24.14.0; npm 11.9.0; Python 3.12.13.
- Không có Docker CLI/Engine, Gradle system installation hoặc Gradle distribution cache.

## Đã chạy thành công

```bash
python3 scripts/validate-repository.py
bash scripts/test-static.sh
cd apps/web
npm ci --no-audit --no-fund
npm run typecheck
npm run build
npm audit
```

Kết quả:

- Validator: 11 JSON, 28 YAML, 19 backend service, 18 service có Flyway — PASS.
- Static/contract regression: **80/80 PASS**.
- Semantic TypeScript: **65 source files PASS**.
- Next.js 16.2.12 optimized production build: **PASS**.
- npm dependency audit: **0 vulnerability**.
- Shell syntax: **PASS**.

## Chưa thể chạy

### Backend Gradle

```bash
cd backend
./gradlew test --no-daemon
```

Gradle Wrapper cần tải Gradle 8.14.5 từ `services.gradle.org`, nhưng network của môi trường audit không truy cập được. Host cũng chỉ có Java 17 trong khi build khai báo Java 21. Vì lệnh dừng trước compile, tài liệu này không suy diễn rằng backend đã build.

### Docker và tích hợp

Không có Docker Engine nên chưa chạy được Compose, Flyway trên PostgreSQL thật, RabbitMQ event, Redis profile, OnlyOffice/local AI, container health, smoke test hoặc browser E2E.

## Lệnh bắt buộc trên máy nghiệm thu

```bash
cd backend && ./gradlew clean test --no-daemon
cd ../apps/web && npm ci --no-audit --no-fund && npm run typecheck && npm run build && npm audit
cd ../.. && docker compose config --quiet
./scripts/setup.sh
```

Sau đó chạy browser E2E, load test kỳ thi, backup/restore drill, RBAC/UAT và pentest. Chỉ chấp nhận runtime khi tất cả service healthy và xuất hiện `SMOKE TEST PASSED`.
