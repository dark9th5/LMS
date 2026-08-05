# Soft Spectrum — LMSPilot 0.14.0

Ngày xác minh: **04/08/2026**.

## Yêu cầu được hiện thực

0.14.0 giữ điều người dùng thích ở Spectrum UI — sáng, nhiều màu, hình khối rõ và giàu năng lượng — nhưng giảm cảm giác sặc sỡ. Yêu cầu bổ sung “phần mục lục tránh pha màu” được xử lý như một invariant riêng: palette đa sắc chỉ được dùng trong content, còn sidebar phải đơn sắc bất kể group đang mở.

Theme mặc định mới là **Sắc màu Cân bằng** với key `soft-spectrum`. Theme Studio vẫn có đúng 10 lựa chọn; `enterprise-blue` được thay bằng theme này, chín cá tính 0.13 còn lại được giữ.

## Hệ màu

| Vai trò | Màu | Cách dùng |
|---|---|---|
| Functional primary | `#B95547` | CTA, tag, trạng thái chọn ngoài sidebar |
| Functional secondary | `#5967B8` | Dữ liệu/phụ trợ |
| Canvas | `#F6F3EF` | Nền làm việc ấm, giảm chói |
| Surface | `#FFFDF9` | Form, panel và card nội dung |
| Text | `#20232E` | Chữ chính có tương phản cao |
| Coral | `#E8927E` | Hero/course accent |
| Rose | `#E8A1BB` | Hero/page section |
| Aqua | `#77C7CC` | Progress và data card |
| Violet | `#9490DB` | Chiều sâu, module card |
| Lime | `#B7CF83` | KPI tích cực |
| Yellow | `#E8C96E` | Nhấn nhẹ và hình học |

Các màu diện tích lớn là pastel trung bình, không phải neon. Dark ink và off-white tạo vùng nghỉ; màu đậm hơn chỉ dành cho nút, icon hoặc dữ liệu nhỏ.

## Sidebar không pha màu

- Rail cố định `#191B28`; surface nâng `#222532`; hover `#2A2D3B`.
- Học tập, Đánh giá và Quản trị không còn accent cyan/indigo/violet riêng.
- Group icon, count, đường cây và mục con cùng hệ neutral gray.
- Active item dùng off-white `#F0EDE7` và chữ ink; không dùng gradient hoặc colored glow.
- Logo mặc định trong rail cũng là trắng/xám. Màu xanh của chấm “hệ thống hoạt động” được giữ vì đây là trạng thái nghiệp vụ, không phải trang trí mục lục.
- Sidebar vẫn lọc theo permission, tự mở group chứa route hiện tại, chỉ mở một group và giữ `aria-expanded`/`aria-controls`.

## Content tươi sáng có kiểm soát

- Dashboard hero giữ color blocking coral–rose–violet cùng aqua/yellow circle, nhưng giảm saturation và chuyển toàn bộ copy sang ink để tăng khả năng đọc.
- KPI dùng bốn tint lime/aqua/yellow/violet; icon nằm trên surface trung tính.
- Quick route đặt trên deck ink; ba action card dùng violet/coral/green vừa phải vì diện tích nhỏ.
- Page header, login, course cover, summary card, class list và exam scene dùng pastel có khoảng trắng.
- Các selector Cosmic cũ từng tạo card tối hoặc title thiếu tương phản đã được override riêng cho Soft Spectrum.
- Progress disc đọc `--progress-accent`/`--progress-track` thay cho màu hard-code.

## Persistence và migration

Flyway `V6__soft_spectrum_default.sql`:

1. Tháo constraint allowlist V5.
2. Chỉ thay palette khi row `enterprise-blue` khớp chính xác palette Enterprise 0.13 hoặc seed fantasy nguyên bản; palette tùy chỉnh khác được giữ.
3. Ánh xạ `enterprise-blue` sang `soft-spectrum`.
4. Chỉ đổi tên/giới thiệu “Học viện Huyền Tri” nếu profile mặc định chưa từng được đổi tên.
5. Đặt database default `soft-spectrum` và thêm allowlist 10 key mới.

Frontend default, Kotlin entity/request default, API regex và Theme Studio initial state cùng dùng một key/palette. Root layout tiếp tục SSR `data-theme` từ public branding.

## Visual QA

Chromium chạy production build ở desktop 1440 px và mobile 390 px. Runner đi qua login, dashboard, khóa học, lớp, kỳ thi, learning, Theme Studio và đủ 10 theme.

Kết quả:

- **23/23 capture** được tạo; contact sheet là ảnh thứ 24.
- `pageErrors: []`.
- `serverErrors: ""`.
- Dashboard mặc định: `screenshots/0.14.0/02-dashboard-desktop.png`.
- Theme Studio: `screenshots/0.14.0/09b-theme-studio-full.png`.
- Soft Spectrum: `screenshots/0.14.0/10-theme-01-soft-spectrum.png`.
- So sánh 10 theme: `screenshots/0.14.0/00-theme-comparison.png`.
- Report máy đọc: `screenshots/0.14.0/visual-qa.json`.

Visual review được lặp lại sau khi phát hiện lớp Cosmic cũ làm tên khóa học và danh sách lớp thiếu tương phản. Capture cuối xác nhận course/class/exam đều sáng, chữ rõ và sidebar không đổi màu theo group.

## Mã nguồn trọng yếu

- `apps/web/app/themes-v014.css`: Soft Spectrum, monochrome rail và adaptation nghiệp vụ.
- `apps/web/lib/themes.ts`: registry/default mới.
- `apps/web/components/Dashboard.tsx`: progress theme token.
- `backend/services/configuration-service/.../V6__soft_spectrum_default.sql`: upgrade migration.
- `tests/test_v014_soft_spectrum.py`: 5 regression contract.

## Giới hạn xác minh

Frontend typecheck/build, 121 test và Chromium render đã đạt. Môi trường tạo bản không có Java 21/Gradle distribution/Docker Engine nên chưa chạy backend compile, migration thật trên PostgreSQL hoặc browser E2E gắn stack thật. Xem `BUILD_VERIFICATION_0.14.0.md`.
