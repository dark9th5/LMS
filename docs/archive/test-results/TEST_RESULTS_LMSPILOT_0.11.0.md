# Test results — LMSPilot 0.11.0

## PASS

- Repository validator: **PASS** — 17 JSON, 28 YAML, 19 services, 18 Flyway services.
- Static/contract/flow/UI regression: **104/104 PASS**.
- Cosmic Research UI regression: **8/8 PASS**.
- Semantic TypeScript typecheck: **PASS**.
- Next.js 16.2.12 optimized production build: **PASS**.
- Production real-mode guard: **PASS**.
- Shell/Python Operations Agent syntax: **PASS**.
- Clean `npm ci` với cache tạm cô lập: **PASS**.
- `npm audit --omit=dev`: **0 vulnerability**.
- Chromium production render: **11 screenshots PASS**, 0 page/console/server error trong lượt chụp.
- Archive extraction verification: **PASS** — safe listing, 443 source checksums, validator, 104 tests, real-mode guard, clean npm install, typecheck, build và audit được chạy lại.

## Chưa xác minh trong môi trường tạo bản

- Backend Gradle compile/test do host thiếu Java 21 và Gradle distribution.
- Docker/PostgreSQL/RabbitMQ integration và smoke do không có Docker Engine.
- Firefox/WebKit, Lighthouse/WCAG chính thức, load/chaos, pentest, restore/rollback và UAT.

Chi tiết: `docs/BUILD_VERIFICATION_0.11.0.md`, `docs/UI_UX_COSMIC_0.11.0.md` và `docs/AUDIT_0.8.2.md`.
