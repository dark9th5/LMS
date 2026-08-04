# LMSPilot CLS 0.8.1 — Hardened full-source release candidate

0.8.1 là bản audit/hardening của 0.8.0, không phải chứng nhận production.

Điểm chính:

- Sửa contract type Learning Path và xác minh semantic TypeScript/Next production build.
- Nâng static checker để phát hiện lỗi type liên file; mở rộng regression suite lên 80 test.
- Khóa idempotency bulk theo DB, operation type và người thực hiện.
- Chặn cross-origin mutation ở cookie-backed BFF; thêm security headers và loại debug/IP cá nhân.
- Gia cố object/purpose access của File Storage và callback editor có giới hạn/không redirect.
- Đồng bộ Spring Boot/Cloud release train; chuyển Docker frontend sang `npm ci`.
- Khóa dependency đã vá; `npm audit` về 0 known vulnerability.

Xem `docs/AUDIT_0.8.1.md` và `docs/BUILD_VERIFICATION_0.8.1.md` để biết bằng chứng và blocker còn lại.
