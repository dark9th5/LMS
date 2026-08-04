# Test results — LMSPilot CLS 0.8.1

## PASS

- Repository validator: PASS.
- Static/contract regression: **80/80 PASS**.
- Semantic TypeScript typecheck: **65 files PASS**.
- Clean `npm ci`: PASS.
- Next.js optimized production build: PASS.
- `npm audit`: **0 vulnerability**.
- Shell syntax: PASS.

## Chưa xác minh

- Backend Gradle compile/test: network không tải được wrapper distribution; host chỉ có Java 17 thay vì Java 21.
- Docker/database/broker integration và smoke test: Docker Engine không có.
- Browser E2E, load, pentest, restore drill và UAT.

Chi tiết: `docs/BUILD_VERIFICATION_0.8.1.md` và `docs/AUDIT_0.8.1.md`.
