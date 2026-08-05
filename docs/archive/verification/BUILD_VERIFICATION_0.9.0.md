# Build verification — LMSPilot 0.9.0

Ngày kiểm tra: **04/08/2026**.

## Môi trường

- OpenJDK 17.0.19; backend khai báo Java toolchain 21.
- Node.js 24.14.0; npm 11.9.0; Python 3.12.13.
- Không có Docker CLI/Engine, browser runtime, Gradle cài hệ thống hoặc Gradle distribution cache.

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

- Validator: 13 JSON, 28 YAML, 19 backend service, 18 service có Flyway — **PASS**.
- Static/contract/flow/UI regression: **104/104 PASS**.
- UI redesign regression riêng: **8/8 PASS**.
- Semantic TypeScript: **65 source files PASS**.
- Next.js 16.2.12 optimized production build: **PASS**.
- Production real-mode guard: **PASS**.
- npm production dependency audit: **0 vulnerability**.
- Design-system guards: no external visual dependency, responsive, reduced-motion, contrast và permission-aware command navigation — **PASS**.

## Không thể chạy trong môi trường này

```bash
GRADLE_USER_HOME=/tmp/lmspilot-gradle ./backend/gradlew -p backend --version
```

Wrapper cần Gradle 8.14.5 từ `services.gradle.org`, nhưng endpoint này không khả dụng trong môi trường tạo bản. Host chỉ có Java 17; không được suy diễn rằng backend đã compile/test thành công.

`docker version` không khả dụng, nên chưa chạy Compose, Flyway trên PostgreSQL, RabbitMQ events, health/smoke, integration hoặc restore drill. Không có Chromium/Firefox/WebKit, nên chưa chạy browser E2E, screenshot matrix, visual regression hoặc Lighthouse.

## Gate bắt buộc trên máy nghiệm thu

```bash
cd backend && ./gradlew clean test --no-daemon
cd ../apps/web && npm ci --no-audit --no-fund && npm run typecheck && npm run build && npm audit
cd ../.. && docker compose config --quiet
./scripts/setup.sh
```

Sau khi `SMOKE TEST PASSED`, chạy Playwright trên Chrome/Firefox/WebKit ở desktop/mobile, accessibility audit, upgrade migration từ 0.8.2, concurrent exam/load, RBAC/UAT, backup/restore và rollback drill.
