# Trạng thái bàn giao — LMSPilot 0.20.4

Bản 0.20.4 là **full-source release candidate** và giữ nguyên nghiệp vụ của 0.20.3. Phạm vi thay đổi tập trung vào tài liệu dự án và khả năng phân công microservice:

- Chuẩn hóa tên sản phẩm thành **LMSPilot**.
- Xóa nhãn cũ khỏi tên tài liệu, contract, cấu hình triển khai và namespace nội bộ liên quan.
- Viết lại README với chức năng, vai trò, kiến trúc, service, port, API và database.
- Thêm `docs/SERVICE_CATALOG.md`, `docs/API_DATABASE_MAP.md`, `docs/TEAM_SERVICE_ASSIGNMENT.md` và `docs/ARCHITECTURE.md`.
- Cập nhật README riêng cho đủ 19 backend service.
- Chuyển tài liệu phát hành và giao diện cũ vào `docs/archive` để thư mục gốc gọn hơn.

Các validator, TypeScript syntax và Python regression suite đã đạt. Full Gradle/Next build chưa chạy được do môi trường đóng gói không tải được dependency. Chi tiết tại `TEST_RESULTS_LMSPILOT_0.20.4.md`.
