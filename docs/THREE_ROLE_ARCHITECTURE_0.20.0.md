# Kiến trúc ba vai trò — LMSPilot 0.20.0

## 1. Quy tắc nền tảng

Hệ thống có đúng ba vai trò sản phẩm:

- `ADMIN` — quản trị nền tảng;
- `INSTRUCTOR` — giảng dạy và đánh giá;
- `STUDENT` — học tập và làm bài.

Database áp dụng unique index trên `user_roles(user_id)`. API tạo, sửa và import tài khoản chỉ nhận một mã role trong allowlist trên. Token và cookie web đều chứa `primaryRole` trùng với phần tử duy nhất trong `roles`.

## 2. Tách route và giao diện

| Role | Namespace | Menu |
|---|---|---|
| ADMIN | `/admin/*` | Tổng quan, Người dùng, Tổ chức, Báo cáo, Cài đặt |
| INSTRUCTOR | `/instructor/*` | Tổng quan, Khóa học, Bài thi, Chấm điểm, Báo cáo |
| STUDENT | `/student/*` | Tổng quan, Khóa học, Bài thi, Kết quả, Chứng chỉ |

Trang server gọi `requireRole`. Session role không phù hợp được chuyển về trang chủ của role hiện tại. Không có nút đổi role trong cùng phiên đăng nhập.

## 3. Ma trận sở hữu chức năng

### ADMIN

- tài khoản và role;
- cây tổ chức và thành viên;
- báo cáo quản trị;
- logo, tên thương hiệu, màu và ảnh nền đăng nhập;
- dịch vụ Redis, SMTP, AI, S3, ONLYOFFICE và họp trực tuyến;
- audit, license và operations.

ADMIN không nhận permission tạo khóa học, tạo đề, chấm điểm hoặc học bài.

### INSTRUCTOR

- tạo/sửa/xuất bản khóa học;
- thêm video, audio, PDF, DOCX, tệp và bài thực hành;
- gán học viên trực tiếp vào khóa học;
- tạo câu hỏi từ PDF/DOCX của khóa học;
- tạo bài kiểm tra thuộc khóa học;
- tạo và giao bài thi độc lập;
- chấm tự luận và bài thực hành;
- xem báo cáo trong phạm vi giảng dạy.

INSTRUCTOR không quản trị tài khoản, tổ chức hoặc thương hiệu.

### STUDENT

- xem khóa học được giao;
- học video/audio và đọc PDF/DOCX;
- tải tài liệu và nộp bài thực hành;
- làm bài kiểm tra trong khóa học;
- làm bài thi độc lập được giao;
- xem điểm, phản hồi và chứng chỉ.

STUDENT không thấy các thao tác tạo, sửa, xuất bản hoặc chấm điểm.

## 4. Mô hình khóa học

Không có route công khai `/classes` và không có `ClassesPage`. Gán người học qua `/api/v1/course-assignments`.

- `COURSE_QUIZ`: bắt buộc có `courseId` và `lessonId` của bài học loại `EXAM`.
- `STANDALONE_EXAM`: không được có `courseId`/`lessonId`; chỉ hiển thị ở mục Bài thi.
- AI chỉ nhận `documentFileIds` nằm trong `document-scope` của khóa học và có loại PDF/DOCX.

## 5. Bảo vệ tệp theo role

| Role | Purpose tải lên |
|---|---|
| ADMIN | `BRANDING_LOGO`, `BRANDING_BACKGROUND`, `NEWS_ATTACHMENT`, `GENERAL` |
| INSTRUCTOR | `COURSE_CONTENT`, `COURSE_DOCUMENT`, `QUESTION_SOURCE`, `GENERAL` |
| STUDENT | `ASSIGNMENT_SUBMISSION` |

Permission `files:upload` không đủ để vượt qua kiểm tra purpose.

## 6. Tương thích dữ liệu cũ

Schema cũ có `training_classes` và `class_id`. 0.20 dùng chúng như bản ghi phân phối nội bộ để ghim version khóa học và giữ lịch sử enrollment. Không có public controller, sidebar, route hoặc form Lớp học. Việc đổi tên vật lý bảng/column được hoãn để tránh migration phá dữ liệu khách hàng.
