# Spectrum OS — UI/UX rebuild 0.10.0

Ngày chốt: **04/08/2026**.

## Kết luận

0.10.0 là một bản dựng lại giao diện, không phải reskin 0.9.0. Lớp Astral Academy V3 đã bị gỡ khỏi runtime: `astral-v3.css`, `PortalShell`, `MysticBackdrop`, biểu tượng Mystic và toàn bộ câu chữ fantasy không còn trong source đang chạy. API, quyền, model dữ liệu và quy tắc nghiệp vụ được giữ nguyên.

## Nghiên cứu tham chiếu

Nhóm thiết kế tham khảo các gallery có hội đồng tuyển chọn thay vì sao chép một website cụ thể:

- [Awwwards — Colorful](https://www.awwwards.com/websites/colorful/): bảng màu bão hòa có chủ đích, nền mạnh và tương phản rõ.
- [Awwwards — Interactive](https://www.awwwards.com/websites/web-interactive/): phản hồi vi mô, chuyển động có mục đích và cảm giác khám phá.
- [Awwwards — Sites of the Year](https://www.awwwards.com/websites/sites_of_the_year/): typography đóng vai trò cấu trúc, bố cục bất đối xứng và storytelling theo nhịp cuộn.
- [CSS Design Awards](https://www.cssdesignawards.com/): cân bằng giữa visual design, UI, UX và khả năng sử dụng.
- [Godly](https://godly.design/): bố cục web đương đại, bento, editorial type và art direction táo bạo.

Các nguyên tắc được chuyển hóa thành sản phẩm LMS: 3–5 màu nổi bật trong từng scene, chữ lớn nhưng vẫn giữ thứ bậc, bento bất đối xứng, artwork CSS tự chứa, chuyển động chỉ để chỉ báo trạng thái. Không dùng font, ảnh, script hoặc asset của website tham chiếu.

## Ngôn ngữ Spectrum OS

- **Canvas:** nền giấy sáng, texture điểm mờ và chromatic field; không còn nền vũ trụ tối.
- **Navigation:** rail đen chuyên nghiệp, mỗi nhóm có màu nhấn; active state lime/pink; route vẫn lọc theo quyền thật.
- **Typography:** editorial display bằng system font, tiêu đề cỡ lớn, nhãn mono-like và số liệu đậm.
- **Color:** coral, pink, violet, blue, cyan, lime và yellow; mỗi module nhận một tổ hợp riêng.
- **Material:** viền đen 1.5–2px, hard shadow có kiểm soát, surface sáng, soft gradient và hình khối 2D/3D giả lập bằng CSS.
- **Motion:** pulse, orbit, drift và ticker; mọi animation tắt gần như tức thời khi hệ điều hành bật reduced motion.

## Bề mặt đã dựng lại

| Bề mặt | Thay đổi chính | Dữ liệu/luồng được giữ |
|---|---|---|
| Login | Editorial poster, sculpture tiến độ, proof strip, access card mới | Login BFF, fail-closed, forced password flow |
| Portal shell | Sidebar, topbar, mobile drawer và command palette mới | RBAC, role/account type, notification, logout |
| Dashboard | Hero dữ liệu, progress art, KPI cards, quick-workspace deck, chart/donut/activity | Reporting/Course/Class/Grade/User/Notification API |
| Khóa học | Header, summary, toolbar và collection card artwork mới | Lọc, tạo, mở chi tiết, trạng thái publish/draft |
| Lớp | Header, summary và numbered training rows mới | Mở lớp, course version, instructor, deadline |
| Học tập | Personal header, journey poster, mastery disc và learning cards mới | Enrollment, tiến độ server-side, resume route |
| Kỳ thi | Assessment scene, score artwork và card grid mới | Question bank, exam create/take, version/status |
| Đổi mật khẩu | Security poster và form gate mới | Policy, validation và auth mutation |
| Workspace nâng cao | Token/surface/form/table/modal/material phủ lại | API, scoped permissions và business state không đổi |

## Responsive và accessibility

- Sáu ngưỡng: 1480, 1240, 1080, 900, 720 và 480 px.
- Sidebar chuyển thành drawer ở tablet/mobile; focus-visible rõ; control có label/aria-label.
- Nội dung không phụ thuộc hover; thanh cuộn được ẩn thị giác nhưng vẫn cuộn bằng chuột, touch và bàn phím.
- Có `prefers-reduced-motion` và `prefers-contrast: more`.
- Không có `@import`, URL ảnh hoặc font ngoài trong stylesheet Spectrum.

## Ảnh render từ build production

Các ảnh dưới đây được chụp từ Next.js production build bằng Chromium. Dữ liệu mẫu chỉ được intercept trong tiến trình QA để lấp đầy màn hình; source production vẫn gọi API thật và production real-mode guard vẫn cấm mock/fallback.

| Ảnh | Kích thước |
|---|---:|
| `screenshots/0.10.0/01-login-desktop.png` | 1440 × 1000 |
| `screenshots/0.10.0/02-dashboard-desktop.png` | 1440 × 1100 |
| `screenshots/0.10.0/02b-dashboard-full.png` | 1440 × 2274 |
| `screenshots/0.10.0/03-courses-desktop.png` | 1440 × 1100 |
| `screenshots/0.10.0/03b-courses-full.png` | 1440 × 1682 |
| `screenshots/0.10.0/04-classes-desktop.png` | 1440 × 1100 |
| `screenshots/0.10.0/05-exams-desktop.png` | 1440 × 1100 |
| `screenshots/0.10.0/06-learning-desktop.png` | 1440 × 1100 |
| `screenshots/0.10.0/07-learning-mobile.png` | 390 × 844 |
| `screenshots/0.10.0/07b-learning-mobile-full.png` | 390 × 4022 |
| `screenshots/0.10.0/08-login-mobile-full.png` | 390 × 1383 |

`screenshots/0.10.0/visual-qa.json` ghi nhận danh sách ảnh, `pageErrors: []` và không có lỗi server trong lượt chụp.

## Ranh giới xác minh

Browser render trên Chromium, TypeScript và build frontend đã đạt. Đây không thay thế Compose integration, browser đa engine, accessibility audit chính thức, test tải, pentest hoặc UAT với dữ liệu thật. Xem `BUILD_VERIFICATION_0.10.0.md` và `../DELIVERY_STATUS.md`.
