package com.lmspilot.course.api;
import com.lmspilot.course.domain.*; import jakarta.validation.constraints.*; import java.time.Instant; import java.util.*;
public final class CourseModels { private CourseModels() {} }
record CategoryRequest(@NotBlank @Size(max=80) String code,@NotBlank @Size(max=180) String name,UUID parentId,Integer sortOrder) {}
record CategoryResponse(UUID id,String code,String name,UUID parentId,RecordStatus status,int sortOrder) {}
record CourseRequest(@NotBlank @Size(max=80) String code,@NotBlank @Size(max=240) String name,String description,String objectives,String targetAudience,@Min(0) Integer durationMinutes,@DecimalMin("0") @DecimalMax("100") Double passingScore,String completionPolicyJson,UUID categoryId) {}
record LessonRequest(@NotBlank @Size(max=220) String title,@NotNull LessonType type,String textContent,UUID fileId,Boolean required,@Min(0) Integer sortOrder,@Min(0) Integer estimatedMinutes) {}
record LessonResponse(UUID id,String title,LessonType type,String textContent,UUID fileId,boolean required,int sortOrder,int estimatedMinutes) {}
record CourseResponse(UUID id,String code,String name,String description,String objectives,String targetAudience,Integer durationMinutes,double passingScore,String completionPolicyJson,UUID categoryId,CourseStatus status,int contentVersion,int publishedVersion,boolean hasUnpublishedChanges,Instant publishedAt,UUID ownerId,List<LessonResponse> lessons) {}
record PageResponse<T>(List<T> items,int page,int size,long totalElements,int totalPages) {}
record PublicationStatus(UUID courseId,CourseStatus status,int contentVersion,int publishedVersion,boolean published) {}
record CourseLearningMetadata(UUID courseId,int version,String code,String name,CourseStatus status,List<LessonResponse> lessons) {}
record CourseDocumentScope(UUID courseId,Set<UUID> fileIds) {}
record CourseVersionSummary(UUID id,UUID courseId,int versionNumber,UUID createdBy,Instant createdAt) {}
record CourseSnapshot(UUID id,String code,String name,String description,String objectives,String targetAudience,Integer durationMinutes,double passingScore,String completionPolicyJson,UUID categoryId,int version,UUID ownerId,List<LessonResponse> lessons) {}
