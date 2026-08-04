# LMSPilot CLS 0.14.0 — Soft Spectrum release candidate

0.14.0 giữ giao diện đa sắc tươi sáng nhưng giảm saturation và tách mục lục khỏi mọi accent màu. Soft Spectrum trở thành default trong catalog 10 theme; sidebar tiếp tục là accordion permission-aware nhưng chỉ dùng navy–graphite–xám–trắng.

Điểm chính:

- Hero, KPI, summary, course, class, exam, login và page header dùng pastel coral/rose/aqua/violet/lime/yellow có khoảng nghỉ.
- Sidebar Học tập/Đánh giá/Quản trị không pha màu theo group; active item off-white, icon/count/branch cùng neutral gray.
- Theme Studio vẫn đủ 10 theme; preview Soft Spectrum phản ánh đúng rail đơn sắc.
- Flyway V6 đổi default/allowlist, map `enterprise-blue` và bảo toàn custom palette.
- Chromium render 23 capture và contact sheet; 0 page/console/server error.
- 121/121 test, semantic TypeScript, Next.js production build và npm production audit đạt.

Đây vẫn là **full-source release candidate**, chưa phải chứng nhận production. Xem `docs/SOFT_SPECTRUM_0.14.0.md`, `docs/BUILD_VERIFICATION_0.14.0.md` và `DELIVERY_STATUS.md`.
