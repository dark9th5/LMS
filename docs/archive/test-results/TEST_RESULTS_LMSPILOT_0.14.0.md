# Test results — LMSPilot 0.14.0

## PASS

- Repository validator: **PASS** — 23 JSON, 28 YAML, 19 services, 18 Flyway services.
- Static/contract/flow/UI regression: **121/121 PASS**.
- Soft Spectrum regression: **5/5 PASS**.
- Semantic TypeScript typecheck: **PASS**.
- Next.js 16.2.12 optimized production build: **PASS**.
- Production real-mode guard: **PASS**.
- Shell/Python Operations Agent syntax: **PASS**.
- `npm audit --omit=dev`: **0 vulnerability**.
- Chromium production render: **23 capture PASS** và một contact sheet, 0 page/console/server error.
- Archive extraction verification: **PASS** — safe listing, 544 source checksums, validator, 121 tests, real-mode guard, typecheck, build và audit được chạy lại.

## Chưa xác minh trong môi trường tạo bản

- Backend Gradle compile/test do host thiếu Java 21 và Gradle distribution.
- Docker/PostgreSQL/RabbitMQ integration và smoke do không có Docker Engine.
- Firefox/WebKit, Lighthouse/WCAG chính thức, load/chaos, pentest, restore/rollback và UAT.

Chi tiết: `docs/BUILD_VERIFICATION_0.14.0.md`, `docs/SOFT_SPECTRUM_0.14.0.md` và `docs/AUDIT_0.8.2.md`.
