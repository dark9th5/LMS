# Hướng dẫn vận hành LMSPilot

## Cài đặt trong LAN

1. Cài Docker Engine và Docker Compose trên máy chủ Linux nội bộ.
2. Sao chép repository/release bundle lên máy chủ, không kèm dữ liệu khách hàng.
3. Tạo `.env` từ `.env.example`, thay JWT secret, internal token, mật khẩu quản trị và mật khẩu hạ tầng.
4. Giữ `LMSPILOT_COOKIE_SECURE=false` khi chạy HTTP trong LAN. Chỉ chuyển thành `true` sau khi đã cấu hình HTTPS.
5. Chạy `docker compose up -d --build`.
6. Kiểm tra `http://<IP-máy-chủ>:8080/actuator/health` và đăng nhập tại `http://<IP-máy-chủ>:3000`.
7. Khi dùng seed demo, đổi mật khẩu tạm ngay sau lần đăng nhập đầu tiên. Khi triển khai thật, đặt `LMSPILOT_SEED_DEMO=false`.

## Thành phần mặc định

Stack mặc định gồm web, API Gateway, PostgreSQL, RabbitMQ, Redis và các service cốt lõi: tài khoản, tổ chức, khóa học, lớp/ghi danh, học tập, bài thi, chấm điểm, báo cáo, file, license, audit, thông báo, chứng chỉ, cấu hình và vận hành.

AI local, adapter tích hợp, Prometheus và Grafana chỉ bật khi cần:

```bash
docker compose --profile extended --profile observability up -d --build
```

## Sao lưu

```bash
./scripts/backup.sh
```

Script tạo thư mục theo thời gian, sao lưu PostgreSQL, định nghĩa RabbitMQ, file lưu trữ, cấu hình và license; sau đó ghi manifest checksum. Chính sách lịch chạy, mã hóa, nơi giữ bản sao và thời gian lưu vẫn phải chốt theo TBD của BA.

## Phục hồi

1. Đưa hệ thống vào chế độ bảo trì.
2. Chạy `./scripts/restore.sh backups/<timestamp>`.
3. Khởi động lại PostgreSQL, RabbitMQ, Redis, Identity/Gateway rồi các service nghiệp vụ.
4. Kiểm tra health, đăng nhập, dữ liệu ghi danh, tiến độ và file trước khi mở ghi dữ liệu.

## Cập nhật và rollback

- Luôn backup trước khi cập nhật.
- Nạp image/release đã kiểm tra vào hạ tầng nội bộ.
- Chạy migration tương thích trước khi chuyển traffic.
- Cập nhật theo nhóm nhỏ và kiểm tra health sau mỗi nhóm.
- Khi lỗi, quay lại image tag trước và dùng bản backup tương thích; không tự sửa trực tiếp database của service khác.
