# Course Service

Course Service quan ly khoa hoc, danh muc khoa hoc, bai hoc, phien ban publish va thao luan khoa hoc trong he thong LMSPilot.

Tai lieu nay la convention chinh cho `course-service`. Khi refactor cac service khac, hay giu cung tu duy: Clean Architecture, SOLID, Repository Pattern, CQRS, DTO/Mapper ro rang, Flyway quan ly database, Docker chay doc lap tung service.

## 1. Tong Quan

- Service: `course-service`
- Runtime: Java 21, Spring Boot 3.5.x
- Default internal port: `8083`
- Docker host port: `18083`
- Gateway host port: `18080`
- Database: PostgreSQL rieng cho course
- Migration: Flyway
- Message broker: RabbitMQ rieng cho course-only stack
- Auth: JWT stateless, dung chung `LMSPILOT_JWT_SECRET`

API chinh:

```text
/api/v1/categories/**
/api/v1/courses/**
/api/v1/discussions/**
/internal/v1/courses/**
```

## 2. Kien Truc

Course Service dang duoc to chuc theo Clean Architecture:

```text
com.lmspilot.course
+-- presentation
|   +-- controller
+-- application
|   +-- dto
|   |   +-- request
|   |   +-- response
|   |   +-- command
|   |   +-- query
|   +-- interfaces
|   |   +-- service
|   |   +-- repository
|   +-- mapper
|   +-- service
+-- domain
|   +-- model
|   +-- enums
+-- infrastructure
|   +-- config
|   +-- persistence
|   |   +-- entity
|   |   +-- jpaRepository
|   |   +-- mapper
|   +-- repository
+-- config
```

Huong phu thuoc bat buoc:

```text
presentation -> application -> domain
infrastructure -> application
infrastructure -> domain
```

Khong duoc lam:

```text
domain -> Spring/JPA/Web
application service -> JpaRepository truc tiep
controller -> repository truc tiep
presentation -> infrastructure truc tiep
```

## 3. Vai Tro Tung Tang

### Presentation

Thu muc:

```text
presentation/controller
```

Nhiem vu:

- Nhan HTTP request.
- Validate request bang annotation nhu `@Valid`, `@RequestBody`, `@PathVariable`.
- Goi mapper de doi request DTO sang command/query.
- Goi service interface trong application.
- Tra response DTO ra ngoai.

Controller khong duoc:

- Viet business logic.
- Goi repository.
- Dung JPA entity.
- Tu map persistence entity.

Vi du dung:

```java
return courses.create(courseMapper.toUpsertCommand(request));
```

### Application

Thu muc:

```text
application/service
application/interfaces/service
application/interfaces/repository
application/dto
application/mapper
```

Nhiem vu:

- Chua use case cua he thong.
- Dinh nghia service interface.
- Dinh nghia repository interface.
- Dieu phoi domain model va repository.
- Nhan command/query, tra response/result DTO.

Application service chi phu thuoc vao:

- Domain model.
- Domain enum.
- Application repository interface.
- Application DTO command/query/response.

Application service khong duoc phu thuoc vao:

- `JpaRepository`
- `EntityManager`
- JPA entity trong `infrastructure/persistence/entity`
- Controller/request web details.

### Domain

Thu muc:

```text
domain/model
domain/enums
```

Nhiem vu:

- Chua model nghiep vu thuan Java.
- Chua enum nghiep vu.
- Bao ve business invariant neu co the.

Domain khong duoc import:

```text
jakarta.persistence.*
org.springframework.*
org.hibernate.*
```

Ten domain model khong ket thuc bang `Entity`.

Dung:

```text
Course
Lesson
CourseCategory
DiscussionThread
```

Khong dung trong domain:

```text
CourseEntity
LessonEntity
CourseRepository extends JpaRepository
```

### Infrastructure

Thu muc:

```text
infrastructure/persistence/entity
infrastructure/persistence/jpaRepository
infrastructure/persistence/mapper
infrastructure/repository
infrastructure/config
```

Nhiem vu:

- Chua JPA entity.
- Chua Spring Data repository.
- Chua repository adapter implement application repository interface.
- Chua mapper domain <-> persistence entity.
- Chua config ky thuat nhu OpenAPI, persistence config.

## 4. CQRS Convention

Course Service dung CQRS o muc application DTO:

```text
command = them/sua/xoa/thay doi trang thai
query   = lay du lieu
```

Thu muc:

```text
application/dto/command
application/dto/query
```

Command hien co:

```text
CategoryCommand
CourseCommand
LessonCommand
DiscussionCommand
```

Query hien co:

```text
CourseSearchQuery
CourseVersionQuery
```

Quy tac:

- API `POST`, `PUT`, `PATCH`, `DELETE` phai map sang `Command`.
- API `GET` phai map sang `Query` neu co nhieu tham so loc/tim kiem/version.
- Controller khong truyen request DTO truc tiep vao service.
- Service khong nhan request DTO cua REST layer.

Flow chuan cho write API:

```text
HTTP Request
-> Request DTO
-> DtoMapper
-> Command
-> Application Service Interface
-> Application Service Impl
-> Domain Model
-> Repository Interface
-> Repository Adapter
-> JPA Repository
-> Database
```

Flow chuan cho read API:

```text
HTTP Request Params
-> DtoMapper
-> Query
-> Application Service Interface
-> Repository Interface
-> Repository Adapter
-> Response DTO
-> HTTP Response
```

## 5. DTO Convention

DTO duoc chia ro:

```text
application/dto/request
application/dto/response
application/dto/command
application/dto/query
```

### Request DTO

Dung cho input tu HTTP API.

Vi du:

```text
CourseRequest
LessonRequest
CategoryRequest
CreateDiscussionThreadRequest
```

Request DTO co the chua validation annotation:

```java
public record CourseRequest(
    @NotBlank String code,
    @NotBlank String name
) {}
```

### Response DTO

Dung cho output tra ve client.

Vi du:

```text
CourseResponse
LessonResponse
CategoryResponse
PageResponse
DiscussionThreadResponse
```

### Command DTO

Dung cho use case ghi du lieu.

Vi du:

```java
public final class CourseCommand {
    public record Upsert(...) {}
    public record TransitionStatus(CourseStatus status) {}
}
```

### Query DTO

Dung cho use case doc du lieu.

Vi du:

```java
public record CourseSearchQuery(
    String query,
    CourseStatus status,
    UUID categoryId,
    int page,
    int size
) {}
```

## 6. Mapper Convention

Mapper la bat buoc khi di qua ranh gioi tang.

Application REST mapper:

```text
application/mapper/CategoryDtoMapper.java
application/mapper/CourseDtoMapper.java
application/mapper/LessonDtoMapper.java
application/mapper/DiscussionDtoMapper.java
```

Persistence mapper:

```text
infrastructure/persistence/mapper/CoursePersistenceMapper.java
```

Quy tac:

- Controller dung mapper de doi request -> command/query.
- Repository adapter dung mapper de doi entity -> domain va domain -> entity.
- Khong map lung tung trong controller.
- Khong de JPA entity ro ri len application service.

## 7. Repository Pattern

Application khai bao repository interface:

```text
application/interfaces/repository/ICourseRepository.java
application/interfaces/repository/ILessonRepository.java
application/interfaces/repository/ICourseCategoryRepository.java
```

Infrastructure implement repository:

```text
infrastructure/repository/CourseRepositoryAdapter.java
infrastructure/repository/LessonRepositoryAdapter.java
infrastructure/repository/CourseCategoryRepositoryAdapter.java
```

Spring Data JPA chi nam o infrastructure:

```text
infrastructure/persistence/jpaRepository/CourseJpaRepository.java
```

Quy tac Dependency Inversion:

```text
Application Service -> ICourseRepository
Infrastructure Adapter -> implements ICourseRepository
```

Application service khong biet `JpaRepository` ton tai.

## 8. SOLID Rules

### S - Single Responsibility

Moi class chi nen co mot ly do de thay doi.

- Controller: HTTP.
- Mapper: chuyen doi du lieu.
- Service: use case.
- Repository interface: contract truy cap data.
- Repository adapter: ket noi contract voi JPA.
- Domain model: nghiep vu cot loi.

### O - Open/Closed

Them behavior moi bang service/use case/mapper moi khi hop ly, han che sua lan sang nhieu class khong lien quan.

### L - Liskov Substitution

Repository adapter phai ton trong contract cua repository interface. Service dung interface nao thi adapter thay the duoc interface do.

### I - Interface Segregation

Khong gom tat ca vao mot service lon.

Hien dang tach theo interface:

```text
ICategoryService
ICourseService
ILessonService
IDiscussionService
ICourseInternalQueryService
```

### D - Dependency Inversion

Tang application phu thuoc vao abstraction, khong phu thuoc vao framework persistence.

Dung:

```java
private final ICourseRepository courses;
```

Khong dung trong application service:

```java
private final CourseJpaRepository courses;
```

## 9. Database Va Flyway

Course Service dung Flyway lam source of truth cho schema.

Migration nam tai:

```text
src/main/resources/db/migration
```

Cau hinh:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Y nghia:

- Hibernate khong tu tao bang.
- Flyway tao/cap nhat bang khi service start.
- Hibernate chi validate entity co khop schema khong.

Khi chay Docker:

```text
course-db start
course-service start
Flyway chay migration
Bang duoc tao/cap nhat trong PostgreSQL container
Hibernate validate schema
Service ready
```

Neu them/sua bang, tao migration moi:

```text
src/main/resources/db/migration/V7__your_change_name.sql
```

Khong sua migration da release neu DB da tung chay migration do.

Bang course service dang so huu:

```text
course_categories
course_document_links
course_versions
courses
demo_seed_history
discussion_posts
discussion_threads
lessons
```

## 10. Auth Va Gateway

Course Service dang dung JWT stateless.

```text
Frontend -> API Gateway -> Course Service
```

Gateway route course:

```yaml
COURSE_URL: http://course-service:8083
```

Course Service va API Gateway phai dung cung secret:

```text
LMSPILOT_JWT_SECRET
```

Course Service khong goi Identity Service qua gRPC trong kien truc hien tai. Token do `identity-service` cap, gateway va course-service tu validate JWT bang shared secret.

Khi test API can auth:

```text
Authorization: Bearer <access_token>
```

Swagger da co OpenAPI security scheme Bearer JWT.

## 11. Khi Nao Dung gRPC

Khong dung gRPC cho:

```text
Frontend -> Gateway -> Service
```

Nen dung REST/HTTP JSON cho frontend va Swagger.

Dung gRPC khi service noi bo can goi nhau:

```text
learning-service -> course-service
enrollment-service -> course-service
certificate-service -> learning-service
assessment-service -> course-service
```

Vi du hop ly:

- Learning service lay metadata khoa hoc.
- Enrollment service check course da publish chua.
- Certificate service check learner da hoan thanh course chua.
- Assessment service lay context lesson/course.

Course -> Identity chi can gRPC neu course-service muon check user/role realtime. Neu JWT da co `userId`, `roles`, `permissions` thi chua can.

## 12. Docker Course-Only Stack

File compose nam tai:

```text
backend/services/course-service/docker-compose.yml
```

Stack hien tai gom:

```text
course-db
course-rabbitmq
course-service
api-gateway
```

Port tren may host:

```text
course-db:       localhost:15432
course-rabbitmq: localhost:15683
course-service:  localhost:18083
api-gateway:     localhost:18080
```

Docker image cua service dung:

```text
backend/Dockerfile.runtime
```

Ly do co `Dockerfile.runtime`:

- Gradle build jar tren host Windows.
- Docker chi copy jar vao runtime image.
- Tranh loi Docker build khong tai duoc Gradle tu `services.gradle.org`.

## 13. Cach Chay

### Lan dau hoac sau khi sua code Java

Build jar tren host:

```powershell
cd C:\Users\Admin\Desktop\Project_LMSPilot\LMSPilot\backend
.\gradlew.bat :services:course-service:bootJar :services:api-gateway:bootJar
```

Build va chay Docker:

```powershell
cd C:\Users\Admin\Desktop\Project_LMSPilot\LMSPilot\backend\services\course-service
docker compose up -d --build
```

### Sau khi tat may, khong sua code

Mo Docker Desktop, sau do:

```powershell
cd C:\Users\Admin\Desktop\Project_LMSPilot\LMSPilot\backend\services\course-service
docker compose up -d
```

Khong can `--build` neu khong sua code.

### Xem trang thai

```powershell
docker compose ps
```

### Xem log

```powershell
docker compose logs -f course-service
docker compose logs -f api-gateway
```

### Tat stack

```powershell
docker compose down
```

Neu muon xoa ca data volume:

```powershell
docker compose down -v
```

Can than: lenh `-v` se xoa database data cua stack nay.

## 14. Swagger Va Test API

Swagger truc tiep course-service:

```text
http://localhost:18083/swagger-ui/index.html
```

Course API qua gateway:

```text
http://localhost:18080/api/v1/courses
```

Health check:

```text
http://localhost:18083/actuator/health
http://localhost:18080/actuator/health
```

Neu API can login, lay token tu `identity-service`, sau do bam Authorize trong Swagger:

```text
Bearer <access_token>
```

## 15. Quy Tac Khi Viet Chuc Nang Moi

Khi them mot chuc nang moi, lam theo thu tu:

1. Xac dinh API la command hay query.
2. Tao request DTO neu API nhan body.
3. Tao command/query DTO trong application.
4. Them method vao service interface phu hop.
5. Implement use case trong application service.
6. Neu can DB, them method vao repository interface.
7. Implement repository adapter trong infrastructure.
8. Neu can schema moi, them Flyway migration moi.
9. Them mapper request -> command/query va domain/entity neu can.
10. Controller chi goi mapper va service interface.
11. Chay compile/test.

Checklist nhanh:

```text
[ ] Controller khong co business logic
[ ] Controller khong goi repository
[ ] Service nhan Command/Query, khong nhan Request DTO
[ ] Application service chi dung repository interface
[ ] Domain khong import Spring/JPA
[ ] JPA entity nam trong infrastructure
[ ] Spring Data repository nam trong infrastructure
[ ] Co mapper ro rang
[ ] Co migration Flyway neu doi database
[ ] Compile thanh cong
```

## 16. Lenh Kiem Tra

Compile course-service:

```powershell
cd C:\Users\Admin\Desktop\Project_LMSPilot\LMSPilot\backend
.\gradlew.bat :services:course-service:compileJava
```

Build jar course-service va gateway:

```powershell
.\gradlew.bat :services:course-service:bootJar :services:api-gateway:bootJar
```

Build Docker image:

```powershell
cd C:\Users\Admin\Desktop\Project_LMSPilot\LMSPilot\backend\services\course-service
docker compose build
```

Run Docker stack:

```powershell
docker compose up -d
```

## 17. Trang Thai Refactor Hien Tai

Da hoan thanh:

```text
[x] Tach presentation/controller
[x] Tach application/service
[x] Tach application/interfaces/service
[x] Tach application/interfaces/repository
[x] Tach application/dto/request
[x] Tach application/dto/response
[x] Them application/dto/command
[x] Them application/dto/query
[x] Them application/mapper
[x] Tach domain/model
[x] Tach domain/enums
[x] Tach JPA entity sang infrastructure/persistence/entity
[x] Tach Spring Data repository sang infrastructure/persistence/jpaRepository
[x] Them repository adapter trong infrastructure/repository
[x] Them persistence mapper
[x] Them OpenAPI config
[x] Them Dockerfile.runtime
[x] Compose course-only chay duoc course-service va api-gateway
[x] Gradle compile thanh cong
[x] Docker compose start thanh cong
```

Can cai tien tiep neu muon chuan hon nua:

```text
[ ] Tach CourseManagementServiceImpl thanh cac service nho hon: CategoryServiceImpl, CourseServiceImpl, LessonServiceImpl, CoursePublicationServiceImpl
[ ] Dua business invariant tu service vao domain method/policy
[ ] Tao exception rieng cho domain thay vi nem ApiException truc tiep trong service
[ ] Bo sung unit test cho domain va application service
[ ] Them identity-service vao compose neu muon login lay JWT bang Docker
```
