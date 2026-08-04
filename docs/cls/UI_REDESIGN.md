# Tài liệu lịch sử — Học viện Huyền Tri UI

> **Không còn là giao diện hiện hành.** Runtime fantasy và các component được nhắc bên dưới đã bị loại bỏ từ các bản sau. Giao diện hiện hành nằm trong `../SOFT_SPECTRUM_0.14.0.md`; tài liệu này chỉ được giữ để truy vết lịch sử.

## Mục tiêu

Giao diện được thiết kế lại theo hướng **immersive dark fantasy** nhưng không biến LMS thành trò chơi khó sử dụng. Ngôn ngữ hình ảnh lấy cảm hứng từ tinh đồ, cổng dịch chuyển, thư viện huyền tri và vật liệu thạch anh; cấu trúc thông tin vẫn tuân theo nghiệp vụ đào tạo doanh nghiệp.

## Nguyên tắc thiết kế

1. **Rõ trước, đẹp sau**: nút chính, trạng thái, bảng dữ liệu, biểu mẫu và lỗi luôn có tương phản cao.
2. **Chiều sâu có phân cấp**: nền vũ trụ → lớp sương/aether → panel kính → nội dung; không dùng đổ bóng ngẫu nhiên.
3. **Chuyển động có nguyên nhân**: chuyển trang dùng fade, blur và light sweep; hover dùng nâng 2–5 px; trạng thái trực tuyến dùng pulse nhẹ.
4. **Không gây say chuyển động**: toàn bộ animation quan trọng tuân theo `prefers-reduced-motion`.
5. **Không phụ thuộc Internet**: nền sao, tinh đồ, logo và hiệu ứng đều là CSS/SVG nội bộ.
6. **Tương thích branding**: `--brand-primary`, `--brand-secondary`, `--brand-background` và `--brand-text` được nối với Configuration Service; màu vàng điểm nhấn giữ ở design token mặc định để bảo đảm tương phản.
7. **Permission-first**: PortalShell hỗ trợ `accountType` và `permissions` mới; khi backend/session cũ chưa trả quyền, nó tự lùi về role cũ để tránh làm mất điều hướng.

## Các thành phần đã thay đổi

- `app/globals.css`: design tokens, nền aether, panel kính, thẻ khóa học, bảng, biểu mẫu, thi cử, learning player, responsive và accessibility.
- `app/layout.tsx`: metadata, favicon và dark color scheme.
- `app/template.tsx`: chuyển cảnh giữa route.
- `components/MysticBackdrop.tsx`: nền sao/tinh đồ không dùng ảnh ngoài.
- `components/PortalShell.tsx`: sidebar phân nhóm, trạng thái hệ thống, permission fallback và nhận branding cơ bản.
- `app/login/page.tsx`: cổng đăng nhập dạng “học viện huyền tri”.
- `app/login/LoginForm.tsx`: form mới, hiện/ẩn mật khẩu và trạng thái loading.
- `public/mystic-mark.svg`: favicon/logo mặc định.

## Ánh xạ trải nghiệm

| Nghiệp vụ | Hình ảnh |
|---|---|
| Dashboard | Bảng chỉ huy |
| Học tập | Hành trình học |
| Khóa học | Thư viện khóa học |
| Thi/kiểm tra | Kỳ thi & thử thách |
| Báo cáo | Tinh đồ báo cáo |
| Chứng chỉ/phần thưởng | Thành tựu |
| Branding/tích hợp | Định hình thế giới |

Tên gọi giàu cảm xúc chỉ nằm ở lớp trình bày; route và nghiệp vụ backend không đổi.

## Branding động đã được nối

`app/layout.tsx` gọi Configuration Service qua helper `getPublicBranding`, đặt metadata và ánh xạ cấu hình thành CSS variables cho toàn portal. Branding hiện hỗ trợ tên hệ thống, giới thiệu, logo, favicon, ảnh nền, màu chính, màu phụ, màu nền và màu chữ; khi service chưa sẵn sàng, giao diện tự dùng bộ nhận diện mặc định.

## Kiểm thử cần chạy trên máy đích

```bash
cd apps/web
npm ci
npm run typecheck
npm run build
```

Kiểm thử thủ công ở 1440 px, 1024 px, 768 px và 390 px; bật Reduce Motion trong hệ điều hành để xác nhận giao diện không còn animation liên tục.
