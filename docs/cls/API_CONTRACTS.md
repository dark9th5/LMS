# LMSPilot CLS — API contracts đang triển khai

Tài liệu này mô tả các endpoint hiện có trong source tree của bản CLS. Tất cả API quản trị đều yêu cầu JWT và permission tương ứng; các API `/internal/v1/**` yêu cầu internal service token và không dành cho trình duyệt.

## 1. Tài khoản, role và quyền theo phạm vi

### Tài khoản

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- `GET /api/v1/users`
- `GET /api/v1/users/{id}`
- `POST /api/v1/users`
- `POST /api/v1/users/bulk` — tạo nhiều tài khoản, hỗ trợ `operationId` để chống chạy trùng.
- `PUT /api/v1/users/{id}`
- `POST /api/v1/users/{id}/reset-password`

Hệ thống không có API đăng ký công khai. Tài khoản chỉ có hai loại: `SYSTEM_ADMIN` và `USER`. Các chức năng Admin/Giảng viên/Học viên là role RBAC có thể gán đồng thời cho một người dùng.

### Role và authorization

- `GET /api/v1/roles`
- `POST /api/v1/roles`
- `PUT /api/v1/roles/{id}`
- `GET /api/v1/authorization/catalog`
- `GET /api/v1/authorization/users/{userId}/assignments`
- `POST /api/v1/authorization/grants/bulk`
- `DELETE /api/v1/authorization/grants/bulk`
- `GET /api/v1/authorization/effective?userId=&resourceType=&resourceId=`

Ví dụ cấp đồng thời role và permission cho nhiều tài khoản:

```json
{
  "operationId": "grant-2026-08-03-01",
  "userIds": [
    "11111111-1111-1111-1111-111111111111",
    "22222222-2222-2222-2222-222222222222"
  ],
  "grants": [
    {
      "roleCode": "LEARNER",
      "scopeType": "COURSE",
      "scopeId": "33333333-3333-3333-3333-333333333333",
      "effect": "ALLOW"
    },
    {
      "permissionCode": "reports:read:scope",
      "scopeType": "DEPARTMENT",
      "scopeId": "44444444-4444-4444-4444-444444444444",
      "effect": "ALLOW"
    }
  ]
}
```

`DENY` có độ ưu tiên cao hơn `ALLOW`. Tài khoản bootstrap `SYSTEM_ADMIN` được đánh dấu bảo vệ và không thể bị khóa, đổi loại hoặc hạ quyền qua API quản trị thông thường.

## 2. Cơ cấu tổ chức

- `GET /api/v1/organization/units`
- `GET /api/v1/organization/units/tree`
- `POST /api/v1/organization/units`
- `PUT /api/v1/organization/units/{id}`
- `POST /api/v1/organization/units/{id}/deactivate`
- `GET /api/v1/organization/memberships`
- `POST /api/v1/organization/memberships/bulk`
- `DELETE /api/v1/organization/memberships/bulk`

Đơn vị hỗ trợ cây chi nhánh, phòng ban và nhóm. API membership kiểm tra scope của từng đơn vị trước khi thêm hoặc xóa thành viên.

## 3. Khóa học, lớp, giao học và học trực tuyến

### Khóa học

- `GET /api/v1/categories`
- `POST /api/v1/categories`
- `GET /api/v1/courses`
- `GET /api/v1/courses/{id}`
- `POST /api/v1/courses`
- `PUT /api/v1/courses/{id}`
- `DELETE /api/v1/courses/{id}`
- `POST /api/v1/courses/{id}/status/{status}`
- `POST /api/v1/courses/{id}/lessons`
- `PUT /api/v1/courses/{courseId}/lessons/{lessonId}`
- `DELETE /api/v1/courses/{courseId}/lessons/{lessonId}`

### Lớp, ghi danh và giao khóa học

- `GET /api/v1/classes`
- `GET /api/v1/classes/{id}`
- `POST /api/v1/classes`
- `POST /api/v1/classes/{id}/enrollments`
- `GET /api/v1/classes/{id}/enrollments`
- `GET /api/v1/enrollments/me`
- `POST /api/v1/course-assignments`
- `GET /api/v1/course-assignments`
- `GET /api/v1/course-assignments/me`

Course assignment hỗ trợ đích `USER`, `GROUP`, `DEPARTMENT`, `BRANCH`, thời điểm bắt đầu, hạn hoàn thành, grace period và bắt buộc/tùy chọn.

### Lớp trực tuyến

- `POST /api/v1/live-sessions`
- `GET /api/v1/live-sessions`
- `GET /api/v1/live-sessions/me`

## 4. Ngân hàng câu hỏi, bài kiểm tra, bài thi và cuộc thi

### Ngân hàng câu hỏi và bài thi

- `GET /api/v1/questions`
- `POST /api/v1/questions`
- `PUT /api/v1/questions/{id}`
- `DELETE /api/v1/questions/{id}`
- `GET /api/v1/exams`
- `GET /api/v1/exams/{id}`
- `POST /api/v1/exams`
- `PUT /api/v1/exams/{id}`
- `DELETE /api/v1/exams/{id}`
- `POST /api/v1/exams/start`
- `PUT /api/v1/exam-sessions/{id}/answers`
- `POST /api/v1/exam-sessions/{id}/submit`

`contextType` phân biệt quiz/assignment trong khóa học với standalone exam và competition. Câu hỏi khách quan được chấm tự động; tự luận đi vào hàng chờ chấm.

### Giao bài thi độc lập

- `POST /api/v1/assessment-assignments`
- `GET /api/v1/assessment-assignments?assessmentId=`
- `DELETE /api/v1/assessment-assignments/{id}`

Assignment hỗ trợ `USER`, `GROUP`, `DEPARTMENT`, `BRANCH`, thời gian hiệu lực và trạng thái bắt buộc. Khi bài thi chưa có assignment đang hoạt động, người có quyền tham gia vẫn có thể truy cập; sau khi có assignment, chỉ đúng đối tượng được giao mới nhìn thấy và bắt đầu làm.

### Cuộc thi, bảng xếp hạng và thưởng

- `GET /api/v1/competitions`
- `GET /api/v1/competitions/{id}`
- `POST /api/v1/competitions`
- `PUT /api/v1/competitions/{id}`
- `GET /api/v1/competitions/{id}/leaderboard`
- `POST /api/v1/competitions/{id}/publish`
- `POST /api/v1/competitions/{id}/rewards/issue`
- `GET /api/v1/competitions/{id}/rewards/ledger`

Thứ hạng được sắp theo điểm giảm dần, thời gian làm tăng dần, thời điểm nộp và cuối cùng là user ID để bảo đảm tính xác định. Reward ledger có khóa chống phát thưởng trùng.

## 5. Branding và dịch vụ bên thứ ba

- `GET /public/v1/branding`
- `GET /public/v1/branding/assets/{kind}`
- `GET /api/v1/branding`
- `PUT /api/v1/branding`
- `GET /api/v1/external-services`
- `POST /api/v1/external-services`
- `PUT /api/v1/external-services/{id}`
- `POST /api/v1/external-services/{id}/test`

Branding gồm tên hệ thống, nội dung giới thiệu, logo, favicon, ảnh nền, màu chính/phụ, màu nền và màu chữ và custom domain. Secret của tích hợp được mã hóa bằng `CONFIGURATION_SECRET_KEY` và không được trả ngược dưới dạng rõ.

## 6. Tin tức

- `GET /api/v1/news/feed`
- `GET /api/v1/news`
- `POST /api/v1/news`
- `PUT /api/v1/news/{id}`
- `POST /api/v1/news/{id}/publish`
- `POST /api/v1/news/{id}/archive`
- `PUT /api/v1/news/{id}/read`
- `PUT /api/v1/news/{id}/acknowledge`

Tin có thể phát toàn hệ thống hoặc theo phạm vi tổ chức, được ghim, đặt thời gian hiển thị và yêu cầu người đọc xác nhận.

## 7. AI sinh bộ câu hỏi

- `GET /api/v1/ai/providers`
- `POST /api/v1/ai/providers`
- `PUT /api/v1/ai/providers/{id}`
- `GET /api/v1/ai/question-generation-jobs`
- `GET /api/v1/ai/question-generation-jobs/{id}`
- `POST /api/v1/ai/question-generation-jobs`
- `POST /api/v1/ai/question-generation-jobs/{id}/review`
- `POST /api/v1/ai/question-generation-jobs/{id}/import`

Provider hỗ trợ local endpoint hoặc API bên ngoài do khách hàng cấu hình. API key được mã hóa bằng `AI_SECRET_KEY`. Kết quả phải tuân theo `contracts/cls/question-set.schema.json`, qua schema validation, business validation và bước review trước khi import vào ngân hàng câu hỏi.

## 8. File, phiên bản và chỉnh sửa tài liệu

- `GET /api/v1/files`
- `POST /api/v1/files`
- `GET /api/v1/files/{id}`
- `GET /api/v1/files/{id}/content`
- `DELETE /api/v1/files/{id}`
- `GET /api/v1/files/{id}/versions`
- `POST /api/v1/files/{id}/edit-sessions`
- `POST /api/v1/files/edit-sessions/{id}/pdf`
- `DELETE /api/v1/files/edit-sessions/{id}`
- `GET /public/v1/file-edit/{id}/content`
- `POST /public/v1/file-edit/{id}/callback`

DOCX sử dụng edit session/callback tương thích document editor như OnlyOffice. PDF hỗ trợ upload bản chỉnh sửa hoặc annotation thành phiên bản mới. Tất cả thao tác tạo phiên bản đều yêu cầu quyền file editing và giữ lịch sử phiên bản.
