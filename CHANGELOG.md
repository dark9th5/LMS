# Changelog

## 0.15.0 - 2026-08-04

### Permission-first authorization

- Chuyển account model về `SYSTEM_ADMIN` và `USER`; loại bỏ role-name gating khỏi các service nghiệp vụ.
- Thêm catalog metadata cho 93 permission và 10 gói quyền theo công việc.
- Thêm preview, giải thích nguồn quyền, phát hiện cấp trùng, thu hồi từng assignment và kiểm tra scope hợp lệ.
- Thêm claim `globalPermissions` để phân biệt quyền toàn hệ thống với capability theo phạm vi.
- Cập nhật frontend quản trị, danh sách người phụ trách lớp, ghi danh và import tài khoản theo permission/gói quyền mới.

### Verification

- 127 test Python đạt; platform-contracts compile bằng Kotlin compiler; 62 tệp TS/TSX parse không lỗi cú pháp.
- Full npm/Gradle/Docker verification chưa chạy được trong môi trường đóng gói; xem `TEST_RESULTS_CLS_0.15.0.md`.

## 0.14.0 - 2026-08-04

- Thêm Sắc màu Cân bằng (`soft-spectrum`) làm theme mặc định: giữ bố cục đa sắc tươi sáng nhưng giảm độ bão hòa, dùng coral/rose/aqua/violet/lime/yellow mềm hơn và tăng surface trung tính để giảm mỏi mắt.
- Tách sidebar khỏi toàn bộ accent đa sắc: rail navy–graphite đơn sắc, group toggle/icon/count/mục con cùng một hệ xám, active state trắng ngà; các lớp `accent-cyan/indigo/violet` bị override đồng nhất.
- Phủ Soft Spectrum lên dashboard hero, KPI, quick route, page header, login, summary, khóa học, lớp, learning và kỳ thi; sửa các selector Cosmic cũ gây card tối hoặc chữ thiếu tương phản.
- Thay `enterprise-blue` bằng `soft-spectrum` nhưng giữ tổng số 10 theme; Theme Studio preview phản ánh đúng rail đơn sắc và content color blocking.
- Thêm Flyway V6: đổi default/allowlist, map dữ liệu cũ, chỉ thay hai palette mặc định đã biết và giữ palette do khách hàng tùy chỉnh; neutralize seed “Học viện Huyền Tri” nếu chưa từng đổi tên.
- Progress ring nhận CSS theme token thay vì hard-code; giao diện vẫn self-contained, responsive và reduced-motion.
- Chromium production render 23 capture và contact sheet, không có page/console/server error; bổ sung 5 regression test, nâng tổng suite lên 121 test.

## 0.13.0 - 2026-08-04

- Thay catalog 0.12 thiên về không gian/fantasy bằng 10 cá tính độc lập cho doanh nghiệp, trường học và tổ chức: Doanh nghiệp Hiện đại, Điều hành Cao cấp, Học viện Di sản, Trường học Năng động, Tổ chức Tin cậy, Xưởng Sáng tạo, Giáo dục Xanh, Tạp chí Cổ điển, Tối giản An nhiên và Trung tâm Công nghệ.
- Mỗi theme có typography, hình học, mật độ, surface, sidebar, hero, KPI và ngôn ngữ thị giác riêng; loại bỏ scenery sao/quỹ đạo/nebula khỏi runtime hiện hành thay vì chỉ thay palette.
- Rút sidebar thành accordion hai tầng permission-aware với ba nhóm Học tập, Đánh giá và Quản trị; tự mở nhóm chứa route hiện tại, chỉ mở một nhóm và giữ đầy đủ `aria-expanded`/`aria-controls`.
- Chuyển default sang `enterprise-blue`; thêm Flyway V5 để ánh xạ 10 khóa cũ sang catalog mới, cập nhật database default và constraint allowlist mà không sửa migration lịch sử.
- Trung hòa copy runtime để phù hợp nhiều loại tổ chức, đồng thời giữ nguyên API, scoped RBAC, trạng thái dữ liệu thật, command palette và luồng lưu branding.
- Sửa lỗi compositor Chromium của bề mặt Giáo dục Xanh trên trang dài; production visual QA tạo 23 ảnh không có page/console/server error cùng contact sheet 10 theme.
- Bổ sung 5 regression test cho catalog đa đối tượng, accordion theo quyền, khác biệt cấu trúc và copy trung tính; toàn bộ suite đạt 116 test, TypeScript và Next.js production build đều đạt.

## 0.12.0 - 2026-08-04

- Thêm Theme Studio trong trang quản trị với 10 preset hoàn chỉnh thuộc bốn nhóm Không gian, Khoa học, Học thuật và Tối giản; hỗ trợ tìm kiếm, lọc dark/light, xem thử toàn trang, hoàn tác và áp dụng toàn hệ thống.
- Mở rộng branding contract bằng `themeKey`, validation allowlist và Flyway `V4`; theme được lưu thật ở Configuration Service thay vì localStorage hoặc fixture giao diện.
- Đồng bộ theme qua SSR root layout và public branding để login, portal và admin dùng đúng giao diện ngay từ HTML đầu tiên; vẫn cho phép tùy chỉnh palette thương hiệu riêng.
- Xây registry type-safe và hệ token đa theme cho màu, typography, hình học, radius, shadow, atmosphere, shell, hero, KPI, bảng, form, modal và trạng thái; không dùng asset/font/CDN ngoài.
- Bổ sung 2 theme sáng có điều chỉnh tương phản chuyên biệt cùng 8 theme tối; sửa specificity cho KPI, quick route, topbar và hero metric qua visual QA.
- Chromium production render 23 ảnh, gồm gallery Theme Studio, 10 dashboard theme, desktop và mobile; không ghi nhận page/console/server error.
- Bổ sung 7 Theme Studio contract/UI test; toàn bộ suite đạt 111 test, TypeScript và Next.js production build đều đạt.

## 0.11.0 - 2026-08-04

- Thay Spectrum OS bão hòa màu bằng Cosmic Research UI: nền deep-space gần đen, phân lớp graphite, xanh lam dữ liệu, cyan telemetry, violet chiều sâu và amber/coral chỉ dành cho trạng thái có ý nghĩa.
- Đổi toàn bộ identity runtime sang `CosmicShell`, `CosmicField` và biểu tượng quỹ đạo; loại bỏ tên, selector và asset Spectrum/Chromatic/Prism khỏi giao diện đang chạy.
- Xây lại login như đài quan sát tri thức; đổi dashboard thành mission control; khóa học, lớp, học tập và kỳ thi thành các module nghiên cứu/quỹ đạo nhất quán.
- Phủ material tối mới lên form, bảng, modal, detail/player và workspace quản trị trong khi giữ nguyên API, scoped RBAC, state thật và command palette theo quyền.
- Sửa responsive thực ở journey mobile: chuyển lưới hai cột sang một cột, khôi phục nhịp đọc tiêu đề, số liệu và mastery orbit ở màn hình 390 px.
- Render production bằng Chromium và chụp lại 11 ảnh desktop/mobile; không có JavaScript/page/console/server error trong lượt chụp.
- Bộ 8 Cosmic UI regression test cùng toàn bộ suite 104 test, TypeScript và Next.js production build đều đạt trước khi đóng gói.

## 0.10.0 - 2026-08-04

- Loại bỏ hoàn toàn Astral Academy V3: xóa stylesheet, shell, nền, logo và ngôn ngữ fantasy của 0.9.0; thay bằng Spectrum OS được xây dựng lại từ số 0.
- Dựng nhận diện chromatic mới với nền sáng, rail điều hướng tối, typography editorial cỡ lớn, bento bất đối xứng, hình khối đồ họa và bảng màu coral/pink/violet/blue/cyan/lime/yellow.
- Viết lại login, đổi mật khẩu, shell, command palette, page header, dashboard, khóa học, lớp, hành trình học và kỳ thi; phủ material system mới lên detail/player/form/table/modal/admin workspace.
- Giữ nguyên API, RBAC, trạng thái tải/lỗi/rỗng và luồng dữ liệu thật; command palette tiếp tục lọc route theo quyền và hỗ trợ `Ctrl/Cmd + K`/`Escape`.
- Bổ sung responsive cho sáu dải màn hình, reduced motion, high contrast, focus-visible, scrollbar keyboard-safe và asset CSS/SVG tự chứa không cần CDN.
- Render build production bằng Chromium thực, chụp 11 ảnh desktop/mobile với fixture QA cô lập; không có JavaScript/page/console error trong ma trận đã chụp.
- Thay bộ regression UI cũ bằng 8 Spectrum OS contract test; tổng suite vẫn đạt 104 test, TypeScript và Next.js production build đạt.

## 0.9.0 - 2026-08-04

- Tái thiết kế toàn bộ portal thành Astral Academy V3 với hệ token, chiều sâu không gian, typography, glass surface, bản đồ thiên thể và chuyển động có chủ đích.
- Thay mới shell điều hướng, sidebar phân nhóm/đánh số, top bar theo ngữ cảnh, trạng thái realm và Command Atlas mở bằng `Ctrl/Cmd + K` với kết quả lọc theo quyền thật.
- Dựng lại hoàn toàn login, dashboard, khóa học, lớp học, hành trình học tập, kỳ thi, đổi mật khẩu và header dùng chung; làm giàu trạng thái, số liệu, hành động và phân cấp thông tin.
- Phủ lại các workspace nghiệp vụ, bảng, form, modal và console quản trị bằng design system mới nhưng giữ nguyên API, quyền và luồng dữ liệu thật.
- Bổ sung responsive cho sáu dải màn hình, focus-visible, high-contrast và `prefers-reduced-motion`; không dùng asset thị giác ngoài hệ thống.
- Thêm 8 UI regression test; tổng suite đạt 104 test, semantic TypeScript, Next.js production build và npm audit 0 vulnerability.

## 0.8.2 - 2026-08-03

- Gắn phiên thi, lượt làm, chấm điểm và reporting vào đúng enrollment/course/lesson; khóa race khi bắt đầu, lưu đáp án và nộp bài.
- Chuyển hoàn thành Assignment/Exam sang kết quả do máy chủ xác minh; bài thực hành chỉ hoàn thành sau khi giảng viên chấp nhận.
- Hiện thực shuffle, auto/manual grade, HIGHEST/LATEST/AVERAGE, kết quả hiệu lực và cập nhật tiến độ/report/notification nhất quán.
- Bổ sung hàng đợi chấm bài thực hành theo đúng lớp, xem file, trả chỉnh sửa và giao diện chấm câu tự luận có đề/câu trả lời.
- Thay quyền file theo purpose bằng grant chính xác theo file-người dùng cho course, assignment và news; kiểm owner/purpose khi gắn file; hỗ trợ HTTP byte-range cho video/audio/PDF.
- Gia cố HTML tin tức bằng sanitizer allowlist idempotent và bổ sung upload/download tệp đính kèm theo audience.
- Trì hoãn phát domain event đến sau DB commit, nối kết quả chấm sang Learning và đồng bộ trạng thái phiên thi.
- Mở rộng regression suite lên 96 test; frontend typecheck/build và npm audit vẫn đạt.

## 0.8.1 - 2026-08-03

- Sửa lỗi type contract Learning Path và thay TypeScript syntax-only checker bằng semantic project typecheck.
- Xác minh clean `npm ci`, Next.js production build và đưa `npm audit` về 0 known vulnerability.
- Đồng bộ Spring Boot 3.5.16/Spring Cloud 2025.0.3, bật lại compatibility verifier và dùng `npm ci` trong Dockerfile.
- Khóa bulk idempotency bằng DB pessimistic lock; ràng buộc replay theo operation type và người thực hiện.
- Chặn cross-origin mutation trong BFF, thêm security headers, loại debug login và IP LAN hard-code.
- Gia cố File Storage theo owner/purpose, giới hạn edit/version access và tải callback editor có timeout/size/no-redirect.
- Mở rộng regression suite lên 80 test; bổ sung báo cáo audit trung thực và production blockers.

## 0.8.0 - 2026-08-03

- Thêm Learning Path nhiều chặng: pin course version, unlock tuần tự, giao theo user/đơn vị, auto-enroll và tổng hợp tiến độ từ Learning Service.
- Thêm KPI reporting theo SELF/ASSIGNED/SYSTEM và thống kê theo khóa học: completion, pass rate, overdue, due soon, activity, progress và score.
- Thêm Notification Template quản trị runtime theo event, preview, audit và kênh in-app/email.
- Thêm due reminder scheduler trước/sau hạn: Reporting read model, service token, idempotent claim, timeout, failure retry và manual run.
- Bổ sung permission/UI/navigation cho learning paths, KPI và notification automation; gia cố xử lý lỗi thao tác xóa.
- Chuẩn hóa source version 0.8.0, Node 22 và `npm ci` trong CI.
- Mở rộng static/contract/unit suite lên 73 test và cập nhật ma trận truy vết/tài liệu build.

## 0.7.0 - 2026-08-03

- Hoàn thiện import tài khoản CSV/XLSX với mapping, preview, lỗi từng dòng, create/upsert, atomic/partial, idempotency, capacity lock và chống XLSX zip expansion.
- Cưỡng chế license offline trên feature, export, tác vụ ghi và giới hạn active users; bổ sung grace period/read-only.
- Thêm snapshot khóa học bất biến và ghim course version xuyên lớp, tiến độ và trình học.
- Tách bài thực hành thành aggregate submission riêng, kiểm tra file owner/purpose, attempt, late status, queue chấm và feedback.
- Bổ sung LDAP/AD authentication an toàn với local protected-admin fallback, timeout và LDAP filter escaping.
- Bổ sung email outbox có lease/retry/dead-letter state và operations host agent có allowlist, claim token, heartbeat/lease.
- Nâng test connection của integration registry theo HTTP/TCP/TLS/file; cấm credentials trong endpoint.
- Nối trình nhập file thật vào giao diện quản trị; mở rộng static/contract suite lên 50 test.
- Thêm ma trận truy vết BA+CLS và operations runbook; giữ trạng thái release candidate do chưa chạy full build/Docker E2E trong môi trường tạo bản.

## 0.5.0 - 2026-08-03

- Chuyển account model sang `SYSTEM_ADMIN`/`USER`, giữ Admin/Giảng viên/Học viên làm role mặc định và hỗ trợ role tùy chỉnh.
- Thêm scoped RBAC, bulk account/grant, protected bootstrap admin và cơ cấu tổ chức nhiều cấp.
- Thêm giao khóa học/bài thi theo user hoặc đơn vị, deadline/progress/live session, standalone exam và competition/reward.
- Thêm branding động, tin tức, external-service registry, Redis tùy chọn và gia cố secret runtime.
- Thêm AI local/API với JSON Schema chung, provenance, review/import câu hỏi.
- Thêm file version, DOCX edit session/callback và PDF annotation/version.
- Thiết kế lại portal theo phong cách Học viện Huyền Tri, permission-first, responsive và reduced-motion.
- Mở rộng static suite lên 30 requirement/contract tests và kiểm tra Flyway trùng version.

## 0.4.0 - 2026-07-30

- Thiết kế lại portal theo luồng LMS nhiều trang, tăng kích thước chữ, khoảng cách và khả năng đọc trên desktop/mobile.
- Tách riêng danh sách/chi tiết khóa học, lớp, trình học, bài kiểm tra và hàng chờ chấm; loại bỏ trải nghiệm dồn mọi dữ liệu trong một trang.
- Nối giao diện với API thật cho tạo khóa học, thêm/sửa bài học hoặc tệp, xuất bản, tạo lớp, ghi danh, tiến độ, nộp bài thực hành, thi và chấm thủ công.
- Thêm trình thi có tự lưu định kỳ, đồng hồ, tự nộp khi hết giờ, idempotency và trạng thái kết quả.
- Sửa lỗi dùng nhầm giao diện vai trò bằng phản hồi user sau login, cookie refresh dùng toàn site, điều hướng theo role và render động không cache.
- Ẩn thanh cuộn của sidebar và mục lục khóa học nhưng vẫn giữ cuộn bằng chuột/bàn phím; bổ sung xem PDF và phát video/audio trực tiếp từ File Storage Service.
- Bổ sung smoke test API/web theo ba vai trò và 14 test contract cho role routing, API thật, lesson editor, inline file, idempotency và quy tắc scrollbar.

## 0.3.0 - 2026-07-30

- Gia cố quy trình cài đặt một lệnh với setup, preflight và smoke test cho Windows/Linux.
- Sửa dependency/version, Gradle Wrapper/checksum, AI YAML và endpoint Organization.
- Loại bỏ secret runtime mặc định, tách mật khẩu database và bổ sung kiểm tra cấu hình bắt buộc.
- Nâng React/React DOM lên 19.2.6 và khóa chính xác version frontend.
- Gia cố file upload, CSV export, SMTP, exception logging và trạng thái khóa học khi cập nhật tiến độ.
- Thêm BuildKit cache, graceful shutdown, log rotation và bộ test contract của repository.
- Viết lại README, trạng thái bàn giao và checklist UAT/production theo đúng mức đã xác minh.

## 0.2.0 - 2026-07-30

- Thay dashboard và màn hình danh sách tĩnh bằng dữ liệu API thật, trạng thái tải/lỗi/rỗng và biểu mẫu cốt lõi.
- Đồng bộ seed demo xuyên suốt Identity, Course, Enrollment, Learning, Assessment, Reporting và Notification.
- Giới hạn khóa học, lớp, tiến độ, câu hỏi, bài thi, hàng chờ chấm và báo cáo theo vai trò/phạm vi; học viên chỉ thấy khóa đã ghi danh, giảng viên chỉ thấy lớp được phân công.
- Chặn học viên xem hoặc bắt đầu bài thi ngoài khóa học được giao; không tạo thêm phiên thi khi vẫn còn phiên đang hoạt động.
- Siết idempotency cho ghi danh, cập nhật tiến độ và nộp bài thi để không trả nhầm dữ liệu khi khóa bị tái sử dụng.
- Sửa tính tiến độ: server tự xác minh ghi danh và số bài bắt buộc, không tin tổng số bài do client gửi lên.
- Sửa dashboard để tách chính xác Chưa bắt đầu, Đang học, Hoàn thành và Quá hạn; dữ liệu tham chiếu phụ không còn làm hỏng toàn bộ màn hình.
- Truyền ngữ cảnh khóa học sang kết quả chấm và chỉ cập nhật báo cáo khi xác định được duy nhất ghi danh phù hợp, không tự gán điểm vào lớp gần nhất.
- Sửa phiên web trong LAN: refresh được cả khi access cookie đã hết hạn, gom các yêu cầu refresh song song, logout giữ đúng hostname/IP, cookie Secure điều khiển riêng cho HTTP/HTTPS; phiên vẫn bị xóa khi token mới tiếp tục bị từ chối.
- Thêm thông báo trong portal, xử lý login lỗi mạng và hoàn thiện kiểu hiển thị responsive/modal.
- Sửa mẫu HTML in chứng chỉ để tạo tài liệu hợp lệ, không còn ký tự escape thừa trong thuộc tính.
- Đưa Audit, Notification, Certificate, Configuration và Operations vào stack mặc định; AI/Integration vẫn tùy chọn.
- Mở rộng validation repository cho version, Flyway migration và service-token của internal API.

## 0.1.0 - 2026-07-30

- Khởi tạo monorepo LMSPilot On-Premise.
- Thêm portal, API Gateway, 18 Kotlin/Spring service, PostgreSQL schema isolation, RabbitMQ, Redis, file storage, observability, backup/restore và tài liệu traceability.
