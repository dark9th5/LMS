# UI redesign 0.18.0

## Nguyên tắc

1. Mỗi mục điều hướng chỉ thể hiện một tên hành động rõ ràng; thông tin giải thích đặt trong trang đích, không lặp dưới menu.
2. Sidebar và panel cây tổ chức là vùng ổn định, có thể thu gọn hoặc sticky để tăng diện tích làm việc.
3. Khóa học là thực thể chính; lớp là một lần triển khai khóa học nên nằm trong cùng khu vực thay vì cạnh tranh ở điều hướng cấp một.
4. Sáng/tối là sở thích hiển thị cá nhân. Màu thương hiệu là cấu hình cấp hệ thống. Hai lớp cấu hình không ghi đè lẫn nhau.
5. Cấu hình tích hợp dùng trường có nhãn và validation theo loại dịch vụ, không yêu cầu khách hàng sửa JSON thô.
6. Trạng thái ít thay đổi được ghi nhớ bằng URL hoặc localStorage: tab khóa học qua query string, tab cài đặt, đơn vị tổ chức và trạng thái sidebar qua localStorage.

## Dịch vụ ngoài

- Redis: host, port, username, database, TLS và mật khẩu.
- SMTP: host, port, tài khoản, STARTTLS/TLS, địa chỉ và tên người gửi.
- AI: endpoint, model và API key; contract được đánh dấu OpenAI-compatible.
- S3: endpoint, bucket, region, access key, secret access key, HTTPS và path-style.
- ONLYOFFICE: Document Server URL, callback URL công khai và JWT secret.
- Họp trực tuyến: nhà cung cấp, endpoint và token/secret khi cần.

Mỗi biểu mẫu có liên kết đến tài liệu chính thức ngay trong giao diện, đồng thời backend từ chối cấu hình thiếu trường bắt buộc hoặc URL/cổng không hợp lệ.
