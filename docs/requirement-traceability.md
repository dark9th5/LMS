# Requirement traceability

Nguồn hiện hành:

- `docs/reference/URD_LMSPilot_0.1_2026-07-29.docx` là tài liệu BA/URD do khách hàng cung cấp.
- `HUONG_DAN_DU_AN_LMSPILOT.docx` là tài liệu hướng dẫn tổng quan và vận hành hiện hành.

Khi có xung đột, yêu cầu bổ sung đã được người dùng xác nhận về account type, role tùy chỉnh/scoped RBAC, standalone exam, competition/reward, AI provider, document editing, news, branding và dịch vụ tùy chọn được dùng làm phần mở rộng ưu tiên cao hơn baseline URD.

## Giao diện & Kiến trúc 0.17

- Design system hiện hành áp dụng chuẩn WCAG, login/input, bố cục học/thi, Core Admin và structured input.
- Hệ thống hoạt động theo mô hình Permission-First RBAC trên 19 microservices.

