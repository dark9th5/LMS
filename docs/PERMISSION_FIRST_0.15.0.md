# Permission-first authorization — LMSPilot 0.15.0

## 1. Mục tiêu

Bản 0.15.0 loại bỏ giả định “mỗi tài khoản là quản trị viên, giảng viên hoặc học viên”. Hệ thống chỉ còn hai `accountType`:

- `SYSTEM_ADMIN`: tài khoản quản trị gốc, được bảo vệ và có toàn bộ quyền.
- `USER`: mọi tài khoản còn lại.

Khả năng làm việc của `USER` được quyết định bởi permission hiệu lực. Role vẫn tồn tại trong database/API để tương thích, nhưng được định nghĩa lại thành **gói quyền có thể ghép**, không phải loại người dùng.

Một người có thể đồng thời có `BASIC_USER + COURSE_AUTHOR + GRADER`, hoặc nhận riêng `exams:assign` cho một kỳ thi mà không cần đổi tài khoản.

## 2. Thành phần mô hình quyền

### Permission

Mỗi permission có:

- `code`: mã ổn định dùng trong API/JWT;
- `group`, `label`, `description`: metadata cho giao diện tiếng Việt;
- `allowedScopes`: các phạm vi có thể cấp;
- `risk`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`;
- `legacy`: đánh dấu alias giữ tương thích.

Catalog hiện mô tả đầy đủ 93 permission trong `AccessProfiles.kt`. Test regression đối chiếu để không permission nào bị thiếu metadata.

### Gói quyền

Mười gói hệ thống được đồng bộ khi identity-service khởi động:

| Mã | Mục đích |
|---|---|
| `BASIC_USER` | Học khóa được giao, làm bài, xem điểm/tin tức/chứng chỉ cá nhân |
| `COURSE_AUTHOR` | Tạo, biên tập khóa học, tài liệu và câu hỏi; chưa tự xuất bản |
| `TRAINING_MANAGER` | Xuất bản/giao khóa, quản lý lớp, ghi danh, lịch và lộ trình |
| `EXAM_MANAGER` | Tạo, giao và vận hành kỳ thi độc lập/cuộc thi |
| `GRADER` | Chấm tự luận và xử lý phúc khảo |
| `ORGANIZATION_MANAGER` | Quản lý đơn vị, thành viên và báo cáo phạm vi |
| `COMMUNICATIONS_EDITOR` | Soạn và phát hành tin tức/thông báo |
| `ACCOUNT_MANAGER` | Tạo, nhập, cập nhật và khóa tài khoản; không tự cấp quyền nhạy cảm |
| `ACCESS_ADMINISTRATOR` | Tạo gói, cấp/thu hồi và kiểm tra quyền |
| `PLATFORM_CUSTOMIZER` | Branding, cấu hình và tích hợp bên thứ ba |

Gói hệ thống là read-only. Khách hàng có thể tạo gói tùy chỉnh từ catalog permission.

### Phạm vi

Các phạm vi hỗ trợ: `SYSTEM`, `BRANCH`, `DEPARTMENT`, `GROUP`, `COURSE`, `EXAM`.

- Permission đơn lẻ bị từ chối nếu cấp ở scope không nằm trong `allowedScopes`.
- Khi một gói được cấp ở scope hẹp, chỉ permission tương thích với scope đó có hiệu lực.
- Preview trả `excludedByScope` để quản trị thấy phần nào trong gói không áp dụng.
- `DENY` luôn thắng `ALLOW` tại cùng tài nguyên/phạm vi áp dụng.

## 3. Ba lớp kiểm tra

1. **Coarse gate tại JWT/Spring Security**: claim `permissions` chứa capability đang hoạt động, đủ để đi qua `@PreAuthorize`.
2. **Global gate**: claim `globalPermissions` chỉ chứa quyền từ gói cơ sở hoặc assignment `SYSTEM`. API cho phép truy vấn `SYSTEM` phải kiểm tra claim này.
3. **Exact resource check**: service gọi identity scoped authorization với permission + scope type + resource ID; ownership/enrollment/assignment vẫn được kiểm tra tại domain service.

Cách tách này giữ UI và gateway nhanh, đồng thời không biến quyền ở một khóa học thành quyền toàn hệ thống.

## 4. Luồng quản trị đề xuất

### Tạo tài khoản

1. Quản trị nhập mã, username, họ tên, email và mật khẩu tạm.
2. Chọn gói khởi tạo, mặc định `BASIC_USER`.
3. Người dùng buộc đổi mật khẩu ở lần đầu.
4. Ghép thêm gói theo nhiệm vụ tại màn hình cấp quyền.

Không có endpoint đăng ký công khai.

### Cấp quyền hàng loạt

1. Chọn tối đa 1.000 tài khoản.
2. Chọn gói hoặc permission đơn lẻ.
3. Chọn scope, đối tượng scope, `ALLOW/DENY`, thời điểm bắt đầu/kết thúc.
4. Gọi `POST /api/v1/authorization/grants/preview`.
5. Kiểm tra quyền mới, quyền đã có, quyền bị DENY, permission bị loại theo scope và cảnh báo `CRITICAL`.
6. Xác nhận qua `POST /api/v1/authorization/grants/bulk`.

Assignment giống hệt bản ghi hiện có được bỏ qua và trả `duplicateAssignments`, không gây lỗi unique constraint.

### Kiểm tra và thu hồi

- `GET /api/v1/authorization/users/{userId}/assignments`: các bản ghi gói/quyền đã cấp.
- `GET /api/v1/authorization/explain`: permission hiệu lực và nguồn phát sinh.
- `DELETE /api/v1/authorization/grants/bulk`: thu hồi đúng grant/role assignment được chọn.

Thu hồi một nguồn không xóa quyền nếu permission vẫn đến từ nguồn khác; màn hình giải thích giúp quản trị thấy điều này.

## 5. Thay đổi BE

- Catalog và access profile dùng chung ở `backend/platform-contracts`.
- Identity bootstrap tự đồng bộ 10 gói hệ thống, không cần migration schema.
- User management bảo vệ gói hệ thống khỏi chỉnh sửa và bảo vệ `SYSTEM_ADMIN` khỏi hạ cấp/vô hiệu hóa.
- Các service khóa học, lớp, học tập, thi, file, tổ chức, tin tức và báo cáo đã bỏ kiểm tra tên role `ADMIN/INSTRUCTOR/LEARNER`.
- Danh sách tài nguyên dùng permission + ownership + scoped authorization.
- JWT có claim mới `globalPermissions`.

## 6. Thay đổi FE

- Sidebar và section access chỉ dựa trên `accountType`/permission.
- Console quyền hiển thị tên/mô tả/risk thay cho bắt người dùng nhớ mã.
- Có profile cards, preview, thời hạn, cảnh báo, explain và revoke.
- Tạo/import tài khoản mặc định `BASIC_USER`.
- Danh sách người phụ trách lớp được lọc bằng permission quản lý/biên tập.
- Danh sách ghi danh được lọc bằng `courses:learn`.

## 7. Nâng cấp từ 0.14

1. Sao lưu database và repository.
2. Triển khai source 0.15, build lại identity-service, reporting-service và web cùng toàn stack.
3. Khởi động identity-service để bootstrap gói hệ thống.
4. Đăng xuất toàn bộ phiên hoặc yêu cầu người dùng đăng nhập lại để token có `globalPermissions`.
5. Ánh xạ tài khoản cũ:
   - `LEARNER` → `BASIC_USER`;
   - `INSTRUCTOR` → tùy nhiệm vụ, thường `BASIC_USER + COURSE_AUTHOR + TRAINING_MANAGER + GRADER`;
   - `ADMIN` không tự chuyển thành `SYSTEM_ADMIN`; chỉ quản trị gốc được giữ `SYSTEM_ADMIN`. Các quản trị viên nghiệp vụ nên nhận `ACCOUNT_MANAGER`, `ACCESS_ADMINISTRATOR`, `PLATFORM_CUSTOMIZER` hoặc gói phù hợp.
6. Sau khi UAT, có thể giữ role cũ để tương thích dữ liệu nhưng không dùng chúng trong luồng mới.

Không có migration schema mới trong 0.15. Thay đổi là contract, bootstrap và logic quyền.

## 8. Checklist UAT phân quyền

- USER chỉ `BASIC_USER` học/làm bài/xem điểm nhưng không tạo khóa hay kỳ thi.
- `COURSE_AUTHOR` tạo khóa, sửa khóa mình sở hữu nhưng không tự xuất bản nếu chưa có quyền.
- Gói cấp tại `COURSE A` không quản lý được `COURSE B`.
- `DENY courses:update` tại một khóa chặn quyền ALLOW cùng phạm vi.
- Permission `branding:manage` không thể cấp ở `COURSE`.
- Gói có permission không tương thích hiển thị số lượng bị loại trong preview.
- Người có quyền báo cáo tại phòng ban không xem được KPI `SYSTEM`.
- Thu hồi một assignment không xóa quyền đến từ assignment khác.
- `SYSTEM_ADMIN` không thể bị vô hiệu hóa hoặc hạ account type qua API thông thường.

## 9. Giới hạn xác minh của gói bàn giao

Static/contract regression, Kotlin compile riêng cho platform-contracts và TypeScript syntax parse đã đạt. Full Gradle multi-module build, npm production build và Docker E2E cần chạy lại trên môi trường có đầy đủ dependency. Xem `TEST_RESULTS_LMSPILOT_0.15.0.md`.
