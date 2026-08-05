# LMSPilot 0.13.0 — Diverse UI release candidate

0.13.0 thay catalog thiên về fantasy/không gian bằng mười cá tính giao diện dành cho nhiều loại người dùng và tổ chức. Bản này đồng thời rút sidebar dài thành accordion hai tầng mà vẫn giữ nguyên permission filtering, API, dữ liệu thật và luồng lưu branding.

Điểm chính:

- 10 theme: Doanh nghiệp Hiện đại, Điều hành Cao cấp, Học viện Di sản, Trường học Năng động, Tổ chức Tin cậy, Xưởng Sáng tạo, Giáo dục Xanh, Tạp chí Cổ điển, Tối giản An nhiên và Trung tâm Công nghệ.
- Theme thay typography, geometry, density, surface, shadow, navigation, hero, KPI, form, bảng và login; scenery fantasy cũ không còn chạy trong 0.13.
- Sidebar có ba nhóm Học tập/Đánh giá/Quản trị, tự mở route hiện tại, chỉ một nhóm mở và mục con lọc theo quyền.
- `themeKey` được validate/lưu thật; root layout SSR từ public branding; Flyway V5 ánh xạ dữ liệu 0.12 sang catalog mới.
- Chromium render 23 capture và một contact sheet; không ghi nhận page/console/server error.
- 116/116 test, semantic TypeScript, Next.js production build và npm production audit đạt.

Đây vẫn là **full-source release candidate**, chưa phải chứng nhận production. Xem `docs/DIVERSE_THEME_STUDIO_0.13.0.md`, `docs/BUILD_VERIFICATION_0.13.0.md` và `DELIVERY_STATUS.md`.
