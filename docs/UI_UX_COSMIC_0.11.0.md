# Cosmic Research UI — refinement 0.11.0

Ngày chốt: **04/08/2026**.

## Kết luận

0.11.0 thay toàn bộ ngôn ngữ Spectrum OS bão hòa màu của 0.10.0 bằng một **đài quan sát tri thức**: khoa học, công nghệ, không gian và huyền bí nhưng vẫn ưu tiên đọc dữ liệu. Đây không phải lớp đổi màu đặt lên giao diện cũ. Runtime dùng identity, shell, nền, logo, token, surface và artwork mới: `CosmicShell`, `CosmicField`, `cosmic-v011.css` và `orbit-mark.svg`.

API, scoped RBAC, trạng thái tải/lỗi/rỗng và quy tắc nghiệp vụ không đổi. Production source tiếp tục gọi API thật; không thêm mock hoặc fallback dữ liệu vào ứng dụng.

## Nghiên cứu tham chiếu

- [NASA Eyes](https://eyes.nasa.gov/apps/solar-system/): cách biểu diễn quỹ đạo, thiên thể và dữ liệu không gian như một công cụ quan sát thay vì hình nền trang trí.
- [Carbon Design System — Themes](https://carbondesignsystem.com/elements/themes/overview/): mô hình dark theme nhiều lớp, trong đó layer nổi được làm sáng dần để giữ phân cấp mà không cần dùng quá nhiều màu.
- [IBM Design Language — Color](https://www.ibm.com/design/language/color): dùng blue và neutral làm lõi; màu bổ sung phải có mục đích, tiết chế và gắn với ý nghĩa.
- [Awwwards — Technology](https://www.awwwards.com/websites/technology/): typography, chiều sâu và storytelling công nghệ đương đại.
- [The Race to Save Space](https://www.awwwards.com/sites/the-race-to-save-space): cảm giác khám phá không gian thông qua nhịp cảnh, scale và chuyển động có chủ đích.

Không sao chép layout, asset, font hoặc mã nguồn từ các trang tham chiếu. Các nguyên tắc được chuyển hóa thành cấu trúc phù hợp với LMS có mật độ dữ liệu cao.

## Hệ thị giác

| Vai trò | Màu/lớp chính | Quy tắc sử dụng |
|---|---|---|
| Không gian nền | `#030711`, `#050b16` | Canvas toàn cục, tạo chiều sâu và giảm chói |
| Bề mặt dữ liệu | graphite/navy nhiều lớp | Layer nổi sáng hơn layer cha; viền xanh xám mảnh |
| Dữ liệu lõi | blue | Hành động chính, tiến độ và vùng đang hoạt động |
| Telemetry | cyan | Live state, icon, đường quỹ đạo, focus và tín hiệu nhỏ |
| Chiều sâu | indigo/violet | Nebula, thiên thể phụ và điểm nhấn huyền bí; không làm nền chữ dài |
| Cảnh báo | amber/coral | Chỉ dùng cho quá hạn, lỗi hoặc trạng thái cần chú ý |
| Thành công | mint | Trạng thái ổn định, hoàn thành và kết nối |

Star field, lưới tọa độ, nebula, horizon và orbital telemetry đều là CSS/SVG nội bộ, opacity thấp và không cạnh tranh với nội dung. Không có font, ảnh, script hoặc stylesheet tải từ CDN.

## Bề mặt đã thay

| Bề mặt | Cấu trúc 0.11.0 | Luồng thật được giữ |
|---|---|---|
| Login | Observatory poster, orbit-sync core, secure access module | BFF login, fail-closed, forced password change |
| Portal shell | Mission groups, encrypted-link state, telemetry topbar, Orbit Navigator | Quyền route, notification, logout, `Ctrl/Cmd + K`, `Escape` |
| Dashboard | Mission-control hero, orbital progress, signal cards, operation modules | Reporting/Course/Class/Grade/User/Notification API |
| Khóa học | Scientific archive header, telemetry summaries, planetary collection cards | Tìm/lọc, tạo, publish/draft, mở detail |
| Lớp | Training-orbit header và numbered mission rows | Course version, instructor, deadline, open/closed |
| Học tập | Personal observatory, mastery orbit và learning mission cards | Enrollment, tiến độ server-side, resume route |
| Kỳ thi | Assessment modules, threshold orbit và mission metadata | Question bank, create/take, version/status |
| Detail/workspace | Dark-layer form, table, modal, player và admin console | API, scoped permissions và business state |

## Responsive và khả năng tiếp cận

- Các ngưỡng chính: 1080, 900, 720 và 480 px; shell chuyển sang mobile drawer ở tablet/điện thoại.
- Journey mobile chuyển từ lưới hai cột sang một cột ở cấp layout. Tiêu đề, ba chỉ số và mastery orbit giữ đúng thứ tự đọc ở 390 px.
- Có focus-visible, label/aria-label, `prefers-reduced-motion` và `prefers-contrast: more`.
- Nội dung không phụ thuộc hover; animation chỉ dùng cho star drift/orbit và bị tắt khi người dùng yêu cầu giảm chuyển động.
- Màu nóng không được dùng làm surface lớn; cảnh báo quá hạn là ngoại lệ có ý nghĩa nghiệp vụ.

## Ảnh render từ production build

Dữ liệu hiển thị trong ảnh là fixture intercept cô lập trong runner QA để lấp đầy màn hình. Fixture không nằm trong bundle và không thay API production.

| Ảnh | Kích thước |
|---|---:|
| `screenshots/0.11.0/01-login-desktop.png` | 1440 × 1000 |
| `screenshots/0.11.0/02-dashboard-desktop.png` | 1440 × 1100 |
| `screenshots/0.11.0/02b-dashboard-full.png` | 1440 × 2278 |
| `screenshots/0.11.0/03-courses-desktop.png` | 1440 × 1100 |
| `screenshots/0.11.0/03b-courses-full.png` | 1440 × 1668 |
| `screenshots/0.11.0/04-classes-desktop.png` | 1440 × 1100 |
| `screenshots/0.11.0/05-exams-desktop.png` | 1440 × 1100 |
| `screenshots/0.11.0/06-learning-desktop.png` | 1440 × 1100 |
| `screenshots/0.11.0/07-learning-mobile.png` | 390 × 844 |
| `screenshots/0.11.0/07b-learning-mobile-full.png` | 390 × 3768 |
| `screenshots/0.11.0/08-login-mobile-full.png` | 390 × 1374 |

`screenshots/0.11.0/visual-qa.json` ghi nhận `pageErrors: []` và `serverErrors: ""` trong lượt chụp.

## Ranh giới xác minh

Chromium production render, TypeScript và frontend build đã đạt. Đây không thay thế Gradle/backend build, Compose integration, Firefox/WebKit, accessibility audit chính thức, test tải, pentest hoặc UAT với dữ liệu thật. Xem `BUILD_VERIFICATION_0.11.0.md` và `../DELIVERY_STATUS.md`.
