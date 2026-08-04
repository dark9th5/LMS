# Trạng thái triển khai LMSPilot CLS 0.9.0

Đây là source tree đầy đủ; các thay đổi nằm trực tiếp trong frontend, backend, migrations, scripts, tests và docs.

## Đã triển khai trong source

- Account model `SYSTEM_ADMIN`/`USER`, protected bootstrap admin, no public registration.
- Default/custom roles, multi-role, scoped `ALLOW/DENY`, bulk account/grant/revoke.
- Password policy, forced password change, lockout và session management.
- Cơ cấu tổ chức nhiều cấp và membership nhiều đơn vị.
- CSV/XLSX import với mapping/preview/row errors/upsert/atomic-partial/idempotency.
- License offline: signature, binding, capacity, features, grace và read-only.
- LDAP bind tùy chọn với local admin fallback.
- Khóa học/lớp/ghi danh/live session, immutable course version và pinned progress.
- Learning path nhiều chặng, sequential unlock, assignment theo user/đơn vị và auto-enroll.
- Learning progress, xAPI LRS, course discussions và competency framework.
- Assignment submission, scoped grading queue, return-for-revision và server-verified completion.
- Quiz/course test, standalone exam, competition, leaderboard/reward; exam gắn exact enrollment.
- Exam autosave/resume/heartbeat/grace, shuffle, objective/manual grading, score strategy, grade history và appeal.
- AI local/API, JSON Schema, provenance, review/import.
- File version, DOCX edit session/callback và PDF revision/annotation.
- Dynamic branding, news sanitizer/attachment ACL và optional external-service/Redis profiles.
- KPI/reporting read model, scheduled export.
- Notification template, due reminder scheduler, in-app/email outbox retry/lease/dead state.
- Certificate templates, issue/revoke/reissue/verification.
- Audit, operation schedules, backup/restore và allowlisted host agent.
- Astral Academy V3 có permission-first navigation, Command Atlas, responsive đa dải, high-contrast và reduced motion.

## Đã kiểm tra trong môi trường tạo bản

- Repository validator: 13 JSON, 28 YAML, 19 service, 18 Flyway service.
- 104/104 static/contract/UI regression tests.
- Semantic TypeScript, clean npm ci, Next production build, shell syntax, route/migration/internal-token/secret checks.
- npm audit: 0 vulnerability.
- Operations-agent allowlist tests.
- Archive cuối được giải nén và chạy lại validator/test.

## Chưa thể xác nhận tại đây

- Full Gradle compile/test do không tải được Gradle distribution/dependency.
- Docker Compose E2E vì không có Docker Engine.
- Browser E2E, load test, pentest, restore/update drill và UAT khách hàng.

Chi tiết: `../UI_UX_REDESIGN_0.9.0.md`, `../BUILD_VERIFICATION_0.9.0.md`, `../AUDIT_0.8.2.md`, `../BA_CLS_TRACEABILITY_0.8.2.md` và root `DELIVERY_STATUS.md`.
