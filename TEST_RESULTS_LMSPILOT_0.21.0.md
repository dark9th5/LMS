# Kết quả kiểm tra LMSPilot 0.21.0

Ngày đóng gói: 2026-08-05.

## Phạm vi chuyển đổi

- Backend runtime: **Java 21 + Spring Boot 3.5.16**.
- Module đã chuyển: `platform-contracts`, `service-support` và 19 microservice.
- Số tệp Java backend: **443**.
- Số tệp Kotlin backend còn lại: **0**.
- Số lớp khởi động Spring Boot: **19**.
- Port backend: **8080–8098**, không trùng nhau.

## Kết quả đã chạy

| Kiểm tra | Kết quả |
|---|---|
| `python scripts/validate-repository.py` | PASS — 10 JSON, 28 YAML, 19 service, 18 service có Flyway, 19 port duy nhất, API wiring và Docker layout hợp lệ |
| `python -m unittest discover -s tests -p "test_*.py"` | PASS — 166 test: 132 đạt, 34 skip có chủ ý |
| TypeScript/TSX syntax scan | PASS — 89 tệp, 0 lỗi cú pháp |
| CSS parse (`globals.css`, `unified.css`) | PASS — 0 lỗi parse cấp cao nhất |
| Java source syntax/public-type scan | PASS — 443 tệp; 0 lỗi cú pháp hoặc tên public type/file |
| `platform-contracts` Java main compile | PASS với stub tối thiểu cho `JsonNode`, dùng để kiểm tra phần Java tự thân khi dependency ngoài chưa tải được |
| Kiểm tra tên sản phẩm đang hoạt động | PASS — không còn tên sản phẩm và chữ viết tắt cũ trong mã nguồn hoặc tài liệu hiện hành |
| Kiểm tra Kotlin backend | PASS — không còn tệp `.kt` trong `backend/` |

## Giới hạn của môi trường đóng gói

Lệnh sau đã được thực hiện:

```bash
cd backend
./gradlew test --no-daemon
```

Gradle Wrapper không thể tải Gradle 8.14.5 vì môi trường đóng gói không phân giải được `services.gradle.org` (`UnknownHostException`). Vì vậy, **chưa xác nhận full semantic compilation và toàn bộ JUnit/Spring context test bằng Gradle** trong môi trường này.

Các bước bắt buộc trên GitHub Actions hoặc máy có Internet trước khi merge/triển khai:

```bash
cd backend
./gradlew clean test
./gradlew bootJar

cd ../apps/web
npm ci
npm run typecheck
npm run build

cd ../
docker compose config
docker compose up --build
```

Sau khi hệ thống khởi động, cần chạy API integration test, migration test trên PostgreSQL sạch và E2E cho ba vai trò `ADMIN`, `INSTRUCTOR`, `STUDENT`.
