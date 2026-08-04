# API catalogue — LMSPilot CLS 0.15.0

Các API nghiệp vụ đi qua API Gateway. Route không ghi `public` đều cần JWT và được service sở hữu dữ liệu kiểm tra quyền/phạm vi lần nữa.

| Nhóm | Endpoint chính |
|---|---|
| Xác thực/phiên | `POST /api/v1/auth/login`, `/refresh`, `/logout`, `/change-password`, `GET /api/v1/auth/me`, session revoke |
| Người dùng/import/role | `/api/v1/users`, `/api/v1/users/import/*`, `/api/v1/roles`, `/api/v1/authorization/*` |
| Cơ cấu tổ chức | `/api/v1/organization/**` |
| Khóa học/nội dung/discussion | `/api/v1/categories`, `/api/v1/courses/**`, `/api/v1/discussions/**` |
| Lớp/ghi danh/lớp trực tuyến | `/api/v1/classes/**`, `/api/v1/enrollments/**`, `/api/v1/course-assignments/**`, `/api/v1/live-sessions/**` |
| Lộ trình học tập | `/api/v1/learning-paths`, `/{id}/publish`, `/{id}/assignments`, `/{id}/participants`, `/me/assigned` |
| Học tập/assignment/xAPI | `/api/v1/learning/**`, `/api/v1/xapi/**` |
| Ngân hàng câu hỏi/thi/cuộc thi | `/api/v1/questions/**`, `/api/v1/exams/**`, `/api/v1/exam-sessions/**`, `/api/v1/competitions/**` |
| Chấm điểm/phúc khảo | `/api/v1/grades/**`, `/api/v1/grading/**` |
| Báo cáo/KPI | `/api/v1/reports/**`, `/api/v1/reports/kpis`, `/api/v1/reports/kpis/courses`, report schedules/exports |
| Tệp/tài liệu | `/api/v1/files/**`, `/public/v1/file-edit/**` |
| License | `/api/v1/license/**` |
| Audit | `/api/v1/audit/**` |
| Thông báo/tin tức | `/api/v1/notifications/**`, `/api/v1/notifications/templates`, `/api/v1/notifications/reminder-rules`, `/api/v1/news/**` |
| Chứng chỉ | `/api/v1/certificates/**`, `/public/v1/certificates/**` |
| AI | `/api/v1/ai/**` |
| Branding/cấu hình/dịch vụ ngoài | `/api/v1/configuration/**`, `/api/v1/branding/**`, `/api/v1/external-services/**`, `/public/v1/branding/**` |
| Tích hợp | `/api/v1/integrations/**` |
| Vận hành | `/api/v1/operations/health`, `/api/v1/operations/jobs`, `/api/v1/operations/schedules` |
| Năng lực | `/api/v1/competencies/**` |

## Header dùng chung

- `Authorization: Bearer <JWT>` cho route bảo vệ.
- `X-Correlation-ID` được tiếp nhận/sinh tại Gateway và truyền xuyên service.
- `Idempotency-Key` hoặc `operationId` dùng ở các luồng retry có nguy cơ tạo trùng.
- `X-Service-Token` bảo vệ internal API; các route này không được client gọi trực tiếp.

## Internal API cốt lõi

- Course publication/learning metadata và course-version lookup.
- Enrollment/class/user-course/assigned-class lookup.
- Learning summaries theo user cho Learning Path.
- Assessment grading payload/manageable exam lookup.
- Identity email/profile resolution và scoped authorization.
- Reporting due-learning lookup cho reminder scheduler.
- License entitlement/capacity checks.
- Operations agent claim/heartbeat/complete.

Repository validator báo lỗi nếu internal controller không gọi `InternalTokenAuthorizer`.

Branding request/response có `themeKey`, được giới hạn trong allowlist 10 preset hiện hành. Flyway V5 đổi catalog 0.12; V6 thay `enterprise-blue` bằng `soft-spectrum`, đổi default và chỉ chuyển palette nếu khớp default cũ đã biết. `PUT /api/v1/branding` yêu cầu quyền quản trị; `GET /public/v1/branding` chỉ công khai dữ liệu nhận diện cần cho SSR, không trả secret tích hợp.

Request/response contract đặt cạnh controller của service sở hữu domain; contract sự kiện/quyền dùng chung nằm trong `backend/platform-contracts`.

## Authorization 0.15

- `GET /api/v1/authorization/catalog`: permission definitions, groups, risk, allowed scopes và access profiles.
- `POST /api/v1/authorization/grants/preview`: dry-run cấp quyền hàng loạt.
- `POST /api/v1/authorization/grants/bulk`: ghi assignment idempotent, bỏ qua bản ghi trùng.
- `DELETE /api/v1/authorization/grants/bulk`: thu hồi permission grant/role assignment theo ID.
- `GET /api/v1/authorization/effective`: permission ALLOW/DENY hiệu lực tại scope.
- `GET /api/v1/authorization/explain`: hiệu lực và mọi nguồn quyền.
- `GET /api/v1/authorization/users/{userId}/assignments`: các assignment đang lưu.

JWT 0.15 có `permissions` cho coarse gate và `globalPermissions` cho thao tác `SYSTEM`. Client không được tự suy luận quyền exact-resource chỉ từ JWT.
