# Diverse Theme Studio — LMSPilot 0.13.0

Ngày xác minh: **04/08/2026**.

## Mục tiêu

0.13.0 thay catalog thiên về fantasy/không gian của 0.12 bằng mười cá tính thiết kế thực sự khác nhau, dùng được cho doanh nghiệp, trường học, cơ quan, tổ chức xã hội và nhóm sáng tạo. System Admin vẫn thao tác tại `Thiết lập thương hiệu → Theme Studio`: tìm/lọc, xem thử trên toàn trang, hoàn tác và chỉ ghi cấu hình khi bấm **Áp dụng toàn hệ thống**.

Preset không chỉ đổi màu. Registry và CSS điều khiển typography, tỷ lệ bo góc, đường viền, shadow, mật độ, sidebar, topbar, hero, KPI, quick action, form, bảng, modal, login và responsive surface. Các lớp sao, nebula, horizon và quỹ đạo cũ bị vô hiệu hóa trong runtime 0.13.

## Mười cá tính hiện hành

| # | Theme | Nhóm | Mode | Bối cảnh phù hợp | Khác biệt chính |
|---:|---|---|---|---|---|
| 01 | Doanh nghiệp Hiện đại | Doanh nghiệp | Sáng | Công ty, trung tâm đào tạo nội bộ | Blue/teal, bố cục sạch, card rõ và phổ dụng |
| 02 | Điều hành Cao cấp | Doanh nghiệp | Tối | Ban điều hành, thương hiệu cao cấp | Navy, champagne, serif và đường nhấn trang trọng |
| 03 | Học viện Di sản | Giáo dục | Sáng | Đại học, học viện lâu đời | Giấy ấm, burgundy, serif và double border |
| 04 | Trường học Năng động | Giáo dục | Sáng | Trường phổ thông, môi trường trẻ | Khối màu thân thiện, góc tròn, KPI nhiều màu có trật tự |
| 05 | Tổ chức Tin cậy | Tổ chức | Sáng | Cơ quan, NGO, đơn vị quy mô lớn | Xanh công vụ, góc chắc, line/border ưu tiên thông tin |
| 06 | Xưởng Sáng tạo | Sáng tạo | Sáng | Agency, truyền thông, thiết kế | Purple/pink, color block và offset shadow editorial |
| 07 | Giáo dục Xanh | Giáo dục | Sáng | Bền vững, sức khỏe, cộng đồng | Sage/đất nung, bề mặt mềm và geometry organic ổn định |
| 08 | Tạp chí Cổ điển | Sáng tạo | Sáng | Nghiên cứu, xuất bản, văn hóa | Burgundy, serif, rule kiểu tạp chí và card vuông |
| 09 | Tối giản An nhiên | Tối giản | Sáng | Làm việc dài, cần tập trung | Sidebar sáng, gần như không shadow/grid, độ nhiễu thấp |
| 10 | Trung tâm Công nghệ | Tối giản | Tối | IT, vận hành, công ty công nghệ | Mono, góc vuông, lưới kỹ thuật và telemetry xanh |

Default mới là `enterprise-blue`. Tỷ lệ 8 sáng/2 tối có chủ đích: môi trường học tập và công việc dùng dài giờ được ưu tiên, trong khi vẫn có lựa chọn dark cho điều hành và kỹ thuật.

## Sidebar hai tầng

Điều hướng trái được rút từ danh sách dài thành ba nhóm chính:

1. **Học tập** — tổng quan, học tập cá nhân, lộ trình, khóa học, lớp, lịch trực tuyến, bản tin và bảo mật tài khoản.
2. **Đánh giá** — kỳ thi, cuộc thi, AI Studio, tài liệu, chấm điểm, kết quả, năng lực, chứng chỉ và báo cáo.
3. **Quản trị** — người dùng/quyền, tổ chức, tự động thông báo, vận hành và thiết lập thương hiệu.

Mỗi mục con vẫn đi qua cùng logic role/permission như trước. Nhóm không có mục con được phép sẽ không xuất hiện; route hiện tại tự mở đúng nhóm; người dùng có thể thu gọn và tối đa một nhóm mở cùng lúc. Toggle dùng button thật, `aria-expanded`, `aria-controls` và panel ID ổn định.

## Persistence và migration

1. `GET /api/v1/branding` tải theme/palette đã lưu cho Admin.
2. Xem thử chỉ cập nhật `data-theme` và CSS variables trong DOM; chưa ghi server.
3. Hoàn tác trả theme/palette về trạng thái đã commit.
4. Áp dụng gọi `PUT /api/v1/branding`; Configuration Service kiểm quyền, allowlist và lưu database.
5. `GET /public/v1/branding` cung cấp nhận diện cho root layout; SSR đặt `data-theme` ngay trên thẻ `html`.

Flyway `V5__diverse_theme_catalog.sql` tháo constraint V4, ánh xạ từng khóa cũ sang theme gần nhất, đổi default sang `enterprise-blue` rồi thêm allowlist 10 khóa mới. V4 không bị sửa nên cả fresh install và upgrade đều có lịch sử migration nhất quán. Client normalize khóa lạ về default để không làm vỡ giao diện.

## Visual QA

Chromium chạy production build ở desktop 1440 px và mobile 390 px. Runner đi qua login, dashboard, course, class, exam, learning, Theme Studio và từng theme dashboard. Dữ liệu lấp đầy màn hình là fixture intercept chỉ có trong runner QA tạm, không nằm trong source/bundle sản phẩm.

Kết quả:

- **23/23 ảnh capture** được tạo; contact sheet tổng hợp là ảnh thứ 24 trong thư mục.
- `pageErrors: []`.
- `serverErrors: ""`.
- Ảnh so sánh: `screenshots/0.13.0/00-theme-comparison.png`.
- Gallery Admin: `screenshots/0.13.0/09b-theme-studio-full.png`.
- 10 dashboard: `screenshots/0.13.0/10-theme-01-*.png` đến `10-theme-10-*.png`.
- Report máy đọc: `screenshots/0.13.0/visual-qa.json`.

Theme Giáo dục Xanh được kiểm riêng sau khi phát hiện Chromium compositor tạo một dải không vẽ trên screenshot trang dài. Bề mặt hero được đổi sang một compositing layer đồng nhất; DOM, hit testing và ảnh capture cuối đều đạt.

## Mã nguồn trọng yếu

- `apps/web/lib/themes.ts`: registry type-safe của 10 preset mới.
- `apps/web/app/themes-v013.css`: token, structure adaptation và accordion shell.
- `apps/web/components/CosmicShell.tsx`: sidebar hai tầng permission-aware.
- `apps/web/components/WorkspaceControlCenter.tsx`: gallery/filter/preview/reset/apply.
- `apps/web/app/layout.tsx`: SSR theme/public branding.
- `backend/services/configuration-service/.../CustomizationApi.kt`: allowlist API.
- `backend/services/configuration-service/.../V5__diverse_theme_catalog.sql`: migration/constraint.
- `tests/test_v013_diverse_ui.py`: 5 regression contract riêng cho 0.13.

## Giới hạn xác minh

Frontend typecheck/build, 116 test và Chromium render đã đạt. Môi trường tạo bản không có Java 21/Gradle distribution/Docker Engine, nên chưa chạy backend compile, migration thật trên PostgreSQL hoặc browser E2E gắn stack thật. Cần chạy các gate đó, accessibility audit chính thức và UAT trước production. Xem `BUILD_VERIFICATION_0.13.0.md`.
