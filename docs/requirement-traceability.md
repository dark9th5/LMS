# Requirement Traceability

Quy ước: **Đã có luồng lõi** = API/schema/quyền nghiệp vụ chính đã có; **Nền tảng** = đã có chỗ mở rộng nhưng còn thiếu chính sách hoặc giao diện chi tiết; **TBD** = bị chặn bởi quyết định chưa có trong BA.

| Khu vực BA | Vị trí code | Trạng thái thực tế |
|---|---|---|
| UC-AUTH-01 đăng nhập/đăng xuất/phiên | `identity-service`, web BFF, gateway | Đã có luồng lõi; tự refresh phiên, hỗ trợ HTTP LAN/HTTPS theo cấu hình |
| UC-ORG-01 cơ cấu tổ chức | `organization-service`, portal | Đã có luồng lõi |
| UC-USER-01 vòng đời tài khoản | `identity-service`, portal | Đã có tạo/list/đổi trạng thái/reset API; giao diện hiện tập trung tạo và tra cứu |
| UC-USER-02 nhập hàng loạt | Chưa có endpoint import chính thức | Nền tảng / TBD chính sách import một phần hay toàn bộ |
| UC-RBAC-01 vai trò và quyền | Identity, Gateway, security tại từng service | Đã có luồng lõi; API vẫn chặn dù giao diện bị gọi trực tiếp |
| UC-LDAP-01 LDAP/AD | `integration-service` | Nền tảng / cần mapping khách hàng |
| UC-COURSE-01..03 | `course-service`, portal | Đã có API vòng đời, bài học, xuất bản và giới hạn chủ sở hữu; học viên chỉ xem khóa đã ghi danh; UI hiện tập trung danh sách/tạo |
| UC-CONTENT-01 | Course lessons, `file-storage-service` | Nền tảng; trình soạn thảo/transcoding chưa phải bản chính thức |
| UC-CLASS-01 / UC-ENROLL-01 | `enrollment-service`, portal | Đã có API lõi, chống tạo trùng và giới hạn lớp được phân công; UI hiện tập trung danh sách/tạo lớp |
| UC-LEARN-01 / UC-LEARN-02 | `learning-service`, portal | Đã có API lõi; server xác minh ghi danh/khóa/bài, tự tính tiến độ và giới hạn giảng viên theo lớp được phân công |
| UC-ASSIGN-01 bài thực hành | File storage và grading extension points | Nền tảng / TBD chính sách nộp lại, trễ hạn |
| UC-QBANK-01 / UC-EXAM-01 | `assessment-service` | Đã có API lõi và giới hạn chủ sở hữu/phạm vi |
| UC-EXAM-02 làm và nộp bài | Assessment session API | Đã có bắt đầu, tự lưu, nộp idempotent, chống mở nhiều phiên đang hoạt động; UI làm bài chi tiết chưa hoàn thiện |
| UC-GRADE-01 chấm điểm | `grading-service` | Đã có chấm khách quan, hàng chờ tự luận theo phạm vi và ngữ cảnh khóa học cho báo cáo; UI nhập rubric/điểm chi tiết còn nền tảng |
| UC-RETAKE-01 thi lại | Assessment attempt policy | Đã có giới hạn lượt/khoảng chờ; cách tổng hợp điểm cuối còn theo cấu hình/TBD |
| UC-AI-01 AI local | `ai-service` | Adapter tùy chọn, chỉ tạo bản nháp để duyệt |
| UC-COMP-01 hoàn thành | Learning completion event | Nền tảng; ma trận điều kiện cuối cùng còn TBD |
| UC-CERT-01 chứng chỉ | `certificate-service` | Metadata, cấp/thu hồi/cấp lại/xác minh; mẫu PDF/QR/chữ ký chính thức còn TBD |
| UC-NOTI-01 thông báo | `notification-service`, menu portal | Đã có in-app read/unread; SMTP tùy chọn |
| UC-REP-01 / 02 | `reporting-service`, dashboard portal | Đã có read model, phạm vi lớp, CSV và ghép điểm an toàn khi ghi danh là duy nhất; danh mục báo cáo chính thức còn TBD |
| UC-LIC-01 | `license-service` | Nền tảng ký/xác thực offline; chính sách hết hạn còn TBD |
| UC-AUDIT-01 | `audit-service` | Đã có kho/search sự kiện nhạy cảm |
| UC-OPS-01 | `operations-service`, Actuator | Đã có health/job nền tảng |
| UC-OPS-02 | scripts backup/restore | Quy trình tham chiếu; RPO/RTO/retention còn TBD |
| UC-OPS-03 | container versioning, runbook | Nền tảng rollback |
| UC-INTEG-01 | `integration-service` | Adapter/job nền tảng / cần đặc tả hệ thống đích |
| UC-CUSTOM-01 | `configuration-service`, portal cấu hình | Đã có lưu/tra cứu cấu hình; áp theme động toàn portal còn nền tảng |
