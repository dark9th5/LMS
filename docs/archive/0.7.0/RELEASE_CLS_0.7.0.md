# LMSPilot CLS 0.7.0 — Full-source release candidate

Bản 0.7.0 hợp nhất repository LMSPilot gốc, tài liệu BA/URD và toàn bộ yêu cầu bổ sung CLS trong cuộc trò chuyện.

## Điểm chính

- Flexible scoped RBAC, protected system admin, no public registration.
- CSV/XLSX user import with preview, per-row errors and atomic/partial policies.
- Immutable course publication versions pinned to classes and learning progress.
- Dedicated assignment submissions and grading workflow.
- Offline license enforcement, LDAP authentication, durable email delivery.
- Standalone exams, competitions, leaderboard/rewards, AI question generation.
- Customer branding/news/optional integrations and dark-fantasy accessible UI.
- Safe operations job agent for backup/restore/maintenance.

## Verification

Static/contract suite: **50/50 passed** on the packaged source. Full Gradle/npm/Docker E2E remains required on the target host because dependency registries and Docker Engine were unavailable in the authoring environment.

Read `DELIVERY_STATUS.md` and `docs/BA_CLS_TRACEABILITY_0.7.0.md` before production deployment.
