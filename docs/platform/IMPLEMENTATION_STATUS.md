# Trạng thái triển khai LMSPilot 0.15.0

Đây là source tree đầy đủ; thay đổi nằm trực tiếp trong frontend, backend, contracts, scripts, tests và docs.

## Trọng tâm 0.15

- Account type chỉ còn `SYSTEM_ADMIN` và `USER`; không có đăng ký công khai.
- Role được dùng như gói quyền có thể ghép, không còn là loại người dùng.
- 93 permission có metadata tiếng Việt, risk và allowed scope; 10 gói hệ thống được bootstrap và read-only.
- Bulk grant có preview, phát hiện trùng, cảnh báo quyền nhạy cảm, thời hạn và lọc permission không phù hợp scope.
- Có API/UI giải thích nguồn quyền và thu hồi đúng từng assignment.
- JWT tách `permissions` và `globalPermissions`; API phạm vi SYSTEM kiểm claim toàn cục.
- Business services không còn access gate dựa trên tên `ADMIN/INSTRUCTOR/LEARNER`.
- FE điều hướng, quản lý lớp, ghi danh, import và console quyền dùng permission hiệu lực.

## Năng lực đã có trong source

- Khóa học/lớp/ghi danh, tiến độ ghim version, deadline và lộ trình học tập.
- Quiz, bài kiểm tra trong khóa, bài thi độc lập, competition, leaderboard và reward.
- Auto-grade câu khách quan, hàng chờ chấm tự luận, lịch sử điểm và phúc khảo.
- AI local hoặc OpenAI-compatible API, JSON Schema chung, provenance, review/import.
- File version, DOCX edit session/callback, PDF revision/annotation.
- Branding/theme runtime, news, reporting, notification, certificate, license và integrations.
- Import CSV/XLSX, tổ chức nhiều cấp, LDAP tùy chọn, audit và vận hành.

## Xác minh 0.15

- Repository validator: PASS.
- Python regression: **127 passed, 2 subtests passed**.
- Permission catalog Kotlin compile: PASS.
- TypeScript/TSX syntax parse: 62 tệp, 0 lỗi.
- Shell syntax và role-gate scans: PASS.

## Chưa xác minh tại đây

- Full semantic TypeScript/Next build do registry dependency 404.
- Full Gradle build/test do distribution/dependency không tải được.
- Docker integration, browser E2E thật, load/security/restore drill và UAT.

Chi tiết: `../PERMISSION_FIRST_0.15.0.md`, `../../TEST_RESULTS_LMSPILOT_0.15.0.md`, `../SOFT_SPECTRUM_0.14.0.md` (lịch sử giao diện) và root `DELIVERY_STATUS.md`.
