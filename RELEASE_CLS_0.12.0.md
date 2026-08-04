# LMSPilot CLS 0.12.0 — Theme Studio release candidate

0.12.0 mở rộng Cosmic Research UI thành hệ 10 giao diện có thể xem thử và áp dụng trực tiếp trong trang Admin. Theme không chỉ đổi màu: preset điều khiển typography, geometry, surface, shadow, atmosphere, navigation, hero, KPI, form, bảng và login trên cùng contract branding.

Điểm chính:

- 10 preset: Cosmic Observatory, Quantum Cyan, Lunar Silver, Aurora Research, Mars Expedition, Abyssal Ocean, Biosphere Lab, Solar Archive, Neo Academia và Mono Terminal.
- Theme Studio có tìm kiếm, lọc nhóm/dark-light, xem thử toàn trang, hoàn tác và sticky apply bar.
- `themeKey` được validate/lưu thật ở Configuration Service và database Flyway V4; không dùng localStorage.
- Root layout SSR từ public branding để tránh flash theme sai và áp dụng đồng nhất cho login/portal/admin.
- 8 theme tối, 2 theme sáng có xử lý tương phản riêng; CSS/SVG tự chứa, không phụ thuộc CDN/font ngoài.
- Chromium render 23 ảnh, gồm gallery và đủ 10 dashboard theme; không ghi nhận page/console/server error.
- 111/111 test, semantic TypeScript và Next.js production build đạt.

Đây vẫn là **full-source release candidate**, chưa phải chứng nhận production. Xem `docs/THEME_STUDIO_0.12.0.md`, `docs/BUILD_VERIFICATION_0.12.0.md` và `DELIVERY_STATUS.md`.
