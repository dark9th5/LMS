# Bảng phân công service LMSPilot 0.21.0

Điền tên owner/reviewer và mục tiêu sprint trước khi bắt đầu.

| Port | Service | Owner | Reviewer | Hạng mục nâng cấp | Hạng mục test | Trạng thái |
|---:|---|---|---|---|---|---|
| 8080 | `api-gateway` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8081 | `identity-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8082 | `organization-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8083 | `course-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8084 | `enrollment-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8085 | `learning-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8086 | `assessment-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8087 | `grading-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8088 | `reporting-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8089 | `file-storage-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8090 | `license-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8091 | `audit-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8092 | `notification-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8093 | `certificate-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8094 | `ai-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8095 | `configuration-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8096 | `integration-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8097 | `operations-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |
| 8098 | `competency-service` | Chưa phân công | Chưa phân công | — | Unit + API + DB migration + contract | Chưa nhận |

## Checklist khi nhận service

1. Đọc `backend/services/<service>/README.md`.
2. Chạy test riêng của service và kiểm tra migration trên database sạch.
3. Xác định API producer/consumer liên quan.
4. Tạo branch `service/<service>/<noi-dung>`.
5. Cập nhật tài liệu API/DB khi thay đổi contract.
6. Yêu cầu reviewer của service và reviewer của consumer nếu có thay đổi liên service.
