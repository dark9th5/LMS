package com.lmspilot.course.infrastructure.persistence.mapper;

import com.lmspilot.course.domain.model.Course;
import com.lmspilot.course.domain.model.CourseCategory;
import com.lmspilot.course.domain.model.CourseVersion;
import com.lmspilot.course.domain.model.DiscussionPost;
import com.lmspilot.course.domain.model.DiscussionThread;
import com.lmspilot.course.domain.model.Lesson;
import com.lmspilot.course.infrastructure.persistence.entity.CourseCategoryEntity;
import com.lmspilot.course.infrastructure.persistence.entity.CourseEntity;
import com.lmspilot.course.infrastructure.persistence.entity.CourseVersionEntity;
import com.lmspilot.course.infrastructure.persistence.entity.DiscussionPostEntity;
import com.lmspilot.course.infrastructure.persistence.entity.DiscussionThreadEntity;
import com.lmspilot.course.infrastructure.persistence.entity.LessonEntity;
import org.springframework.stereotype.Component;

@Component
public class CoursePersistenceMapper {
    // This mapper is the boundary between clean domain models and JPA persistence
    // entities.
    public Course toDomain(CourseEntity entity) {
        Course out = new Course();
        out.id = entity.id;
        out.code = entity.code;
        out.name = entity.name;
        out.description = entity.description;
        out.objectives = entity.objectives;
        out.targetAudience = entity.targetAudience;
        out.durationMinutes = entity.durationMinutes;
        out.passingScore = entity.passingScore;
        out.completionPolicyJson = entity.completionPolicyJson;
        out.categoryId = entity.categoryId;
        out.status = entity.status;
        out.contentVersion = entity.contentVersion;
        out.publishedVersion = entity.publishedVersion;
        out.publishedAt = entity.publishedAt;
        out.publishedBy = entity.publishedBy;
        out.ownerId = entity.ownerId;
        out.createdAt = entity.createdAt;
        out.updatedAt = entity.updatedAt;
        out.rowVersion = entity.rowVersion;
        return out;
    }

    public CourseEntity toEntity(Course domain) {
        CourseEntity out = new CourseEntity();
        out.id = domain.id;
        out.code = domain.code;
        out.name = domain.name;
        out.description = domain.description;
        out.objectives = domain.objectives;
        out.targetAudience = domain.targetAudience;
        out.durationMinutes = domain.durationMinutes;
        out.passingScore = domain.passingScore;
        out.completionPolicyJson = domain.completionPolicyJson;
        out.categoryId = domain.categoryId;
        out.status = domain.status;
        out.contentVersion = domain.contentVersion;
        out.publishedVersion = domain.publishedVersion;
        out.publishedAt = domain.publishedAt;
        out.publishedBy = domain.publishedBy;
        out.ownerId = domain.ownerId;
        out.createdAt = domain.createdAt;
        out.updatedAt = domain.updatedAt;
        out.rowVersion = domain.rowVersion;
        return out;
    }

    public CourseCategory toDomain(CourseCategoryEntity entity) {
        CourseCategory out = new CourseCategory();
        out.id = entity.id;
        out.code = entity.code;
        out.name = entity.name;
        out.parentId = entity.parentId;
        out.status = entity.status;
        out.sortOrder = entity.sortOrder;
        out.createdAt = entity.createdAt;
        out.updatedAt = entity.updatedAt;
        out.version = entity.version;
        return out;
    }

    public CourseCategoryEntity toEntity(CourseCategory domain) {
        CourseCategoryEntity out = new CourseCategoryEntity();
        out.id = domain.id;
        out.code = domain.code;
        out.name = domain.name;
        out.parentId = domain.parentId;
        out.status = domain.status;
        out.sortOrder = domain.sortOrder;
        out.createdAt = domain.createdAt;
        out.updatedAt = domain.updatedAt;
        out.version = domain.version;
        return out;
    }

    public Lesson toDomain(LessonEntity entity) {
        Lesson out = new Lesson();
        out.id = entity.id;
        out.courseId = entity.courseId;
        out.title = entity.title;
        out.type = entity.type;
        out.textContent = entity.textContent;
        out.fileId = entity.fileId;
        out.required = entity.required;
        out.sortOrder = entity.sortOrder;
        out.estimatedMinutes = entity.estimatedMinutes;
        out.createdAt = entity.createdAt;
        out.updatedAt = entity.updatedAt;
        out.version = entity.version;
        return out;
    }

    public LessonEntity toEntity(Lesson domain) {
        LessonEntity out = new LessonEntity();
        out.id = domain.id;
        out.courseId = domain.courseId;
        out.title = domain.title;
        out.type = domain.type;
        out.textContent = domain.textContent;
        out.fileId = domain.fileId;
        out.required = domain.required;
        out.sortOrder = domain.sortOrder;
        out.estimatedMinutes = domain.estimatedMinutes;
        out.createdAt = domain.createdAt;
        out.updatedAt = domain.updatedAt;
        out.version = domain.version;
        return out;
    }

    public CourseVersion toDomain(CourseVersionEntity entity) {
        CourseVersion out = new CourseVersion();
        out.id = entity.id;
        out.courseId = entity.courseId;
        out.versionNumber = entity.versionNumber;
        out.snapshotJson = entity.snapshotJson;
        out.createdBy = entity.createdBy;
        out.createdAt = entity.createdAt;
        return out;
    }

    public CourseVersionEntity toEntity(CourseVersion domain) {
        CourseVersionEntity out = new CourseVersionEntity();
        out.id = domain.id;
        out.courseId = domain.courseId;
        out.versionNumber = domain.versionNumber;
        out.snapshotJson = domain.snapshotJson;
        out.createdBy = domain.createdBy;
        out.createdAt = domain.createdAt;
        return out;
    }

    public DiscussionThread toDomain(DiscussionThreadEntity entity) {
        DiscussionThread out = new DiscussionThread();
        out.id = entity.id;
        out.courseId = entity.courseId;
        out.lessonId = entity.lessonId;
        out.title = entity.title;
        out.authorId = entity.authorId;
        out.status = entity.status;
        out.pinned = entity.pinned;
        out.postCount = entity.postCount;
        out.createdAt = entity.createdAt;
        out.updatedAt = entity.updatedAt;
        out.version = entity.version;
        return out;
    }

    public DiscussionThreadEntity toEntity(DiscussionThread domain) {
        DiscussionThreadEntity out = new DiscussionThreadEntity();
        out.id = domain.id;
        out.courseId = domain.courseId;
        out.lessonId = domain.lessonId;
        out.title = domain.title;
        out.authorId = domain.authorId;
        out.status = domain.status;
        out.pinned = domain.pinned;
        out.postCount = domain.postCount;
        out.createdAt = domain.createdAt;
        out.updatedAt = domain.updatedAt;
        out.version = domain.version;
        return out;
    }

    public DiscussionPost toDomain(DiscussionPostEntity entity) {
        DiscussionPost out = new DiscussionPost();
        out.id = entity.id;
        out.threadId = entity.threadId;
        out.authorId = entity.authorId;
        out.parentPostId = entity.parentPostId;
        out.content = entity.content;
        out.status = entity.status;
        out.createdAt = entity.createdAt;
        out.updatedAt = entity.updatedAt;
        out.version = entity.version;
        return out;
    }

    public DiscussionPostEntity toEntity(DiscussionPost domain) {
        DiscussionPostEntity out = new DiscussionPostEntity();
        out.id = domain.id;
        out.threadId = domain.threadId;
        out.authorId = domain.authorId;
        out.parentPostId = domain.parentPostId;
        out.content = domain.content;
        out.status = domain.status;
        out.createdAt = domain.createdAt;
        out.updatedAt = domain.updatedAt;
        out.version = domain.version;
        return out;
    }
}
