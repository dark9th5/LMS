# LMSPilot 0.17.0

LMSPilot là nền tảng quản lý học tập on-premise dành cho doanh nghiệp, trường học và trung tâm đào tạo. Bản 0.17.0 tập trung sửa toàn diện giao diện, giảm độ phức tạp của khu vực quản trị và ưu tiên không gian cho bài học, bài kiểm tra.

> Trạng thái: **full-source release candidate**. Đây là repository đầy đủ, không phải bản vá. Cần chạy build, migration, Docker smoke test và UAT trên hạ tầng đích trước khi đưa vào production.

## Giao diện thống nhất 0.17

- Chỉ còn **một hệ thống thiết kế**, dùng chung typography, khoảng cách, component và bố cục.
- Hai chế độ màu: `unified-light` và `unified-dark`. Các theme cũ tự ánh xạ sang một trong hai chế độ để giữ tương thích dữ liệu.
- Font hệ thống phổ biến; nội dung thường 16 px, nội dung bài học 18 px, nhãn phụ tối thiểu 13 px.
- Input có màu nền, màu chữ, placeholder, focus và browser autofill được khai báo rõ; không còn lỗi focus làm chữ trùng màu nền ở màn đăng nhập.
- Màu chữ trên màu thương hiệu được tính tự động theo độ sáng. Admin không còn được lưu một màu chữ tùy ý có thể làm mất tương phản.
- Nút và vùng tương tác quan trọng tối thiểu 44 px; focus-visible và reduced-motion được giữ.
- Bài học và câu hỏi thi nằm ở cột chính; mục lục/danh sách câu chỉ là cột phụ và chuyển sang bố cục một cột trên màn hình nhỏ.

## Admin Core

Điều hướng mặc định chỉ giữ các khu vực phục vụ trực tiếp vận hành LMS:

- Tổng quan
- Học tập của tôi
- Khóa học
- Lớp học
- Bài kiểm tra & kỳ thi
- Chấm điểm
- Kết quả
- Báo cáo cơ bản
- Người dùng & quyền
- Tổ chức
- Cài đặt

Các trung tâm nâng cao như Competition, AI Studio, Competency, Operations, Notification Automation, News, Live Session và Learning Path không còn xuất hiện trong sidebar và route giao diện Core trả về 404. Mã backend tương ứng được giữ để tránh migration phá hủy dữ liệu và có thể được triển khai như module tùy chọn sau này.

## Nhập liệu dễ sử dụng hơn

- Phương án, đáp án đúng, thẻ và danh sách thành viên dùng từng dòng riêng với nút `+ / −`.
- Thời lượng, số lần làm, điểm đạt và điểm câu hỏi dùng `NumberStepper`.
- Không còn yêu cầu nhập cấu hình dịch vụ ngoài bằng JSON trong luồng thông thường.
- Import tài khoản hàng loạt đi qua CSV/XLSX có xem trước thay vì dán nhiều bản ghi vào một textarea.
- Cấp quyền thông thường ưu tiên gói công việc và phạm vi; cấu hình permission kỹ thuật được giảm khỏi luồng chính.

## Kiến trúc

- Frontend: Next.js 16, React 19, TypeScript 5.9.
- Backend: Kotlin 2.0, Spring Boot 3.5, Java 21.
- 19 service backend và API Gateway.
- PostgreSQL, RabbitMQ; Redis, OnlyOffice/Collabora và AI local là profile tùy chọn.
- Account type: `SYSTEM_ADMIN` và `USER`; quyền được cấp theo permission, gói quyền và phạm vi tài nguyên.

## Chạy nhanh

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup.ps1
```

### Linux

```bash
chmod +x scripts/*.sh
./scripts/setup.sh
```

Sau khi setup và smoke test thành công, mở `http://localhost:3000`.

## Kiểm tra source

```bash
python scripts/validate-repository.py
python -m unittest discover -s tests -p "test_*.py"
cd apps/web
npm ci
npm run typecheck
npm run build
```

Kết quả trong môi trường đóng gói 0.17.0:

- Repository validator: **PASS** — 26 JSON, 28 YAML, 19 service, 18 service dùng Flyway.
- Python static/contract/UI suite: **115 test đạt, 34 test lịch sử được skip có chủ ý (149 test tổng cộng)**.
- TypeScript/TSX syntax transpilation: **62 file, 0 lỗi cú pháp**.
- CSS parser: `globals.css` và `unified.css` đều **0 lỗi cú pháp**.
- Trình render ảnh giao diện: **7 ảnh đạt**, gồm login, dashboard sáng/tối/mobile, khóa học, bài học và bài thi.

## Cấu trúc

```text
apps/web/                 Next.js portal và unified design system
backend/platform-*        contracts và support dùng chung
backend/services/*        19 Spring/Kotlin service, gồm API Gateway
contracts/cls/            JSON Schema và mẫu dữ liệu
infrastructure/            PostgreSQL, RabbitMQ, observability
deploy/                    profile dịch vụ tùy chọn
tests/                     contract, requirement và UI regression
scripts/                   setup, validate, smoke, backup/restore
docs/                      kiến trúc, quyền, runbook và hướng dẫn vận hành
HUONG_DAN_DU_AN_LMSPILOT.docx Tài liệu tổng quan & hướng dẫn sử dụng chi tiết
```

## Trước production

Bắt buộc chạy lại trên máy đích: `npm ci`, semantic typecheck, Next production build, Gradle test, fresh/upgrade migration, Docker smoke, browser E2E, accessibility audit bằng trình duyệt, RBAC/UAT, tải đồng thời kỳ thi, backup/restore drill, TLS/domain, SMTP/LDAP và pentest.
