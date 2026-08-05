# Kiến trúc LMSPilot

LMSPilot dùng mô hình **microservice monorepo**. Mỗi backend service là một Spring Boot application Java 21 có port, API boundary, schema PostgreSQL, Flyway migration và README riêng.

## Nguyên tắc

- Frontend chỉ gọi API Gateway.
- Service không truy cập trực tiếp bảng của service khác.
- Internal API yêu cầu `X-Service-Token`.
- Event dùng contract trong `backend/platform-contracts` hoặc `contracts/lmspilot`.
- Mỗi tài khoản có đúng một vai trò `ADMIN`, `INSTRUCTOR` hoặc `STUDENT`.
- Bài kiểm tra thuộc khóa học; bài thi độc lập không có `courseId`.
- AI chỉ sinh bản nháp từ phiên bản PDF/DOCX thuộc khóa học và phải qua review.

## Database

Môi trường mặc định dùng một PostgreSQL instance với schema riêng cho từng service. Đây là tách biệt logic; có thể chuyển từng schema sang database instance riêng khi cần mở rộng mà không thay đổi ownership.

## Build

Mã backend là Java. `build.gradle.kts` là Gradle Kotlin DSL, không phải mã ứng dụng Kotlin.
