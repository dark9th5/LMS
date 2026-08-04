# LMSPilot CLS 0.10.0 — Spectrum OS release candidate

0.10.0 xóa lớp Astral Academy V3 của 0.9.0 và xây dựng lại toàn bộ trải nghiệm hiển thị thành Spectrum OS. Đây là bản đổi kiến trúc giao diện, không phải đổi màu hay chỉnh nhẹ CSS.

Điểm chính:

- Shell, navigation, topbar và command palette mới, permission-aware và hỗ trợ bàn phím.
- Login, đổi mật khẩu, dashboard, khóa học, lớp, học tập và kỳ thi được viết lại bằng cấu trúc mới.
- Hệ chromatic/editorial/bento mới phủ các detail, player, form, table, modal và admin workspace.
- API, scoped RBAC và state thật được giữ; production guard xác nhận không có mock/fallback trong source.
- Responsive sáu dải, focus-visible, high contrast và reduced motion.
- 11 ảnh production render trên Chromium, không ghi nhận page/console/server error trong lượt chụp.
- 104/104 test, TypeScript, Next.js production build, validator và npm audit đều đạt.

Đây vẫn là **full-source release candidate**, chưa phải chứng nhận production. Xem `docs/UI_UX_REBUILD_0.10.0.md`, `docs/BUILD_VERIFICATION_0.10.0.md` và `DELIVERY_STATUS.md`.
