# Kiến trúc LMSPilot

## Nguyên tắc

- Mỗi microservice sở hữu API và schema dữ liệu riêng.
- Frontend chỉ đi qua API Gateway; không hard-code URL nội bộ.
- Giao tiếp liên service qua HTTP nội bộ hoặc RabbitMQ, không truy cập chéo database.
- PostgreSQL mặc định dùng database `lmspilot`; mỗi service dùng một schema riêng.
- API công khai yêu cầu JWT; API `/internal/v1/**` yêu cầu service token và không dành cho trình duyệt.
- Mỗi tài khoản chỉ có một vai trò `ADMIN`, `INSTRUCTOR` hoặc `STUDENT`.
- Bài kiểm tra thuộc khóa học; bài thi là nghiệp vụ độc lập.

## Luồng chính

### Đăng nhập

`Web → API Gateway → Identity Service → JWT/refresh token`.

### Học khóa học

`Web → Gateway → Enrollment → Course → Learning → File Storage`.

### Làm bài kiểm tra hoặc bài thi

`Web → Gateway → Assessment → Grading → Reporting/Certificate`.

### Sinh câu hỏi bằng AI

`Instructor → AI Service → File Storage/Course → AI provider → quality validator → review → Assessment import`.

### Cấu hình thương hiệu

`Admin → Configuration Service → File Storage → public branding API → Login/Web`.

## Database

Một PostgreSQL database mặc định tên `lmspilot`, tách schema:

`identity`, `organization`, `course`, `enrollment`, `learning`, `assessment`, `grading`, `reporting`, `file_storage`, `license`, `audit`, `notification`, `certificate`, `ai`, `configuration`, `integration`, `operations`, `competency`.

API Gateway và frontend không sở hữu database.
