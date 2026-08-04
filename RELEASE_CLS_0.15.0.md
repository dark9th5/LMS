# LMSPilot CLS 0.15.0 — Permission-first release candidate

0.15.0 chuyển mô hình truy cập từ ba role cứng sang hai account type và các gói quyền có thể ghép. Người dùng có thể đồng thời học, biên soạn, chấm điểm, quản lý lớp hoặc kỳ thi theo đúng phạm vi được cấp.

## Điểm chính

- 93 permission có metadata tiếng Việt, risk và allowed scope.
- 10 gói quyền hệ thống theo công việc, tự bootstrap và read-only.
- Bulk grant có dry-run, cảnh báo CRITICAL, phát hiện trùng, thời hạn và scope filtering.
- Màn hình explain/revoke cho biết quyền đến từ nguồn nào.
- Backend service không còn role-name gating.
- JWT tách `permissions` và `globalPermissions`.
- FE sidebar, lớp, ghi danh, import và quản trị quyền dùng permission thực tế.

## Tương thích

Schema database không đổi. Các role cũ có thể còn trong dữ liệu để hỗ trợ nâng cấp, nhưng luồng mới không sử dụng tên `ADMIN/INSTRUCTOR/LEARNER` làm điều kiện truy cập. Token cũ không có `globalPermissions`; cần đăng nhập lại sau nâng cấp.

## Trạng thái

Đây là full-source release candidate, chưa phải chứng nhận production. 127 test regression đạt; platform-contracts compile đạt; TypeScript syntax parse đạt. Full npm/Gradle/Docker verification chưa chạy được trong môi trường đóng gói. Xem `docs/PERMISSION_FIRST_0.15.0.md`, `TEST_RESULTS_CLS_0.15.0.md` và `DELIVERY_STATUS.md`.
