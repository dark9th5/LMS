# Phân tích và tái thiết kế LMS theo hướng CLS

## 1. Kết luận rà soát

Repository hiện tại đã có nền tảng tốt cho một CLS on-premise: web Next.js, API gateway, nhiều service Spring/Kotlin, RBAC, tổ chức, khóa học, ghi danh, tiến độ, bài đánh giá, báo cáo, thông báo, file storage, AI và integration. Vì vậy không nên viết lại toàn bộ. Hướng an toàn là mở rộng domain model và chuẩn hóa authorization.

Vấn đề lớn nhất không nằm ở số lượng màn hình mà ở ba điểm:

1. Vai trò đang được dùng đồng thời như chức danh nghiệp vụ và cơ chế phân quyền.
2. Phạm vi quyền chưa được mô hình hóa thống nhất giữa đơn vị tổ chức, khóa học và kỳ thi.
3. Khóa học, lớp học, bài kiểm tra, kỳ thi và cuộc thi chưa có một taxonomy rõ ràng.

## 2. Giải quyết yêu cầu vai trò có vẻ mâu thuẫn

Yêu cầu vừa nói chỉ còn “quản trị/người dùng”, vừa yêu cầu ba role mặc định và role tùy chỉnh. Thiết kế đích tách hai khái niệm:

### Account type

- `SYSTEM_ADMIN`: tài khoản quản trị gốc, cố định, được bootstrap khi cài đặt.
- `USER`: mọi tài khoản còn lại.

### Role/permission

- Role mặc định: `ADMIN`, `INSTRUCTOR`, `LEARNER`.
- Admin được tạo role mới.
- Một user có thể có nhiều role.
- Ngoài role, Admin có thể cấp quyền trực tiếp cho nhiều user.
- Role và quyền đều có scope.

Như vậy “Giảng viên/Học viên” không còn là loại tài khoản cứng. Một người có thể vừa học khóa A, vừa dạy khóa B, vừa quản lý thi ở phòng ban C.

## 3. Mô hình authorization

### Permission grant

Mỗi grant gồm:

- `principal_type`: USER hoặc ROLE.
- `principal_id`.
- `permission_code`.
- `scope_type`: SYSTEM, BRANCH, DEPARTMENT, GROUP, COURSE, EXAM.
- `scope_id`: null với SYSTEM, UUID với các scope còn lại.
- `effect`: ALLOW hoặc DENY.
- `valid_from`, `valid_until`.

Quy tắc đánh giá:

1. `SYSTEM_ADMIN` luôn được phép, trừ thao tác phá hủy tài khoản bootstrap.
2. DENY cụ thể thắng ALLOW cùng hoặc rộng hơn.
3. Grant trực tiếp cho user thắng grant từ role khi cùng độ cụ thể.
4. Scope con kế thừa từ scope cha của tổ chức.
5. Mọi truy vấn danh sách phải lọc theo scope ở tầng service/repository, không chỉ ẩn nút ở frontend.

### Bulk assignment

Admin có thể chọn nhiều user rồi:

- gán/bỏ role;
- cấp/thu hồi permission;
- gán vào chi nhánh, phòng ban, nhóm;
- giao khóa học hoặc kỳ thi.

Mọi thao tác bulk phải có `operationId`, audit log và kết quả từng dòng để retry an toàn.

## 4. Taxonomy nghiệp vụ

### Khóa học

`Course` là aggregate nội dung học, gồm:

- module/bài học/tài liệu;
- lớp/cohort trực thuộc;
- buổi học trực tuyến;
- bài tập và bài kiểm tra gắn khóa;
- quy tắc hoàn thành;
- thời hạn hoàn thành theo assignment;
- tiến độ và lịch sử phiên bản nội dung.

### Assessment

Dùng một engine đánh giá chung với trường `context_type`:

- `COURSE_QUIZ`: bài kiểm tra trong khóa học.
- `COURSE_ASSIGNMENT`: bài tập trong khóa học.
- `STANDALONE_EXAM`: bài thi/kỳ thi không phụ thuộc khóa học.
- `COMPETITION`: cuộc thi có bảng xếp hạng và giải thưởng.

`course_id` bắt buộc với hai context đầu và phải null với hai context sau.

### Competition

Cuộc thi bổ sung:

- cửa sổ đăng ký và thi;
- số lượt thi;
- tie-break: điểm, thời gian hoàn thành, thời điểm nộp;
- leaderboard có trạng thái PROVISIONAL/FINAL;
- reward rule và reward ledger;
- công bố kết quả có audit.

Không ghi thưởng trực tiếp vào bản ghi leaderboard. Dùng ledger riêng để tránh trả thưởng lặp.

## 5. Tiến độ và hạn hoàn thành

Hạn hoàn thành thuộc `course_assignment`, không thuộc course master. Cùng một khóa có thể giao cho các nhóm với hạn khác nhau.

Tiến độ lưu ở ba mức:

- lesson progress;
- course progress snapshot;
- immutable learning event/history.

Completion calculator xét:

- % bài bắt buộc đã hoàn thành;
- điểm tối thiểu các bài kiểm tra bắt buộc;
- thời lượng tối thiểu nếu cấu hình;
- deadline và grace period;
- phiên bản khóa học đã được giao.

## 6. Tổ chức

Organization service quản lý cây đơn vị:

- ROOT
- BRANCH
- DEPARTMENT
- TEAM/GROUP

Membership có thời hạn và có thể nhiều đơn vị. Không lưu một `organizationUnitId` duy nhất làm nguồn sự thật lâu dài; trường hiện tại chỉ nên được xem là primary unit phục vụ tương thích. Quyền theo đơn vị dùng organization membership API hoặc projection/event cache.

## 7. Branding và cấu hình tenant

Configuration service sở hữu:

- tên hệ thống;
- logo, favicon, ảnh nền;
- nội dung giới thiệu;
- màu primary/secondary/background/text;
- tên miền và cấu hình mail;
- feature flags;
- cấu hình external services.

Các secret như API key/Redis password phải mã hóa bằng master key từ environment/secret manager; API đọc không bao giờ trả secret gốc.

## 8. Dịch vụ bên thứ ba tùy chọn

Tạo `ExternalServiceConfig` theo loại:

- REDIS
- SMTP
- VIDEO_CONFERENCE
- AI_PROVIDER
- OBJECT_STORAGE

Mỗi connector có `enabled`, `healthStatus`, `lastCheckedAt`, `configJson`, `encryptedSecret`. Redis là tối ưu hóa cache/session/rate limit chứ không được trở thành điều kiện bắt buộc để hệ thống khởi động. Nếu Redis lỗi, service phải fallback về database/in-memory theo tính năng.

Docker Compose dùng profile `redis`; khách hàng bật bằng:

```bash
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.cls.yml --profile redis up -d
```

## 9. Tin tức

Notification service thêm aggregate `NewsArticle`:

- draft/published/archived;
- publish window;
- audience toàn hệ thống hoặc theo đơn vị;
- pinned/priority;
- acknowledgement bắt buộc tùy tin;
- file đính kèm;
- read receipt.

Tin tức là nội dung broadcast có vòng đời; không nên nhét trực tiếp vào notification message ngắn hạn.

## 10. AI sinh câu hỏi

AI service dùng provider abstraction:

- `LOCAL_OPENAI_COMPATIBLE`: Ollama, vLLM hoặc endpoint local tương thích.
- `REMOTE_OPENAI_COMPATIBLE`: endpoint bên ngoài do khách hàng nhập base URL/model/API key.
- provider khác có adapter riêng nhưng phải trả cùng schema.

Pipeline:

1. File storage tạo phiên bản tài liệu.
2. Extractor lấy text có page/section anchors.
3. Chunking theo cấu trúc tài liệu.
4. AI tạo `QuestionSet` theo JSON Schema chung.
5. Validator kiểm tra schema, đáp án, duplicate, citation và độ phủ.
6. Người có quyền review rồi mới import vào ngân hàng câu hỏi.

Không gửi toàn bộ file tùy tiện cho API ngoài. UI phải hiển thị cảnh báo dữ liệu và tenant có thể tắt remote provider.

## 11. Sửa DOCX/PDF

### DOCX

- Tích hợp editor web như OnlyOffice/Collabora qua integration service.
- Lock theo version và optimistic concurrency.
- Mỗi lần lưu tạo file version mới, không ghi đè bất biến.

### PDF

PDF không phù hợp cho chỉnh nội dung tùy ý như DOCX. Giai đoạn đầu hỗ trợ annotation, highlight, comment, điền form, xoay/xóa/thêm trang. “Edit text PDF” cần editor chuyên dụng hoặc chuyển đổi sang DOCX rồi xuất bản lại; phải lưu bản gốc và lịch sử chuyển đổi.

Quyền cần tách `files:edit`, `files:version:read`, `files:publish`.

## 12. Lộ trình triển khai

### Giai đoạn 1 — nền tảng quyền và schema

- account type + bảo vệ system admin;
- scoped grants + bulk assignment;
- permission catalog mới;
- taxonomy assessment;
- branding/external config;
- migrations không phá dữ liệu cũ.

### Giai đoạn 2 — nghiệp vụ người dùng

- course assignment deadline/progress;
- standalone exam;
- competition leaderboard/reward;
- news;
- UI theo permission, không theo role name.

### Giai đoạn 3 — tài liệu và AI

- file versioning/edit session;
- DOCX integration;
- PDF annotation;
- question generation pipeline và review.

### Giai đoạn 4 — báo cáo và hardening

- reporting projection theo đơn vị;
- audit đầy đủ;
- performance/cache Redis tùy chọn;
- backup/restore, rate limit, penetration test.

## 13. Tiêu chí nghiệm thu bắt buộc

- Không có endpoint đăng ký công khai.
- Không thể khóa/xóa/hạ account type của system admin cuối cùng.
- User có thể đồng thời mang quyền học và dạy ở các scope khác nhau.
- Backend từ chối truy cập ngoài scope ngay cả khi gọi API trực tiếp.
- Course quiz không thể tồn tại thiếu course; standalone exam không bắt buộc course.
- MCQ được chấm xác định, lưu snapshot đáp án và rule tại thời điểm nộp.
- Leaderboard có tie-break ổn định và reward idempotent.
- API key không xuất hiện trong log, response hoặc database dạng plaintext.
- Mọi sửa tài liệu tạo version và audit event.
