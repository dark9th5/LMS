# API catalogue (v1)

Các API nghiệp vụ đi qua API Gateway. Mọi API không ghi rõ là public đều cần JWT và tiếp tục được kiểm tra quyền tại service sở hữu dữ liệu.

| Nhóm | Endpoint chính |
|---|---|
| Xác thực | `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me` |
| Người dùng và vai trò | `/api/v1/users`, `/api/v1/roles` |
| Cơ cấu tổ chức | `/api/v1/organization` |
| Khóa học | `/api/v1/categories`, `/api/v1/courses`, `POST /api/v1/courses/{id}/status/{status}` |
| Lớp và ghi danh | `/api/v1/classes`, `POST /api/v1/classes/{id}/enrollments`, `/api/v1/enrollments/me` |
| Tiến độ học | `GET /api/v1/learning/me`, `GET /api/v1/learning/{enrollmentId}`, `PUT /api/v1/learning/progress` |
| Câu hỏi và bài thi | `/api/v1/questions`, `/api/v1/exams`, `/api/v1/exams/start`, `/api/v1/exam-sessions/{id}` |
| Chấm điểm | `/api/v1/grades/me`, `/api/v1/grades/queue`, `PUT /api/v1/grades/{id}` |
| Báo cáo | `/api/v1/reports/dashboard`, `/api/v1/reports/learning`, `/api/v1/reports/learning/export.csv` |
| Tệp | `POST /api/v1/files`, `GET /api/v1/files/{id}/content` |
| License | `GET /api/v1/license`, `POST /api/v1/license/activate` |
| Audit | `GET /api/v1/audit` |
| Thông báo | `GET /api/v1/notifications`, `PUT /api/v1/notifications/{id}/read` |
| Chứng chỉ | `GET /api/v1/certificates`, `/api/v1/certificates/me`, `/public/v1/certificates/{code}` |
| AI local | `/api/v1/ai/status`, `/api/v1/ai/question-drafts` |
| Cấu hình | `/api/v1/configuration`, `/public/v1/configuration` |
| Tích hợp | `/api/v1/integrations`, `/api/v1/integrations/{id}/test` |
| Vận hành | `/api/v1/operations/health`, `/api/v1/operations/jobs` |

## Header dùng chung

- `Authorization: Bearer <JWT>` cho route bảo vệ.
- `X-Correlation-ID` được tiếp nhận hoặc sinh tại Gateway và truyền xuyên service.
- `Idempotency-Key` bắt buộc ở các luồng retry có nguy cơ tạo trùng, như ghi danh, cập nhật tiến độ và nộp bài thi.
- `X-Service-Token` bảo vệ internal API cùng với cô lập mạng.

Request/response contract nằm cạnh controller của service sở hữu, tránh một contract trung tâm phình to và lệch nghiệp vụ.

## Internal API cốt lõi

Các route dưới đây không đi qua client. Chúng chỉ dùng trong mạng service và bắt buộc `X-Service-Token`:

- `GET /internal/v1/courses/{id}/publication` và `/learning-metadata`.
- `GET /internal/v1/enrollments/{id}`.
- `GET /internal/v1/classes/assigned/{userId}` và `/assigned/{userId}/courses`.
- `GET /internal/v1/classes/user/{userId}/courses`.
- `GET /internal/v1/assessment/sessions/{id}/grading-payload`.
- `GET /internal/v1/assessment/exams/manageable/{userId}`.

Validation repository sẽ báo lỗi nếu một file khai báo internal controller mà không gọi `InternalTokenAuthorizer`.
