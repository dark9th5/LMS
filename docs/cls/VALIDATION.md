# Validation report — LMSPilot CLS 0.9.0

## Đã chạy thành công

```bash
bash scripts/test-static.sh
```

Kết quả:

- JSON hợp lệ: 13.
- YAML hợp lệ: 28.
- Backend services: 19.
- Flyway services: 18; không trùng version và không có migration phá bảng/cột bị cấm.
- Semantic TypeScript project typecheck: PASS (65 source files).
- Shell syntax: PASS.
- Repository/requirement/operations-agent/flow/UI tests: **104/104 PASS**.
- Next.js production build và `npm audit` 0 vulnerability: PASS.

## Giới hạn môi trường

- Gradle wrapper không tải được distribution từ `services.gradle.org`.
- Host chỉ có Java 17 trong khi backend yêu cầu Java 21.
- Docker Engine không có.

Không được diễn giải các giới hạn trên thành backend build thành công. Frontend đã semantic typecheck/build; vẫn cần Gradle build, Compose smoke, browser E2E và UAT trên máy đích. Xem `../BUILD_VERIFICATION_0.9.0.md`, `../UI_UX_REDESIGN_0.9.0.md` và `../AUDIT_0.8.2.md`.
