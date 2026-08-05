# Theme Studio — LMSPilot 0.12.0

Ngày xác minh: **04/08/2026**.

## Phạm vi

0.12.0 biến Cosmic Research UI thành một hệ sinh thái có thể chọn trực tiếp trong trang quản trị. System Admin mở `Thiết lập thương hiệu → Theme Studio`, tìm/lọc preset, xem thử trên toàn trang, hoàn tác nếu không phù hợp và chỉ ghi cấu hình khi bấm **Áp dụng toàn hệ thống**.

Đây không phải bộ đổi màu cục bộ. Mỗi preset điều khiển nền, surface, line, text, accent, typography hiển thị, geometry, radius, shadow, atmosphere, sidebar, topbar, hero, KPI, quick route, form, bảng, modal, login và responsive surface.

## Mười preset

| # | Theme | Nhóm | Chế độ | Định hướng |
|---:|---|---|---|---|
| 01 | Cosmic Observatory | Không gian | Tối | Cân bằng, huyền bí, cyan/violet có kiểm soát |
| 02 | Quantum Cyan | Khoa học | Tối | Phòng lab lượng tử, telemetry cyan rõ nét |
| 03 | Lunar Silver | Tối giản | Tối | Graphite/bạc, ít nhiễu, đọc lâu ít mỏi |
| 04 | Aurora Research | Không gian | Tối | Cực quang xanh lục và tím trên nền nghiên cứu |
| 05 | Mars Expedition | Không gian | Tối | Đỏ gạch/cam đồng cho nhiệm vụ và vận hành |
| 06 | Abyssal Ocean | Khoa học | Tối | Xanh vực sâu, sonar và sinh học biển |
| 07 | Biosphere Lab | Khoa học | Tối | Xanh sinh quyển, phòng thí nghiệm sự sống |
| 08 | Solar Archive | Học thuật | Sáng | Giấy ngà, amber, thư viện và kho lưu trữ |
| 09 | Neo Academia | Học thuật | Sáng | Trắng lạnh, cobalt, học viện công nghệ hiện đại |
| 10 | Mono Terminal | Tối giản | Tối | Monospace, góc vuông, bảng điều khiển kỹ thuật |

## Luồng hoạt động thật

1. `GET /api/v1/branding` tải theme và palette đã lưu cho Admin.
2. Xem thử chỉ cập nhật `data-theme` và CSS variables trong DOM hiện tại; chưa ghi server.
3. Hoàn tác trả toàn bộ theme/palette về trạng thái đã commit.
4. Áp dụng gọi `PUT /api/v1/branding` với `themeKey` và palette; Configuration Service kiểm quyền, validate allowlist và lưu database.
5. `GET /public/v1/branding` cung cấp nhận diện công khai cho root layout; SSR đặt `data-theme` ngay trên thẻ `html` và chọn `themeColor` phù hợp.
6. Mọi phiên mới nhận theme đã lưu; không dùng localStorage, không có fixture hoặc API giả trong production source.

Migration `V4__branding_theme_studio.sql` thêm cột `theme_key`, default an toàn `cosmic-observatory` và constraint chỉ cho phép 10 khóa hợp lệ. Client cũng normalize khóa lạ về preset mặc định để không làm vỡ giao diện khi dữ liệu cũ hoặc dữ liệu lỗi xuất hiện.

## Phân quyền và khả năng sử dụng

- Theme Studio nằm trong trung tâm thiết lập thương hiệu chỉ dành cho người có quyền tương ứng.
- Tìm theo tên, mô tả, tag; lọc theo nhóm và dark/light.
- Card có `aria-pressed`, nhãn rõ, focus-visible; animation tuân thủ `prefers-reduced-motion`.
- Hai preset sáng có override tương phản riêng cho topbar, hero metric, KPI và quick route.
- Sticky action bar luôn cho biết đang xem thử hay đã đồng bộ và cho phép hoàn tác trước khi ghi.

## Visual QA

Chromium chạy production build ở desktop 1440 px và mobile 390 px. Runner đi qua login, dashboard, course, class, exam, learning, Theme Studio và từng preset dashboard. Dữ liệu lấp đầy màn hình là fixture intercept chỉ có trong runner QA tạm, không nằm trong source/bundle sản phẩm.

Kết quả:

- **23/23 ảnh** được tạo.
- `pageErrors: []`.
- `serverErrors: ""`.
- Ảnh so sánh: `screenshots/0.12.0/00-theme-comparison.png`.
- Gallery Admin: `screenshots/0.12.0/09b-theme-studio-full.png`.
- 10 ảnh dashboard: `screenshots/0.12.0/10-theme-01-*.png` đến `10-theme-10-*.png`.
- Report máy đọc: `screenshots/0.12.0/visual-qa.json`.

## Mã nguồn trọng yếu

- `apps/web/lib/themes.ts`: registry type-safe của 10 preset.
- `apps/web/app/themes-v012.css`: token và component adaptation đa theme.
- `apps/web/components/WorkspaceControlCenter.tsx`: gallery, filter, preview, reset và apply.
- `apps/web/app/layout.tsx`: SSR theme/public branding.
- `backend/services/configuration-service/.../CustomizationApi.kt`: contract/validation/persistence.
- `backend/services/configuration-service/.../V4__branding_theme_studio.sql`: migration và database constraint.
- `tests/test_v012_theme_studio.py`: 7 regression contract riêng cho Theme Studio.

## Giới hạn xác minh

Frontend typecheck/build, 111 test và Chromium render đã đạt. Môi trường tạo bản không có Java 21/Gradle distribution/Docker Engine, nên chưa chạy backend compile, migration thật trên PostgreSQL hoặc browser E2E gắn stack thật. Cần chạy các gate đó, accessibility audit chính thức và UAT trước production. Xem `BUILD_VERIFICATION_0.12.0.md`.
