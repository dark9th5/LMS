# LMSPilot 0.8.0 — Full-source release candidate

Bản 0.8.0 hợp nhất repository LMSPilot gốc, tài liệu BA/URD và yêu cầu LMSPilot mở rộng.

Điểm mới so với 0.7.0:

- Learning path nhiều chặng, pin version, sequential unlock, assignment theo user/đơn vị và auto-enrollment.
- KPI dashboard theo scope và theo khóa học.
- Notification templates cấu hình runtime.
- Due reminder rules trước/sau hạn, Reporting read model, idempotent dispatch, timeout và failure retry.
- Hoàn thiện permission/UI/audit cho lộ trình, KPI và notification automation.
- Chuẩn hóa Node 22/npm ci trong CI và nâng version source lên 0.8.0.

Static/contract/unit suite: **73/73 đạt**. Full Gradle/npm/Docker E2E vẫn phải chạy trên máy đích có registry và Docker.
