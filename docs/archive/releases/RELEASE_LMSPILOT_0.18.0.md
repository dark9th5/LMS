# LMSPilot 0.18.0 — Compact Workspace & Brand Personalization

Bản phát hành này tập trung sửa trực tiếp các vấn đề UI/UX được ghi nhận trong sidebar, cây tổ chức và trang cài đặt.

## Thay đổi chính

- Sidebar chỉ còn nhãn một dòng, có chế độ thu gọn và ghi nhớ lựa chọn trong trình duyệt.
- Khóa học và lớp học dùng chung một mục `Khóa học`; lớp trở thành tab `Lớp triển khai` để vẫn giữ đầy đủ nghiệp vụ lịch, ghi danh và học viên.
- Khu vực Tổ chức được chuyển thành bố cục hai vùng: cây tổ chức cố định bên trái và vùng quản lý rộng bên phải.
- Đơn vị đang chọn được lưu cục bộ, không bị reset khi chuyển mục rồi quay lại.
- Gán thành viên bằng danh sách tìm kiếm theo tên, tài khoản hoặc email, thay cho nhập user ID thủ công.
- `Bộ nhận diện` được thay bằng `Cấu hình thông tin`, hỗ trợ upload logo PNG/JPG, tên thương hiệu, giới thiệu, tên miền và màu chủ đạo.
- Không còn lựa chọn sáng/tối trong Cài đặt. Chế độ hiển thị vẫn thuộc nút trên topbar và được lưu theo từng người dùng.
- Dịch vụ ngoài có biểu mẫu riêng cho Redis, SMTP, AI tương thích OpenAI, S3, ONLYOFFICE Docs và họp trực tuyến.
- Backend kiểm tra trường bắt buộc, cổng, URL HTTP/HTTPS, callback ONLYOFFICE và thực hiện health check phù hợp theo nhóm dịch vụ.

## Tương thích

- Route `/classes` được giữ để tương thích liên kết cũ nhưng tự chuyển sang `/courses?view=classes`.
- API hiện có không bị xóa.
- Dữ liệu branding và external service cũ vẫn được đọc; cấu hình bí mật không bị trả về frontend.

## Nâng cấp

Giữ nguyên thư mục `.git`, tạo branch mới, sao lưu dữ liệu và thay toàn bộ source bằng nội dung gói 0.18.0. Sau đó chạy validator, test frontend/backend và migration trong môi trường CI hoặc máy có dependency cache.
