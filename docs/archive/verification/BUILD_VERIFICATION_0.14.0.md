# Build verification — LMSPilot 0.14.0

Ngày kiểm tra: **04/08/2026**.

## Môi trường

- OpenJDK 17.0.19; backend khai báo Java toolchain 21.
- Node.js 24.14.0; npm 11.9.0; Python 3.12.13.
- Không có Docker CLI/Engine hoặc Gradle distribution cache.
- Chromium headless chỉ dùng trong QA tạm; sản phẩm không thêm browser dependency.

## Gate đã chạy

```bash
python3 -m unittest discover -s tests -p 'test_*.py'
python3 scripts/validate-repository.py
node scripts/check-production-real-mode.mjs
bash -n scripts/*.sh
python3 -m py_compile scripts/operations-agent.py
cd apps/web
npm run typecheck
npm run build
npm audit --omit=dev --audit-level=moderate
```

Kết quả:

- Repository validator: **PASS** — 23 JSON, 28 YAML, 19 backend services, 18 services có Flyway.
- Static/contract/flow/UI regression: **121/121 PASS**.
- Soft Spectrum 0.14 regression riêng: **5/5 PASS**.
- Semantic TypeScript: **PASS**.
- Next.js 16.2.12 optimized production build: **PASS**.
- Production real-mode guard, repository validator và syntax gates: **PASS**.
- Production dependency audit: **PASS**, 0 vulnerability.
- Chromium production render: **23 capture + 1 contact sheet**, 0 page/console/server error.
- Archive giải nén: **PASS** — safe listing, 544 source checksums, validator, 121 test, real-mode guard, typecheck, production build và audit được chạy lại.

## Browser visual QA

Build production được mở bằng Chromium ở 1440 × 1000/1100 và 390 × 844. Runner đi qua login, dashboard, courses, classes, exams, learning, Theme Studio và 10 dashboard theme. Ảnh/report nằm trong `screenshots/0.14.0/`; `visual-qa.json` ghi `pageErrors: []` và `serverErrors: ""`.

## Không thể chạy trong môi trường này

- Backend Gradle compile/test: host thiếu Java 21 và Gradle distribution.
- Docker Compose, fresh/upgrade Flyway trên PostgreSQL, RabbitMQ events, health/smoke và restore drill: không có Docker Engine.
- Firefox/WebKit, Lighthouse/WCAG audit chính thức, load/chaos, pentest và UAT: chưa chạy.

Không được diễn giải các giới hạn trên thành backend build thành công hoặc chứng nhận production.

## Gate bắt buộc trên máy nghiệm thu

```bash
cd backend && ./gradlew clean test --no-daemon
cd ../apps/web && npm ci --no-audit --no-fund && npm run typecheck && npm run build && npm audit
cd ../.. && docker compose config --quiet
./scripts/setup.sh
```

Sau `SMOKE TEST PASSED`, chạy Chrome/Firefox/WebKit, accessibility audit, fresh/upgrade migration có `V6__soft_spectrum_default.sql`, concurrent exam/load, RBAC/UAT, backup/restore và rollback drill.
