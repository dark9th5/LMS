# Build verification — LMSPilot 0.8.2

Ngày kiểm tra: **03/08/2026**.

## Môi trường

- OpenJDK 17.0.19; backend khai báo Java toolchain 21.
- Node.js 24.14.0; npm 11.9.0; Python 3.12.13.
- Không có Docker CLI/Engine, Gradle cài hệ thống hoặc Gradle distribution cache.

## Đã chạy thành công

```bash
python3 -m unittest discover -s tests -v
python3 scripts/validate-repository.py
node scripts/check-production-real-mode.mjs
node scripts/check-typescript.js
cd apps/web
npm run typecheck
npm run build
npm audit --omit=dev --audit-level=moderate
```

Kết quả:

- Validator: 12 JSON, 28 YAML, 19 backend service, 18 service có Flyway — PASS.
- Static/contract/flow regression: **96/96 PASS**.
- Semantic TypeScript: **65 source files PASS**.
- Next.js 16.2.12 optimized production build: **PASS**.
- Production real-mode guard: **PASS**.
- npm production dependency audit: **0 vulnerability**.

## Không thể chạy trong môi trường này

```bash
GRADLE_USER_HOME=/tmp/lmspilot-gradle ./backend/gradlew -p backend --version
```

Wrapper cố tải Gradle 8.14.5 từ `services.gradle.org` và dừng với `Network is unreachable`. Host chỉ có Java 17; lệnh dừng trước compile. Không được suy diễn rằng backend đã build thành công.

`docker version` trả về `command not found`, nên chưa chạy Compose, Flyway trên PostgreSQL, RabbitMQ events, health/smoke, browser E2E, load hoặc restore drill.

## Gate bắt buộc trên máy nghiệm thu

```bash
cd backend && ./gradlew clean test --no-daemon
cd ../apps/web && npm ci --no-audit --no-fund && npm run typecheck && npm run build && npm audit
cd ../.. && docker compose config --quiet
./scripts/setup.sh
```

Sau đó chạy upgrade migration từ 0.8.1, browser E2E, concurrent exam/load, RBAC/UAT, backup/restore và rollback drill. Runtime chỉ đạt khi mọi service healthy và `SMOKE TEST PASSED`.
