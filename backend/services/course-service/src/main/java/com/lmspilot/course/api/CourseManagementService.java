package com.lmspilot.course.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.lmspilot.course.domain.*;

import com.lmspilot.support.api.ApiException;

import com.lmspilot.support.security.CurrentUser;

import java.time.Instant;

import java.util.*;

import org.springframework.data.domain.*;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import static com.lmspilot.course.api.CourseModels.*;
@Service
@Transactional
public class CourseManagementService {
    private final CourseCategoryRepository categories;
    private final CourseRepository courses;
    private final LessonRepository lessons;
    private final CourseVersionRepository versions;
    private final ObjectMapper mapper;
    public CourseManagementService(CourseCategoryRepository categories,CourseRepository courses,LessonRepository lessons,CourseVersionRepository versions,ObjectMapper mapper){
        this.categories=categories;
        this.courses=courses;
        this.lessons=lessons;
        this.versions=versions;
        this.mapper=mapper;
    }
    @Transactional(readOnly=true)
    public List<CategoryResponse> categories(){
        return categories.findAllByOrderBySortOrderAscNameAsc().stream().map(this::category).toList();
    }
    public CategoryResponse createCategory(CategoryRequest in){
        String code=normCode(in.code());
        if(categories.existsByCodeIgnoreCase(code)) conflict("Mã danh mục đã tồn tại");
        CourseCategoryEntity e=new CourseCategoryEntity();
        e.code=code;
        e.name=in.name().trim();
        e.parentId=in.parentId();
        e.sortOrder=in.sortOrder()==null?0:in.sortOrder();
        return category(categories.save(e));
    }
    public CategoryResponse updateCategory(UUID id,CategoryRequest in){
        CourseCategoryEntity e=categories.findById(id).orElseThrow(this::notFoundCategory);
        String code=normCode(in.code());
        if(!e.code.equalsIgnoreCase(code)&&categories.existsByCodeIgnoreCase(code)) conflict("Mã danh mục đã tồn tại");
        e.code=code;
        e.name=in.name().trim();
        e.parentId=in.parentId();
        e.sortOrder=in.sortOrder()==null?e.sortOrder:in.sortOrder();
        e.updatedAt=Instant.now();
        return category(categories.save(e));
    }
    public CategoryResponse deactivateCategory(UUID id){
        CourseCategoryEntity e=categories.findById(id).orElseThrow(this::notFoundCategory);
        e.status=RecordStatus.INACTIVE;
        e.updatedAt=Instant.now();
        return category(categories.save(e));
    }
    @Transactional(readOnly=true)
    public PageResponse<CourseResponse> search(String query,CourseStatus status,UUID categoryId,int page,int size){
        int safe=Math.max(1,Math.min(100,size));
        Page<CourseEntity> p=courses.search(blankToNull(query),status,categoryId,PageRequest.of(Math.max(0,page),safe,Sort.by(Sort.Direction.DESC,"updatedAt")));
        return new PageResponse<>(p.getContent().stream().map(e->response(e,List.of())).toList(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());
    }
    @Transactional(readOnly=true)
    public CourseResponse get(UUID id){
        CourseEntity e=requireCourse(id);
        return response(e,lessonResponses(id));
    }
    public CourseResponse create(CourseRequest in){
        String code=normCode(in.code());
        if(courses.existsByCodeIgnoreCase(code)) conflict("Mã khóa học đã tồn tại");
        CourseEntity e=new CourseEntity();
        apply(e,in);
        e.code=code;
        e.ownerId=currentUser();
        return response(courses.save(e),List.of());
    }
    public CourseResponse update(UUID id,CourseRequest in){
        CourseEntity e=requireCourse(id);
        if(e.status==CourseStatus.ARCHIVED) conflict("Không thể sửa khóa học đã lưu trữ");
        String code=normCode(in.code());
        if(!e.code.equalsIgnoreCase(code)&&courses.existsByCodeIgnoreCase(code)) conflict("Mã khóa học đã tồn tại");
        apply(e,in);
        e.code=code;
        e.contentVersion++;
        e.updatedAt=Instant.now();
        return response(courses.save(e),lessonResponses(id));
    }
    public LessonResponse addLesson(UUID courseId,LessonRequest in){
        CourseEntity c=requireCourse(courseId);
        ensureEditable(c);
        LessonEntity e=new LessonEntity();
        e.courseId=courseId;
        apply(e,in);
        if(in.sortOrder()==null)e.sortOrder=(int)lessons.countByCourseId(courseId);
        lessons.save(e);
        touch(c);
        return lesson(e);
    }
    public LessonResponse updateLesson(UUID courseId,UUID lessonId,LessonRequest in){
        CourseEntity c=requireCourse(courseId);
        ensureEditable(c);
        LessonEntity e=lessons.findById(lessonId).filter(x->courseId.equals(x.courseId)).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"LESSON_NOT_FOUND","Không tìm thấy bài học"));
        apply(e,in);
        e.updatedAt=Instant.now();
        lessons.save(e);
        touch(c);
        return lesson(e);
    }
    public void deleteLesson(UUID courseId,UUID lessonId){
        CourseEntity c=requireCourse(courseId);
        ensureEditable(c);
        LessonEntity e=lessons.findById(lessonId).filter(x->courseId.equals(x.courseId)).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"LESSON_NOT_FOUND","Không tìm thấy bài học"));
        lessons.delete(e);
        touch(c);
    }
    public void archive(UUID id){
        CourseEntity e=requireCourse(id);
        e.status=CourseStatus.ARCHIVED;
        e.updatedAt=Instant.now();
        courses.save(e);
    }
    public CourseResponse transition(UUID id,CourseStatus target){
        CourseEntity e=requireCourse(id);
        if(target==CourseStatus.PUBLISHED){
            if(lessons.countByCourseId(id)==0) conflict("Khóa học phải có ít nhất một bài học");
            e.status=target;
            e.publishedVersion=e.contentVersion;
            e.publishedAt=Instant.now();
            e.publishedBy=currentUser();
            saveSnapshot(e);
        }
        else if(target==CourseStatus.HIDDEN||target==CourseStatus.DRAFT||target==CourseStatus.ARCHIVED){
            e.status=target;
        }
        else conflict("Chuyển trạng thái không hợp lệ");
        e.updatedAt=Instant.now();
        courses.save(e);
        return response(e,lessonResponses(id));
    }
    @Transactional(readOnly=true)
    public List<CourseVersionSummary> versions(UUID id){
        requireCourse(id);
        return versions.findAllByCourseIdOrderByVersionNumberDesc(id).stream().map(v->new CourseVersionSummary(v.id,v.courseId,v.versionNumber,v.createdBy,v.createdAt)).toList();
    }
    @Transactional(readOnly=true)
    public CourseResponse version(UUID id,int version){
        CourseEntity e=requireCourse(id);
        CourseVersionEntity v=versions.findByCourseIdAndVersionNumber(id,version).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"COURSE_VERSION_NOT_FOUND","Không tìm thấy phiên bản khóa học"));
        try{
            CourseSnapshot s=mapper.readValue(v.snapshotJson,CourseSnapshot.class);
            return new CourseResponse(s.id(),s.code(),s.name(),s.description(),s.objectives(),s.targetAudience(),s.durationMinutes(),s.passingScore(),s.completionPolicyJson(),s.categoryId(),e.status,s.version(),e.publishedVersion,false,e.publishedAt,s.ownerId(),s.lessons());
        }
        catch(Exception ex){
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"COURSE_VERSION_INVALID","Không đọc được phiên bản khóa học");
        }

    }
    @Transactional(readOnly=true)
    public PublicationStatus publication(UUID id){
        CourseEntity e=requireCourse(id);
        return new PublicationStatus(id,e.status,e.contentVersion,e.publishedVersion,e.status==CourseStatus.PUBLISHED);
    }
    @Transactional(readOnly=true)
    public CourseDocumentScope documentScope(UUID id){
        requireCourse(id);
        Set<UUID> ids=new LinkedHashSet<>();
        for(LessonEntity l:lessons.findAllByCourseIdOrderBySortOrderAsc(id))if(l.fileId!=null)ids.add(l.fileId);
        return new CourseDocumentScope(id,Set.copyOf(ids));
    }
    @Transactional(readOnly=true)
    public CourseLearningMetadata learningMetadata(UUID id,Integer version){
        CourseResponse r=version==null?get(id):version(id,version);
        return new CourseLearningMetadata(id,version==null?r.contentVersion():version,r.code(),r.name(),r.status(),r.lessons());
    }
    private void saveSnapshot(CourseEntity e){
        CourseSnapshot s=new CourseSnapshot(e.id,e.code,e.name,e.description,e.objectives,e.targetAudience,e.durationMinutes,e.passingScore,e.completionPolicyJson,e.categoryId,e.contentVersion,e.ownerId,lessonResponses(e.id));
        try{
            CourseVersionEntity v=versions.findByCourseIdAndVersionNumber(e.id,e.contentVersion).orElseGet(CourseVersionEntity::new);
            v.courseId=e.id;
            v.versionNumber=e.contentVersion;
            v.snapshotJson=mapper.writeValueAsString(s);
            v.createdBy=currentUser();
            versions.save(v);
        }
        catch(Exception ex){
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"COURSE_SNAPSHOT_FAILED","Không thể tạo phiên bản khóa học");
        }

    }
    private void apply(CourseEntity e,CourseRequest in){
        e.name=in.name().trim();
        e.description=in.description();
        e.objectives=in.objectives();
        e.targetAudience=in.targetAudience();
        e.durationMinutes=in.durationMinutes();
        e.passingScore=in.passingScore()==null?70:in.passingScore();
        e.completionPolicyJson=in.completionPolicyJson()==null||in.completionPolicyJson().isBlank()?"{\"requiredLessonPercent\":100}":in.completionPolicyJson();
        e.categoryId=in.categoryId();
    }
    private void apply(LessonEntity e,LessonRequest in){
        e.title=in.title().trim();
        e.type=in.type();
        e.textContent=in.textContent();
        e.fileId=in.fileId();
        e.required=in.required()==null||in.required();
        e.sortOrder=in.sortOrder()==null?e.sortOrder:in.sortOrder();
        e.estimatedMinutes=in.estimatedMinutes()==null?0:in.estimatedMinutes();
        if(Set.of(LessonType.PDF,LessonType.DOCX,LessonType.VIDEO,LessonType.AUDIO,LessonType.FILE).contains(e.type)&&e.fileId==null)throw new ApiException(HttpStatus.BAD_REQUEST,"LESSON_FILE_REQUIRED","Loại bài học này cần tệp tài liệu");
    }
    private void touch(CourseEntity c){
        c.contentVersion++;
        c.updatedAt=Instant.now();
        courses.save(c);
    }
    private void ensureEditable(CourseEntity c){
        if(c.status==CourseStatus.ARCHIVED)conflict("Không thể sửa khóa học đã lưu trữ");
    }
    private CourseEntity requireCourse(UUID id){
        return courses.findById(id).orElseThrow(this::notFoundCourse);
    }
    private ApiException notFoundCourse(){
        return new ApiException(HttpStatus.NOT_FOUND,"COURSE_NOT_FOUND","Không tìm thấy khóa học");
    }
    private ApiException notFoundCategory(){
        return new ApiException(HttpStatus.NOT_FOUND,"CATEGORY_NOT_FOUND","Không tìm thấy danh mục");
    }
    private void conflict(String m){
        throw new ApiException(HttpStatus.CONFLICT,"COURSE_CONFLICT",m);
    }
    private String normCode(String s){
        return s.trim().toUpperCase(Locale.ROOT);
    }
    private String blankToNull(String s){
        return s==null||s.isBlank()?null:s.trim();
    }
    private UUID currentUser(){
        try{
            return CurrentUser.id();
        }
        catch(Exception e){
            return new UUID(0,1);
        }

    }
    private CategoryResponse category(CourseCategoryEntity e){
        return new CategoryResponse(e.id,e.code,e.name,e.parentId,e.status,e.sortOrder);
    }
    private List<LessonResponse> lessonResponses(UUID id){
        return lessons.findAllByCourseIdOrderBySortOrderAsc(id).stream().map(this::lesson).toList();
    }
    private LessonResponse lesson(LessonEntity e){
        return new LessonResponse(e.id,e.title,e.type,e.textContent,e.fileId,e.required,e.sortOrder,e.estimatedMinutes);
    }
    private CourseResponse response(CourseEntity e,List<LessonResponse> ls){
        return new CourseResponse(e.id,e.code,e.name,e.description,e.objectives,e.targetAudience,e.durationMinutes,e.passingScore,e.completionPolicyJson,e.categoryId,e.status,e.contentVersion,e.publishedVersion,e.contentVersion>e.publishedVersion,e.publishedAt,e.ownerId,ls);
    }

}
