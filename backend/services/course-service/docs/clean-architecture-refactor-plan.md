# Course Service Clean Architecture Refactor Plan

## 1. Objective

Refactor `backend/services/course-service` from the current simple layered structure into a clean, SOLID-friendly architecture inspired by the reference banking project:

- `presentation/controller`
- `application/interfaces`
- `application/service`
- `application/dto`
- `application/mapper`
- `domain/model`
- `infrastructure/persistence/entity`
- `infrastructure/persistence/jpaRepository`
- `infrastructure/persistence/mapper`
- `infrastructure/repository`
- `infrastructure/config`

The target architecture keeps the service deployable as a Spring Boot microservice, keeps PostgreSQL schema changes under Flyway, and prevents domain/application code from depending directly on JPA entities or Spring Data repositories.

## 2. Current State

Current package layout:

```text
com.lmspilot.course
+-- api
|   +-- CategoryController
|   +-- CourseController
|   +-- CourseManagementService
|   +-- CourseModels
|   +-- DiscussionApi
|   +-- InternalCourseController
+-- config
|   +-- DevelopmentSeed
+-- domain
    +-- *Entity
    +-- *Repository extends JpaRepository
    +-- enums
```

Main issues:

- Domain package contains JPA entities and Spring Data repositories.
- `CourseManagementService` handles category, course, lesson, publication, version snapshot, and internal metadata use cases in one class.
- Entity fields are public and business invariants are mostly enforced outside the domain.
- API DTOs, service results, and internal API models are grouped together in `CourseModels`.
- Repositories expose `JpaRepository` directly to application logic.
- Mapping is mostly inline in services.

## 3. Target Dependency Rule

Allowed dependencies:

```text
presentation -> application -> domain
infrastructure -> application
infrastructure -> domain
config -> application/infrastructure
```

Forbidden dependencies:

```text
domain -> Spring / JPA / Jackson / Web
application -> infrastructure persistence entities
application -> Spring Data JpaRepository
presentation -> infrastructure
```

Spring annotations should mostly live in:

- `presentation`
- `application/service`
- `infrastructure`
- `config`

Domain models should be plain Java.

## 4. Target Package Structure

```text
com.lmspilot.course
+-- CourseServiceApplication.java
+-- common
|   +-- exception
|   |   +-- CourseDomainException.java
|   |   +-- CourseNotFoundException.java
|   |   +-- CourseConflictException.java
|   +-- result
|       +-- PageResult.java
|       +-- OperationResult.java
+-- domain
|   +-- model
|   |   +-- Course.java
|   |   +-- CourseCategory.java
|   |   +-- CourseVersion.java
|   |   +-- DiscussionPost.java
|   |   +-- DiscussionThread.java
|   |   +-- Lesson.java
|   +-- enums
|   |   +-- CourseStatus.java
|   |   +-- DiscussionPostStatus.java
|   |   +-- DiscussionThreadStatus.java
|   |   +-- LessonType.java
|   |   +-- RecordStatus.java
|   +-- policy
|       +-- CoursePublicationPolicy.java
|       +-- LessonFilePolicy.java
+-- application
|   +-- dto
|   |   +-- command
|   |   |   +-- CreateCategoryCommand.java
|   |   |   +-- CreateCourseCommand.java
|   |   |   +-- CreateDiscussionPostCommand.java
|   |   |   +-- CreateDiscussionThreadCommand.java
|   |   |   +-- CreateLessonCommand.java
|   |   |   +-- ModerateDiscussionThreadCommand.java
|   |   |   +-- TransitionCourseStatusCommand.java
|   |   |   +-- UpdateCategoryCommand.java
|   |   |   +-- UpdateCourseCommand.java
|   |   |   +-- UpdateLessonCommand.java
|   |   +-- query
|   |   |   +-- CourseSearchQuery.java
|   |   |   +-- CourseVersionQuery.java
|   |   +-- result
|   |       +-- CategoryResult.java
|   |       +-- CourseDocumentScopeResult.java
|   |       +-- CourseLearningMetadataResult.java
|   |       +-- CoursePublicationResult.java
|   |       +-- CourseResult.java
|   |       +-- CourseVersionResult.java
|   |       +-- DiscussionPostResult.java
|   |       +-- DiscussionThreadResult.java
|   |       +-- LessonResult.java
|   +-- interfaces
|   |   +-- repository
|   |   |   +-- ICourseCategoryRepository.java
|   |   |   +-- ICourseRepository.java
|   |   |   +-- ICourseVersionRepository.java
|   |   |   +-- IDiscussionPostRepository.java
|   |   |   +-- IDiscussionThreadRepository.java
|   |   |   +-- ILessonRepository.java
|   |   +-- service
|   |       +-- ICategoryService.java
|   |       +-- ICourseInternalQueryService.java
|   |       +-- ICoursePublicationService.java
|   |       +-- ICourseService.java
|   |       +-- IDiscussionService.java
|   |       +-- ILessonService.java
|   +-- mapper
|   |   +-- CategoryDtoMapper.java
|   |   +-- CourseDtoMapper.java
|   |   +-- DiscussionDtoMapper.java
|   |   +-- LessonDtoMapper.java
|   +-- service
|   |   +-- CategoryServiceImpl.java
|   |   +-- CourseInternalQueryServiceImpl.java
|   |   +-- CoursePublicationServiceImpl.java
|   |   +-- CourseServiceImpl.java
|   |   +-- DiscussionServiceImpl.java
|   |   +-- LessonServiceImpl.java
|   +-- validator
|       +-- CategoryValidator.java
|       +-- CourseValidator.java
|       +-- DiscussionValidator.java
|       +-- LessonValidator.java
+-- infrastructure
|   +-- config
|   |   +-- CourseApplicationConfig.java
|   |   +-- CoursePersistenceConfig.java
|   |   +-- DevelopmentSeed.java
|   +-- persistence
|   |   +-- entity
|   |   |   +-- CourseCategoryEntity.java
|   |   |   +-- CourseEntity.java
|   |   |   +-- CourseVersionEntity.java
|   |   |   +-- DiscussionPostEntity.java
|   |   |   +-- DiscussionThreadEntity.java
|   |   |   +-- LessonEntity.java
|   |   +-- jpaRepository
|   |   |   +-- CourseCategoryJpaRepository.java
|   |   |   +-- CourseJpaRepository.java
|   |   |   +-- CourseVersionJpaRepository.java
|   |   |   +-- DiscussionPostJpaRepository.java
|   |   |   +-- DiscussionThreadJpaRepository.java
|   |   |   +-- LessonJpaRepository.java
|   |   +-- mapper
|   |       +-- CourseCategoryEntityMapper.java
|   |       +-- CourseEntityMapper.java
|   |       +-- CourseVersionEntityMapper.java
|   |       +-- DiscussionEntityMapper.java
|   |       +-- LessonEntityMapper.java
|   +-- repository
|       +-- CourseCategoryRepositoryImpl.java
|       +-- CourseRepositoryImpl.java
|       +-- CourseVersionRepositoryImpl.java
|       +-- DiscussionPostRepositoryImpl.java
|       +-- DiscussionThreadRepositoryImpl.java
|       +-- LessonRepositoryImpl.java
+-- presentation
    +-- controller
    |   +-- CategoryController.java
    |   +-- CourseController.java
    |   +-- DiscussionController.java
    |   +-- InternalCourseController.java
    +-- dto
    |   +-- request
    |   |   +-- CreateCategoryRequestDto.java
    |   |   +-- CreateCourseRequestDto.java
    |   |   +-- CreateDiscussionPostRequestDto.java
    |   |   +-- CreateDiscussionThreadRequestDto.java
    |   |   +-- CreateLessonRequestDto.java
    |   |   +-- ModerateDiscussionThreadRequestDto.java
    |   |   +-- TransitionCourseStatusRequestDto.java
    |   |   +-- UpdateCategoryRequestDto.java
    |   |   +-- UpdateCourseRequestDto.java
    |   |   +-- UpdateLessonRequestDto.java
    |   +-- response
    |       +-- ApiResponseDto.java
    |       +-- CategoryResponseDto.java
    |       +-- CourseDocumentScopeResponseDto.java
    |       +-- CourseLearningMetadataResponseDto.java
    |       +-- CoursePublicationResponseDto.java
    |       +-- CourseResponseDto.java
    |       +-- CourseVersionResponseDto.java
    |       +-- DiscussionPostResponseDto.java
    |       +-- DiscussionThreadResponseDto.java
    |       +-- LessonResponseDto.java
    |       +-- PageResponseDto.java
    +-- mapper
        +-- CategoryRestMapper.java
        +-- CourseRestMapper.java
        +-- DiscussionRestMapper.java
        +-- LessonRestMapper.java
```

## 5. Naming Rules

### Domain

- Domain model: `Course`, `Lesson`, `CourseCategory`.
- Domain enum: `CourseStatus`, `LessonType`.
- Domain policy: `CoursePublicationPolicy`, `LessonFilePolicy`.
- Domain models must not end with `Entity`.
- Domain models must not import `jakarta.persistence`, `org.springframework`, or web classes.

### Application

- Service interface: `ICourseService`, `ILessonService`.
- Service implementation: `CourseServiceImpl`, `LessonServiceImpl`.
- Repository interface: `ICourseRepository`, `ILessonRepository`.
- Incoming use case data: `CreateCourseCommand`, `UpdateLessonCommand`.
- Query object: `CourseSearchQuery`.
- Application output: `CourseResult`, `LessonResult`.
- Application mapper: `CourseDtoMapper`, `LessonDtoMapper`.

### Infrastructure

- JPA entity: `CourseEntity`, `LessonEntity`.
- Spring Data repository: `CourseJpaRepository`, `LessonJpaRepository`.
- Repository adapter: `CourseRepositoryImpl implements ICourseRepository`.
- Persistence mapper: `CourseEntityMapper`.
- Config: `CoursePersistenceConfig`.

### Presentation

- REST controller: `CourseController`.
- Request DTO: `CreateCourseRequestDto`.
- Response DTO: `CourseResponseDto`.
- REST mapper: `CourseRestMapper`.

## 6. Repository Pattern Rule

Application services must depend on repository interfaces:

```java
public interface ICourseRepository {
    Course save(Course course);
    Optional<Course> findById(UUID id);
    Optional<Course> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    PageResult<Course> search(CourseSearchQuery query);
}
```

Infrastructure owns Spring Data:

```java
public interface CourseJpaRepository extends JpaRepository<CourseEntity, UUID> {
    boolean existsByCodeIgnoreCase(String code);
}
```

Adapter bridges the two:

```java
@Repository
public class CourseRepositoryImpl implements ICourseRepository {
    private final CourseJpaRepository jpaRepository;
    private final CourseEntityMapper mapper;

    public Course save(Course course) {
        CourseEntity saved = jpaRepository.save(mapper.toEntity(course));
        return mapper.toDomain(saved);
    }
}
```

## 7. DTO and Mapping Flow

Use this flow for write APIs:

```text
CreateCourseRequestDto
    -> CourseRestMapper
CreateCourseCommand
    -> CourseServiceImpl
Course domain model
    -> ICourseRepository
CourseResult
    -> CourseRestMapper
CourseResponseDto
```

Use this flow for persistence:

```text
Course domain model
    -> CourseEntityMapper
CourseEntity
    -> CourseJpaRepository
CourseEntity
    -> CourseEntityMapper
Course domain model
```

Avoid mapping inside controllers and repository adapters except through dedicated mapper classes.

## 8. Suggested Service Split

Replace `CourseManagementService` with:

- `CategoryServiceImpl`
  - list categories
  - create category
  - update category
  - deactivate category

- `CourseServiceImpl`
  - search courses
  - get course detail
  - create course
  - update course
  - archive course

- `LessonServiceImpl`
  - add lesson
  - update lesson
  - delete lesson

- `CoursePublicationServiceImpl`
  - transition status
  - publish course
  - create immutable version snapshot
  - list versions
  - get version

- `CourseInternalQueryServiceImpl`
  - publication status
  - document scope
  - learning metadata

- `DiscussionServiceImpl`
  - list threads
  - create thread
  - get thread
  - reply
  - moderate thread
  - delete post

## 9. Domain Model Rules

Move business rules from application services into domain methods where possible.

Example target shape:

```java
public class Course {
    private UUID id;
    private String code;
    private String name;
    private CourseStatus status;
    private int contentVersion;
    private int publishedVersion;

    public void updateDetails(...) {
        ensureEditable();
        // update fields
        markContentChanged();
    }

    public void publish(int lessonCount, UUID publishedBy) {
        if (lessonCount == 0) {
            throw new CourseDomainException("Course must have at least one lesson");
        }
        this.status = CourseStatus.PUBLISHED;
        this.publishedVersion = this.contentVersion;
        // set publication metadata
    }

    public void archive() {
        this.status = CourseStatus.ARCHIVED;
    }

    private void ensureEditable() {
        if (status == CourseStatus.ARCHIVED) {
            throw new CourseDomainException("Archived course cannot be changed");
        }
    }
}
```

Application services orchestrate. Domain models protect invariants.

## 10. Flyway and Database Rule

Keep:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Rules:

- Never rely on Hibernate to generate production tables.
- Never edit released migration files.
- Any schema change must add a new Flyway migration:

```text
src/main/resources/db/migration/V7__describe_change.sql
```

When running Docker:

```text
course-db starts
course-service starts
Flyway runs migrations
tables are created/updated
Hibernate validates JPA entities against tables
service becomes ready
```

So tables are auto-created in Docker by Flyway, not by domain classes and not by Hibernate DDL generation.

## 11. Refactor Phases

### Phase 1 - Create Package Skeleton

- Add new package directories.
- Move existing enums into `domain/enums`.
- Move existing JPA entities into `infrastructure/persistence/entity`.
- Move existing Spring Data repositories into `infrastructure/persistence/jpaRepository`.
- Update imports only; preserve behavior.
- Run compile/tests.

### Phase 2 - Introduce Domain Models

- Add `Course`, `Lesson`, `CourseCategory`, `CourseVersion`, `DiscussionThread`, `DiscussionPost`.
- Keep IDs and timestamps compatible with current database.
- Move simple invariants into domain methods.
- Do not remove JPA entities yet.

### Phase 3 - Add Repository Interfaces and Adapters

- Create `I*Repository` interfaces in `application/interfaces/repository`.
- Create `*RepositoryImpl` adapters in `infrastructure/repository`.
- Create `*EntityMapper` classes.
- Change application services to use interfaces instead of `JpaRepository`.

### Phase 4 - Split Application Services

- Replace `CourseManagementService` gradually.
- First extract category use cases.
- Then lesson use cases.
- Then publication/version use cases.
- Then internal query use cases.
- Keep endpoint responses backward-compatible.

### Phase 5 - Split DTOs

- Replace `CourseModels` with request/response/command/result files.
- Keep JSON field names stable.
- Add REST mappers from request to command and result to response.

### Phase 6 - Discussion Refactor

- Split `DiscussionApi.java`, which currently contains request records, response records, service, and controller in one file.
- Move controller to `presentation/controller/DiscussionController.java`.
- Move DTOs to `presentation/dto`.
- Move service to `application/service/DiscussionServiceImpl.java`.
- Move repository access behind interfaces.

### Phase 7 - Validation and Error Handling

- Move reusable validation to `application/validator`.
- Keep HTTP status translation in global exception handling.
- Domain throws domain exceptions, not HTTP exceptions.
- Application can translate domain exceptions into application exceptions.
- Presentation/common layer maps exceptions to API error response.

### Phase 8 - Tests

Add focused tests per layer:

- Domain tests for publication, archive, lesson file requirement, discussion lock.
- Application service tests with fake repository implementations.
- Repository adapter tests with JPA slice if available.
- Controller tests for request/response compatibility.
- Flyway migration validation test on clean PostgreSQL or Testcontainers if allowed.

### Phase 9 - Docker Verification

Run:

```powershell
docker compose -f docker-compose.course-service.yml up -d --build
```

Check:

```powershell
curl http://localhost:18083/actuator/health
curl http://localhost:18080/actuator/health
```

Confirm tables:

```powershell
docker compose -f docker-compose.course-service.yml exec course-db psql -U course_user -d course_db -c "\dt"
```

## 12. Compatibility Constraints

Keep these API paths unchanged:

```text
/api/v1/categories/**
/api/v1/courses/**
/api/v1/discussions/**
/internal/v1/courses/**
```

Keep database table names unchanged unless a migration explicitly changes them:

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

Keep gateway route unchanged:

```text
COURSE_URL=http://course-service:8083
```

## 13. Definition of Done

- Domain package has no Spring/JPA imports.
- Application services do not import `JpaRepository` or persistence entities.
- Controllers do not call repositories.
- Repository adapters are the only classes that call Spring Data JPA repositories.
- Mappers exist for REST DTOs and persistence entities.
- `CourseModels.java` is removed or reduced to transitional compatibility only.
- `CourseManagementService` is removed after service split.
- Flyway migrations remain the source of truth for schema.
- Docker course-only stack starts successfully.
- Gateway routes to course through `COURSE_URL`.
