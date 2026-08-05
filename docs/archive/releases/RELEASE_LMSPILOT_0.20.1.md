# LMSPilot 0.20.1 — Polished three-role UI

Bản 0.20.1 giữ nguyên toàn bộ kiến trúc và nghiệp vụ ba vai trò của 0.20.0, nhưng thực hiện một vòng tinh chỉnh giao diện theo phong cách 0.18 đã được chọn.

## Phạm vi

- Không thay đổi ranh giới quyền `ADMIN`, `INSTRUCTOR`, `STUDENT`.
- Không khôi phục Lớp học; sản phẩm vẫn chỉ dùng Khóa học.
- Bài kiểm tra vẫn nằm trong khóa học; Bài thi vẫn là kỳ thi độc lập.
- Giữ khả năng đổi logo, màu thương hiệu và ảnh nền đăng nhập.
- Giữ 19 service độc lập và port riêng.

## Tinh chỉnh giao diện

- Bỏ thẻ vai trò lớn trong sidebar; vai trò vẫn hiển thị ở hồ sơ và thanh trên.
- Sidebar gọn, mục đang chọn có thanh chỉ báo tinh tế, không dùng dấu `>`.
- Tiêu đề trang thấp và cân đối hơn, tăng diện tích thao tác.
- Dashboard giảm kích thước hero, cân lại thẻ số liệu và danh sách.
- Đồng bộ bán kính, border, shadow, form, table và trạng thái hover/focus.
- Tăng độ rõ của chữ nhỏ mà không làm giao diện nặng.
- Tinh chỉnh riêng vùng biên soạn khóa học, tạo bài kiểm tra, làm bài và xem DOCX/PDF.
- Dark mode dùng viền xanh-xám dịu, không dùng viền trắng sáng.

Bộ ảnh QA tĩnh nằm tại `docs/screenshots/0.20.1`.
