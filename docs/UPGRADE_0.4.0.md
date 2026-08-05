# Nâng cấp repository GitHub lên LMSPilot 0.4.0

## Cách an toàn nhất

1. Sao lưu `.env`, dữ liệu PostgreSQL và thư mục file storage hiện có.
2. Giải nén gói mã nguồn 0.4.0.
3. Chép file `.env` cũ vào thư mục mới; không chép đè source mới bằng source cũ.
4. Chạy kiểm tra tĩnh rồi build lại:

```bash
./scripts/test-static.sh
docker compose build --no-cache web
docker compose up -d --build
./scripts/smoke-test.sh
```

Trên Windows dùng các file `.ps1` tương ứng.

## Đưa lên GitHub hiện tại

```bash
git checkout -b improve/real-lms-ui
git add .
git commit -m "feat: replace demo portal with real LMS workflows"
git push -u origin improve/real-lms-ui
```

Sau khi kiểm tra trên máy thật, tạo Pull Request vào `main`.

## Kiểm tra vai trò

- `admin` phải vào `/dashboard` và thấy menu quản trị.
- `instructor` phải vào `/dashboard` với nhãn **Giảng viên**, không hiện lời chào học viên.
- `student` phải vào `/learning`, chỉ thấy khóa học đã ghi danh, bài kiểm tra và chứng chỉ cá nhân.

Nếu đã từng chạy bản 0.3.0, nên đăng xuất hoặc xóa cookie `lmspilot_*` một lần trước khi kiểm thử ba tài khoản.
