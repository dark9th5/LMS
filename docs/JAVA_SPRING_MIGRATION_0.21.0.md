# Chuyển backend sang Java Spring Boot — 0.21.0

## Phạm vi

- Chuyển `platform-contracts`, `service-support` và 19 Spring Boot service từ Kotlin sang Java 21.
- Giữ Spring Boot 3.5.16, Gradle multi-module, API path, port, schema và Flyway migration.
- Chuyển test backend sang JUnit 5 Java.
- Xóa toàn bộ file nguồn `.kt` trong backend.

## Không thay đổi

- Frontend Next.js/TypeScript.
- Mô hình ba vai trò.
- Ranh giới microservice và port 8080–8098.
- Dữ liệu/migration đã phát hành.

## Lưu ý build

`build.gradle.kts` vẫn được giữ vì đây là Gradle Kotlin DSL. Điều này không có nghĩa backend dùng Kotlin; mã runtime nằm trong `src/main/java`.
