# Trạng thái bàn giao – LMSPilot 0.4.0

## Mục tiêu của bản 0.4.0

Bản này tập trung sửa đúng các vấn đề của repository GitHub: giao diện còn mang tính minh họa, điều hướng sai vai trò, không mở được khóa học, chữ nhỏ, thanh cuộn thừa và quá nhiều nội dung bị dồn vào một màn hình. Phạm vi nghiệp vụ vẫn giữ đúng tài liệu BA, không bổ sung module ngoài lõi LMS.

## Đã hoàn thiện trong mã nguồn

- Sửa luồng đăng nhập theo vai trò: Quản trị viên và Giảng viên vào Dashboard; Học viên vào Học tập. Phản hồi đăng nhập trả đúng user hiện tại, cookie áp dụng toàn site và điều hướng toàn trang để không tái sử dụng giao diện của phiên trước.
- Thiết kế lại portal nhiều trang với menu riêng theo vai trò, dashboard, danh sách và trang chi tiết tách biệt.
- Tăng cỡ chữ, khoảng cách, vùng bấm và độ tương phản; bổ sung responsive cho desktop, tablet và mobile.
- Ẩn thanh cuộn dọc/ngang ở sidebar và mục lục bài học nhưng vẫn giữ thao tác cuộn bằng chuột, touchpad và bàn phím.
- Khóa học dùng API thật: danh sách, tìm kiếm, tạo, mở chi tiết, sửa thông tin, thêm/sửa bài học, đính kèm tệp, xuất bản, tạm ẩn và lưu trữ.
- PDF, video và audio có thể xem/phát trực tiếp qua File Storage Service; các loại tệp khác được tải xuống theo quyền.
- Lớp học dùng API thật: tạo lớp, mở chi tiết, tải danh sách học viên và ghi danh có idempotency.
- Học tập dùng dữ liệu ghi danh/progress thật: mở khóa học, điều hướng bài, lưu vị trí, đánh dấu hoàn thành và nộp tệp thực hành.
- Kiểm tra dùng API thật: tạo câu hỏi, tạo bài thi, bắt đầu phiên, tự lưu đáp án, đồng hồ, nộp idempotent và hiển thị kết quả.
- Hàng chờ chấm hỗ trợ nhập điểm và nhận xét thủ công.
- Smoke test kiểm tra cả API lẫn web BFF cho ba tài khoản mẫu và xác nhận đúng trang đích theo vai trò.
- Giữ các gia cố của 0.3.0: secret runtime, upload allow-list/SHA-256, CSV an toàn, service token, migration không phá dữ liệu, backup/restore script và giới hạn log.

## Kiểm tra đã chạy thành công trong môi trường tạo bản

- `python scripts/validate-repository.py`
- `node scripts/check-typescript.js`
- Kiểm tra semantic TypeScript bằng `tsc` cục bộ với stub framework ngoại tuyến.
- `bash -n` cho toàn bộ shell script.
- `python -m unittest discover -s tests -p 'test_*.py' -v`: **14/14 test đạt**.
- `bash scripts/test-static.sh`: toàn bộ static/contract suite đạt.
- `git diff --check` và `git fsck --full`.
- Kiểm tra ZIP sau đóng gói: giải nén lại và chạy lại static test trên bản giải nén.

## Chưa thể xác nhận trong môi trường tạo bản

Môi trường tạo bản không có Docker Engine và không truy cập được Maven/npm registry, nên chưa thể chạy tại đây:

- `docker compose up --build` với toàn bộ container.
- Gradle compile/test đầy đủ bằng dependency tải từ Maven.
- `npm ci` và Next.js production build bằng dependency tải từ npm.
- E2E trình duyệt trên stack PostgreSQL/RabbitMQ/Redis thật.
- Kiểm thử tải, pentest và UAT trên hạ tầng khách hàng.

Các bước này được chuyển thành setup/smoke test trên máy đích. Chỉ coi cài đặt thành công khi `scripts/setup.sh` hoặc `scripts/setup.ps1` kết thúc bằng **`SMOKE TEST PASSED`**.

## Phần BA vẫn cần chốt trước production

- Số người dùng/phiên thi đồng thời và SLA.
- RPO/RTO, lịch sao lưu, retention và mã hóa backup.
- Chính sách sửa phiên bản khóa học đã xuất bản.
- Mapping LDAP/AD, HRM/ERP, NAS và SMTP thực tế.
- Mẫu chứng chỉ chính thức, QR/chữ ký và chính sách nộp lại bài thực hành.

## Đánh giá

Bản 0.4.0 phù hợp để import vào repository, chạy demo/UAT và trình bày luồng thật cho quản lý. Không nên gọi là đã được chứng nhận production trên mọi máy trước khi setup, smoke test và UAT thành công ở môi trường đích.
