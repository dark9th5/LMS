# Astral Academy V3 — UI/UX redesign 0.9.0

Ngày hoàn thiện: **04/08/2026**.

## Kết quả

Portal 0.9.0 không giữ nguyên bố cục cũ rồi thay màu. Toàn bộ ngôn ngữ trải nghiệm đã được dựng lại thành **Astral Academy V3**: một học viện số có chiều sâu không gian, đủ giàu chi tiết cho sản phẩm enterprise nhưng vẫn giữ thứ bậc, khả năng đọc và tốc độ thao tác.

API, permission, dữ liệu và route nghiệp vụ được giữ nguyên để tránh hồi quy. Lớp trình bày, kiến trúc điều hướng, bố cục trang, visual hierarchy, trạng thái tương tác và responsive behavior đều được thay mới.

## Nguồn tham chiếu và cách chuyển hóa

- [Awwwards Sites of the Year](https://www.awwwards.com/websites/sites_of_the_year/) và [Interactive collection](https://www.awwwards.com/websites/web-interactive/) được dùng để nghiên cứu chiều sâu, nhịp kể chuyện, composition bất đối xứng và chuyển động có chủ đích.
- [Linear design refresh](https://linear.app/now/behind-the-latest-design-refresh) là tham chiếu cho nguyên tắc cấu trúc nên được cảm nhận qua hierarchy, spacing và surface thay vì phô ra bằng quá nhiều đường viền.
- [W3C — Animation from Interactions](https://www.w3.org/WAI/WCAG22/Understanding/animation-from-interactions.html) là chuẩn tham chiếu cho cơ chế giảm chuyển động.

Các nguồn trên chỉ định hướng nguyên tắc. Giao diện không sao chép website cụ thể, không kéo asset của bên thứ ba và vẫn bám chủ đề “Học viện Huyền Tri” của sản phẩm.

## Hệ thiết kế mới

| Lớp | Hiện thực |
|---|---|
| Không gian | Aurora ribbon, celestial disc, constellation map, vignette, rune dust và lớp sương bằng CSS/SVG nội bộ |
| Surface | Obsidian glass, viền ánh kim nhiều cấp, glow theo ngữ cảnh và độ nổi có kiểm soát |
| Màu | Gold cho tri thức/hành động chính, cyan cho tín hiệu hệ thống, violet cho chiều sâu, coral cho cảnh báo |
| Typography | Display face cho tiêu đề nghi lễ, sans-serif có độ đọc cao cho nội dung và số liệu |
| Nhịp bố cục | Khu vực hero lớn, index/coordinate, section heading, grid có hierarchy và khoảng thở rõ |
| Chuyển động | Reveal, orbit, shimmer và ambient drift; toàn bộ bị vô hiệu hóa hoặc rút gọn khi người dùng chọn reduced motion |
| Khả năng truy cập | `focus-visible`, target rõ, tương phản nâng cao, semantic button/dialog, đóng palette bằng Escape |

File `apps/web/app/astral-v3.css` là lớp design system hiện hành và được import sau compatibility stylesheet trong `app/layout.tsx`. Cách tách lớp này cho phép phủ mới toàn bộ trải nghiệm mà không làm gãy hàng trăm class nghiệp vụ đang dùng dữ liệu thật.

## Điều hướng và tương tác

### Portal shell

- Sidebar trở thành một navigation rail nổi với logo, edition, nhóm route có đánh số và trạng thái realm.
- Top bar hiển thị ngữ cảnh khu vực hiện tại, trạng thái hệ thống và lối tắt bàn phím.
- Navigation vẫn permission-first: route không có quyền không được render.

### Command Atlas

- Mở bằng `Ctrl + K` hoặc `Cmd + K`, có nút thật trên giao diện.
- Tìm theo tiêu đề hoặc mô tả route.
- Chỉ tìm trong danh sách route đã qua permission filter.
- Hỗ trợ focus tự động, empty state, click backdrop và phím Escape.
- Kết quả là liên kết thật; chọn mục sẽ điều hướng và đóng palette.

Đây là chức năng thực, không phải ô tìm kiếm trang trí.

## Các bề mặt đã dựng lại

| Khu vực | Thay đổi chính |
|---|---|
| Login | Observatory hai cột, manifesto, feature signal, số liệu hệ thống, trust/security panel và identity form mới |
| Dashboard | Command center theo vai trò, progress orbit, telemetry, KPI signal, quick-action route deck và dữ liệu API thật |
| Khóa học | Archive summary, celestial course map, volume/index metadata, trạng thái và CTA rõ hơn |
| Lớp học | Class atlas, summary signal, tiến độ/sĩ số/giảng viên và hành động theo quyền |
| Hành trình học | Journey hero, orbit tiến độ, thống kê và card hành trình giàu trạng thái |
| Kỳ thi | Assessment observatory, score scene, deadline/duration/attempt và CTA theo trạng thái |
| Đổi mật khẩu | Protection chamber riêng, rule matrix, shield signal và form hai tầng |
| Trang chi tiết | Header thiên thể dùng chung, breadcrumb/coordinate và hierarchy thống nhất |
| Quản trị | Table, form, modal, tab, health panel, KPI, import wizard và advanced console được phủ design system mới |

## Responsive

Design system có điều chỉnh riêng tại các ngưỡng 1480, 1240, 1080, 900, 720 và 480 px:

- Thu gọn rail và header trước khi nội dung bị ép.
- Chuyển hero, metric grid, card atlas và login về một cột theo thứ tự đọc.
- Giữ CTA có kích thước chạm phù hợp, table có cuộn ngang và modal không tràn viewport.
- Giảm chi tiết trang trí ở màn hình nhỏ để ưu tiên nội dung và hiệu năng.

## Bảo toàn tính năng thật

- Không thay mock data vào các trang đang gọi API.
- Không bỏ permission gate, trạng thái loading/error/empty hoặc role-aware action.
- Không thay contract backend, route hoặc endpoint.
- Dynamic branding, notification, logout và forced-password flow vẫn hoạt động theo logic cũ.
- Không có `http(s)` visual dependency, CSS `url()` ngoài hoặc CDN font trong lớp redesign.

## Bằng chứng kiểm tra

- 8 UI regression test mới kiểm tra import order, permission-aware Command Atlas, các surface chính, login/password, external dependency, responsive, motion/contrast, advanced console và PageHeader consistency.
- Toàn suite: **104/104 PASS**.
- Semantic TypeScript: **65 source file PASS**.
- Next.js 16.2.12 optimized production build: **PASS**.
- Production real-mode guard: **PASS**.
- `npm audit --omit=dev`: **0 vulnerability**.

## Giới hạn xác minh

Môi trường tạo bản không có browser runtime và Docker Engine, nên chưa thể cung cấp screenshot QA, browser E2E, visual-regression pixel diff hoặc kiểm tra tích hợp với backend đang chạy. CSS/TSX đã qua semantic typecheck, production build và test cấu trúc; vẫn cần chạy Playwright/browser matrix, Lighthouse, WCAG audit và UAT trên máy nghiệm thu trước production.

Giới hạn backend của 0.8.2 vẫn còn: host chỉ có Java 17 trong khi project dùng Java 21, Gradle distribution không tải được và Docker không có. Xem `BUILD_VERIFICATION_0.9.0.md`.
