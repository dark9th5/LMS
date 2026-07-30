# LMSPilot Platform 0.4.0

LMSPilot là hệ thống quản lý học tập On-Premise chạy trong mạng LAN. Repository giữ đúng ba nhóm người dùng chính của tài liệu BA: **Quản trị viên, Giảng viên và Học viên**, cùng luồng cốt lõi **tài khoản → khóa học → lớp/ghi danh → học tập → kiểm tra/chấm điểm → hoàn thành → chứng chỉ → báo cáo**.

## Những thay đổi giao diện và luồng thật ở 0.4.0

- Sửa dứt điểm điều hướng sau đăng nhập: Admin/Giảng viên vào Dashboard, Học viên vào trang Học tập; dùng full navigation để không tái sử dụng giao diện của phiên trước.
- Thay trang khóa học dạng bảng/demo bằng thẻ khóa học và trang chi tiết riêng. Có tạo khóa học, thêm/sửa bài học văn bản hoặc tệp, cập nhật thông tin và chuyển trạng thái xuất bản.
- Thêm trang lớp riêng, xem danh sách ghi danh và ghi danh học viên qua Enrollment Service.
- Thêm trình học riêng: mục lục không hiện thanh cuộn, xem PDF/phát video hoặc audio trực tiếp, tải tài liệu, nộp tệp bài thực hành và cập nhật tiến độ thật.
- Thêm luồng thi hoàn chỉnh: bắt đầu phiên, điều hướng câu hỏi, tự lưu 20 giây, nộp bài idempotent, tự nộp khi hết giờ và xem trạng thái kết quả.
- Thêm hàng chờ chấm tự luận cho giảng viên/quản trị viên.
- Thiết kế lại toàn bộ typography, khoảng cách, responsive, dashboard và thanh điều hướng; không dồn mọi nghiệp vụ vào một màn hình.

## Cách chạy dễ nhất

### Windows 10/11

Yêu cầu: Docker Desktop đang chạy và sử dụng Linux containers.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup.ps1
```

### Linux

Yêu cầu: Docker Engine và Docker Compose v2.

```bash
./scripts/setup.sh
```

Hai script trên sẽ:

1. Tạo `.env` và sinh secret/mật khẩu ngẫu nhiên nếu chưa tồn tại.
2. Kiểm tra Docker, cấu hình, dung lượng trống và các biến bắt buộc.
3. Build rồi khởi động hệ thống.
4. Chờ hệ thống sẵn sàng và chạy smoke test API/web bằng tài khoản Admin, Giảng viên và Học viên.

Sau khi thành công:

- Portal: `http://localhost:3000`
- API Gateway: `http://localhost:8080`
- RabbitMQ Management: `http://localhost:15672`

Mật khẩu tài khoản demo nằm trong biến `LMSPILOT_DEFAULT_ADMIN_PASSWORD` của file `.env`:

| Vai trò | Tên đăng nhập |
|---|---|
| Quản trị viên | `admin` |
| Giảng viên | `instructor` |
| Học viên | `student` |

> Lần build đầu cần Internet để tải Docker image và dependency. Sau khi image đã được build, hệ thống có thể vận hành trong LAN mà không phụ thuộc Internet.

## Kiểm tra trước khi chạy

```bash
./scripts/test-static.sh
./scripts/preflight.sh
```

Trên Windows:

```powershell
.\scripts\test-static.ps1
.\scripts\preflight.ps1
```

Kiểm tra trạng thái sau khi đã khởi động:

```bash
./scripts/smoke-test.sh
docker compose ps
docker compose logs --tail=200
```

## Các phần đã có trong mã nguồn

- Portal responsive dùng chung cho ba vai trò, điều hướng và trang bắt đầu tách đúng theo quyền.
- Đăng nhập local, access/refresh token, RBAC và kiểm tra quyền tại backend.
- Quản lý người dùng, vai trò, cơ cấu tổ chức; tạo/mở/chỉnh sửa/xuất bản khóa học; thêm/sửa bài học; tạo lớp và ghi danh thật qua API.
- Trình học riêng theo từng khóa học: mở bài học, xem PDF/video/audio trực tiếp, tải tài liệu, nộp tệp thực hành, lưu tiến độ và tiếp tục vị trí gần nhất.
- Ngân hàng câu hỏi, cấu hình bài kiểm tra, phiên thi có đồng hồ, tự lưu định kỳ, tự nộp khi hết giờ và chấm điểm thật.
- Báo cáo read-model, dashboard, CSV an toàn và giới hạn dữ liệu theo phạm vi.
- Thông báo trong ứng dụng, SMTP tùy chọn, chứng chỉ và mã xác minh trong LAN.
- File storage có giới hạn dung lượng, allow-list định dạng, kiểm tra tên tệp và SHA-256.
- License offline, audit, health check, backup/restore bằng script.
- AI local và adapter tích hợp được giữ ở profile tùy chọn, không làm nặng luồng lõi.

## Lệnh vận hành

```bash
make up             # chạy stack lõi
make up-all         # thêm AI/integration/observability
make down
make logs
make backup
```

Sao lưu và phục hồi thật được thực hiện tại máy chủ:

```bash
./scripts/backup.sh
./scripts/restore.sh backups/<thu-muc-backup>
```

API Operations chỉ ghi nhận yêu cầu và hiển thị health; nó không được cấp Docker socket để tránh tạo lỗ hổng điều khiển máy chủ.

## Cấu trúc repository

```text
apps/web/                 Next.js portal
backend/platform-*        contract và support dùng chung
backend/services/*        18 Spring/Kotlin service, gồm API Gateway
infrastructure/           PostgreSQL, Prometheus, Grafana
tests/                    kiểm tra contract của repository
scripts/                  setup, preflight, smoke test, backup/restore
docs/                     kiến trúc, API, traceability và runbook
```

## Phạm vi xác nhận

Bản 0.4.0 là **release candidate có bộ cài và kiểm tra tự động**, chưa phải chứng nhận production cho mọi khách hàng. Các chỉ tiêu tải đồng thời, SLA, RPO/RTO, retention, LDAP/AD và adapter HRM/ERP vẫn phải chốt theo môi trường triển khai cụ thể. Xem `DELIVERY_STATUS.md` và `docs/verification-checklist.md`.
