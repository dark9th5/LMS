# Build verification — LMSPilot CLS 0.10.0

Ngày kiểm tra: **04/08/2026**.

## Môi trường

- OpenJDK 17.0.19; backend khai báo Java toolchain 21.
- Node.js 24.14.0; npm 11.9.0; Python 3.12.13.
- Không có Docker CLI/Engine hoặc Gradle distribution cache.
- Chromium headless được chuẩn bị trong thư mục QA tạm, không thêm dependency trình duyệt vào sản phẩm.

## Đã chạy thành công

```bash
python3 -m unittest discover -s tests -v
python3 scripts/validate-repository.py
node scripts/check-production-real-mode.mjs
bash -n scripts/*.sh
python3 -m py_compile scripts/operations-agent.py
cd apps/web
npm ci --no-audit --no-fund
npm run typecheck
npm run build
npm audit --omit=dev --audit-level=moderate
```

Kết quả:

- Validator: 15 JSON, 28 YAML, 19 backend service, 18 service có Flyway — **PASS**.
- Static/contract/flow/UI regression: **104/104 PASS**.
- Spectrum OS UI regression riêng: **8/8 PASS**.
- Semantic TypeScript: **PASS**.
- Next.js 16.2.12 optimized production build: **PASS**.
- Production real-mode guard: **PASS**.
- Shell và Operations Agent syntax: **PASS**.
- npm production dependency audit: **0 vulnerability**.
- Archive giải nén: **PASS** — safe listing, 425 checksum, validator, 104 tests, real-mode guard, clean npm install, typecheck, build và audit được chạy lại.

## Browser visual QA đã chạy

Build production được mở bằng Chromium ở 1440 × 1000/1100 và 390 × 844. Đã chụp 11 ảnh: login, dashboard, courses, classes, exams, learning và full-page mobile. Kết quả của lượt chụp:

- JavaScript/page error: **0**.
- Console error: **0**.
- Server error trong tiến trình chụp: **0**.
- Horizontal overflow quan sát được ở mobile: **không**.

Dữ liệu lấp đầy màn hình là fixture intercept cô lập trong runner QA, không nằm trong bundle và không thay API production. Ảnh và report nằm trong `docs/screenshots/0.10.0/`.

## Không thể chạy trong môi trường này

- Backend Gradle compile/test: host thiếu Java 21 và Gradle distribution không có sẵn.
- Docker Compose, fresh/upgrade Flyway trên PostgreSQL, RabbitMQ events, health/smoke và restore drill: không có Docker Engine.
- Firefox/WebKit matrix, Lighthouse/WCAG audit chính thức, load/chaos, pentest và UAT: chưa chạy.

Không được diễn giải các giới hạn trên thành backend build thành công hoặc chứng nhận production.

## Gate bắt buộc trên máy nghiệm thu

```bash
cd backend && ./gradlew clean test --no-daemon
cd ../apps/web && npm ci --no-audit --no-fund && npm run typecheck && npm run build && npm audit
cd ../.. && docker compose config --quiet
./scripts/setup.sh
```

Sau `SMOKE TEST PASSED`, chạy Chrome/Firefox/WebKit, accessibility audit, upgrade migration từ bản đang vận hành, concurrent exam/load, RBAC/UAT, backup/restore và rollback drill.
