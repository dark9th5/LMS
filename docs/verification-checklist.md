# Checklist xác minh bản cài LMSPilot

## 1. Trước khi build

- Docker Engine/Docker Desktop đang chạy.
- Docker Compose v2 hoạt động.
- Còn tối thiểu khoảng 10 GB dung lượng trống.
- File `.env` không còn giá trị `replace-with-*` hoặc `ChangeMe-*`.
- Chạy `scripts/preflight.*` thành công.

## 2. Build và khởi động

- Chạy `scripts/setup.sh` hoặc `scripts/setup.ps1`.
- Tất cả container lõi ở trạng thái running/healthy.
- Script kết thúc bằng `SMOKE TEST PASSED`.

## 3. Luồng UAT cốt lõi

1. Admin đăng nhập, xem người dùng/cơ cấu tổ chức.
2. Giảng viên tạo khóa học, thêm ít nhất một bài học và xuất bản.
3. Admin mở lớp từ khóa học đã xuất bản và ghi danh học viên.
4. Học viên thấy khóa học trong trang cá nhân, mở bài và cập nhật tiến độ.
5. Giảng viên tạo câu hỏi/bài kiểm tra và kích hoạt.
6. Học viên bắt đầu, tự lưu, nộp bài; không thể sửa sau khi nộp.
7. Hệ thống chấm câu khách quan; giảng viên xử lý hàng chờ tự luận nếu có.
8. Dashboard/báo cáo phản ánh đúng phạm vi và thời điểm đồng bộ.
9. Khi đạt điều kiện hoàn thành, chứng chỉ được cấp đúng một lần.
10. Kiểm tra notification và audit cho thao tác nhạy cảm.

## 4. Khả năng phục hồi

- Chạy `scripts/backup.sh` và xác nhận `SHA256SUMS` hợp lệ.
- Tạo dữ liệu thử sau backup.
- Chạy restore trên môi trường kiểm thử, không làm trực tiếp lần đầu trên production.
- Chạy lại smoke test và đối chiếu dữ liệu.

## 5. Trước production

- Tắt `LMSPILOT_SEED_DEMO` và development license.
- Đổi toàn bộ mật khẩu demo.
- Bật HTTPS rồi đặt `LMSPILOT_COOKIE_SECURE=true`.
- Chốt RPO/RTO, retention, dung lượng file và số người dùng đồng thời.
- Chạy vulnerability scan, test tải, pentest và UAT có biên bản.
