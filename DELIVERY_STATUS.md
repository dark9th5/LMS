# Trạng thái bàn giao — LMSPilot 0.21.0

Backend đã được chuyển sang **Java 21 + Spring Boot 3.5.16** cho `platform-contracts`, `service-support` và toàn bộ 19 microservice. Không còn tệp Kotlin trong `backend/`. API path, port, PostgreSQL schema và Flyway migration hiện có được giữ nguyên.

Đã đạt repository validator, Python regression, Java syntax/public-type scan, TypeScript/TSX syntax scan và CSS parse. Full Gradle test bị chặn vì môi trường không truy cập được `services.gradle.org`; phải chạy lại trên CI trước khi merge vào `main`.

Xem chi tiết tại `TEST_RESULTS_LMSPILOT_0.21.0.md`.
