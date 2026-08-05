# Nền tảng thiết kế giao diện LMSPilot 0.17.0

Bản 0.17.0 không sao chép trực tiếp giao diện của một sản phẩm cụ thể. Thiết kế được tổng hợp từ các tiêu chuẩn và design system có tài liệu triển khai, kiểm thử khả năng tiếp cận và hướng dẫn sử dụng rõ ràng.

## Tài liệu tham khảo chính

1. **WCAG 2.2 — W3C**  
   https://www.w3.org/TR/WCAG22/  
   Dùng làm chuẩn cho tương phản, resize 200%, reflow, bàn phím, focus không bị che, target size, xác thực dễ tiếp cận và thông báo trạng thái.

2. **WCAG Understanding — Contrast Minimum**  
   https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum  
   Chữ thường phải đạt tối thiểu 4.5:1; chữ lớn tối thiểu 3:1. Quy tắc này được áp dụng cho text, placeholder và trạng thái focus.

3. **WCAG Understanding — Target Size Minimum**  
   https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html  
   Control quan trọng của LMSPilot được thiết kế tối thiểu 44 px để dễ dùng trên chuột và cảm ứng, cao hơn mức tối thiểu 24 px của WCAG AA.

4. **GOV.UK Design System — Styles**  
   https://design-system.service.gov.uk/styles/  
   Tham khảo hệ thống typography, khoảng cách nhất quán, cấu trúc heading và functional colour.

5. **GOV.UK Design System — Spacing**  
   https://design-system.service.gov.uk/styles/spacing/  
   LMSPilot dùng một thang khoảng cách lặp lại thay vì đặt margin/padding ngẫu nhiên cho từng trang.

6. **GOV.UK Design System — Button**  
   https://design-system.service.gov.uk/components/button/  
   Mỗi màn hình chỉ ưu tiên một hành động chính; nội dung nút mô tả hành động, dùng sentence case và tránh nhiều nút primary cạnh tranh nhau.

7. **U.S. Web Design System — Side Navigation**  
   https://designsystem.digital.gov/components/side-navigation/  
   Điều hướng bên trái được giữ nông, có active state rõ, nhãn ngắn và luôn kiểm tra bằng bàn phím.

8. **U.S. Web Design System — Components**  
   https://designsystem.digital.gov/components/overview/  
   Tham khảo cách xây component nhất quán, mobile-friendly và dùng token chức năng thay vì CSS trang trí riêng lẻ.

9. **Carbon Design System — Data table**  
   https://carbondesignsystem.com/components/data-table/usage/  
   Tham khảo cách tổ chức bảng dữ liệu quản trị, toolbar, trạng thái hàng và hành động theo ngữ cảnh.

## Nguyên tắc áp dụng vào LMS

- **Task-first:** trang học ưu tiên nội dung; trang thi ưu tiên câu hỏi; admin ưu tiên công việc cần xử lý.
- **Một hệ thống, hai chế độ:** light/dark dùng chung component, kích thước, khoảng cách và hành vi.
- **Màu có vai trò:** màu chủ đạo cho hành động và focus; màu trạng thái không dùng làm trang trí đại trà.
- **Typography dễ đọc:** body 16 px, nội dung bài học 18 px, line-height rộng, font hệ thống phổ biến.
- **Progressive disclosure:** permission kỹ thuật, JSON và thao tác nâng cao không xuất hiện trong luồng mặc định.
- **Không dùng UUID như giao diện:** người dùng chọn thực thể theo tên, mã và ngữ cảnh; ID chỉ là dữ liệu nội bộ hoặc cột import nâng cao.
- **Responsive theo tác vụ:** desktop dùng cột phụ; mobile chuyển mục lục/danh sách câu thành drawer hoặc bố cục một cột.
- **Motion tiết chế:** animation ngắn, không nhấp nháy và tôn trọng `prefers-reduced-motion`.

## Màn hình đại diện

Ảnh render từ chính `globals.css` và `unified.css` được lưu trong `docs/screenshots/0.17.0`. Script tái tạo ảnh: `python scripts/render-ui-previews.py`.
