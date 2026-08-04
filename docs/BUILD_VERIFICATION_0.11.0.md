# Build verification — LMSPilot CLS 0.11.0

Ngày kiểm tra: **04/08/2026**.

## Môi trường

- OpenJDK 17.0.19; backend khai báo Java toolchain 21.
- Node.js 24.14.0; npm 11.9.0; Python 3.12.13.
- Không có Docker CLI/Engine hoặc Gradle distribution cache.
- Chromium headless nằm trong vùng QA tạm; sản phẩm không thêm browser dependency.

## Gate đã chạy

```bash
python3 -m unittest discover -s tests -v
python3 scripts/validate-repository.py
node scripts/check-production-real-mode.mjs
bash -n scripts/*.sh
python3 -m py_compile scripts/operations-agent.py
cd apps/web
npm ci --cache /tmp/lms-cosmic-npm-cache-clean --no-audit --no-fund
npm run typecheck
npm run build
npm audit --omit=dev --audit-level=moderate
```

Kết quả:

- Validator: 17 JSON, 28 YAML, 19 backend services, 18 services có Flyway — **PASS**.
- Static/contract/flow/UI regression: **104/104 PASS**.
- Cosmic Research UI regression riêng: **8/8 PASS**.
- Semantic TypeScript: **PASS**.
- Next.js 16.2.12 optimized production build: **PASS**.
- Production real-mode guard, repository validator và syntax gates: **PASS**.
- Clean npm install và production dependency audit: **PASS**, 0 vulnerability.
- Chromium production render: **11 ảnh**, 0 page/console/server error trong lượt chụp.
- Archive giải nén: **PASS** — safe listing, 443 source checksums, validator, 104 test, real-mode guard, clean npm install, typecheck, build và audit được chạy lại.

## Browser visual QA

Build production được mở bằng Chromium ở 1440 × 1000/1100 và 390 × 844. Runner đi qua login, dashboard, courses, classes, exams và learning; chụp cả viewport lẫn full page ở các màn hình trọng yếu.

Dữ liệu lấp đầy màn hình là fixture intercept cô lập trong runner QA, không nằm trong bundle và không thay API production. Ảnh/report nằm trong `screenshots/0.11.0/`.

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

Sau `SMOKE TEST PASSED`, chạy Chrome/Firefox/WebKit, accessibility audit, upgrade migration từ bản đang vận hành, concurrent exam/load, RBAC/UAT, backup/restore và rollback drill.
