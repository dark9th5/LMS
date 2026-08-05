# Danh mục service 0.20.0

Mỗi service nằm trong một thư mục độc lập, có port riêng và có thể giao cho một thành viên/nhóm chịu trách nhiệm.

| Service | Port | Phạm vi |
|---|---:|---|
| `api-gateway` | `8080` | Cổng API duy nhất cho web, xác thực tuyến và chuyển tiếp đến các service nội bộ. |
| `identity-service` | `8081` | Đăng nhập, phiên, tài khoản và mô hình một tài khoản–một vai trò ADMIN/INSTRUCTOR/STUDENT. |
| `organization-service` | `8082` | Cơ cấu tổ chức, đơn vị và quan hệ thành viên phục vụ quản trị. |
| `course-service` | `8083` | Khóa học, mục lục, bài học, tài liệu PDF/DOCX, video, bài thực hành và bài kiểm tra thuộc khóa học. |
| `enrollment-service` | `8084` | Gán người học trực tiếp vào khóa học và lưu bản phân phối nội bộ tương thích dữ liệu cũ; không cung cấp giao diện lớp học. |
| `learning-service` | `8085` | Tiến độ học, mở nội dung khóa học, nộp bài thực hành và trạng thái hoàn thành. |
| `assessment-service` | `8086` | Ngân hàng câu hỏi, bài kiểm tra trong khóa học và bài thi độc lập. |
| `grading-service` | `8087` | Chấm tự động, chấm thủ công, phản hồi và khiếu nại điểm. |
| `reporting-service` | `8088` | Read model, KPI, báo cáo quản trị, báo cáo giảng viên và kết quả cá nhân. |
| `file-storage-service` | `8089` | Tải lên, tải xuống, xem DOCX/PDF, chỉnh sửa tài liệu và phân quyền tệp theo mục đích. |
| `license-service` | `8090` | Xác thực giấy phép và giới hạn tính năng triển khai. |
| `audit-service` | `8091` | Nhật ký kiểm toán bất biến cho thao tác nhạy cảm. |
| `notification-service` | `8092` | Thông báo trong hệ thống, email, mẫu và nhắc hạn. |
| `certificate-service` | `8093` | Cấp và tra cứu chứng chỉ sau khi hoàn thành điều kiện học tập. |
| `ai-service` | `8094` | Trích xuất PDF/DOCX và tạo câu hỏi có kiểm duyệt từ đúng tài liệu của khóa học. |
| `configuration-service` | `8095` | Cấu hình thương hiệu, logo, ảnh nền đăng nhập, màu chủ đạo và dịch vụ ngoài. |
| `integration-service` | `8096` | Điểm mở rộng tích hợp hệ thống bên thứ ba. |
| `operations-service` | `8097` | Health check và tác vụ vận hành có kiểm soát; chỉ bind localhost khi chạy Compose. |
| `competency-service` | `8098` | Khung năng lực và hồ sơ năng lực; module backend tùy chọn. |

## Ranh giới sản phẩm

- Giao diện công khai chỉ có **Khóa học**; không có khu vực Lớp học.
- `enrollment-service` giữ một bản ghi phân phối nội bộ để tương thích dữ liệu cũ, nhưng không công khai API hoặc UI lớp học.
- `assessment-service` phân biệt `COURSE_QUIZ` (nằm trong khóa học) và `STANDALONE_EXAM` (Bài thi độc lập).
- Ba vai trò sản phẩm là `ADMIN`, `INSTRUCTOR`, `STUDENT`; một tài khoản chỉ có đúng một vai trò.
