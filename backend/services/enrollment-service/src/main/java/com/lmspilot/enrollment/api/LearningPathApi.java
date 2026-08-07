package com.lmspilot.enrollment.api;

import com.lmspilot.enrollment.domain.*;

import com.lmspilot.support.api.ApiException;

import com.lmspilot.support.security.CurrentUser;

import jakarta.validation.Valid;

import jakarta.validation.constraints.*;

import java.time.*;

import java.util.*;

import org.springframework.http.*;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.bind.annotation.*;
record LearningPathItemRequest(@NotNull UUID courseId,Boolean required,LearningPathUnlockMode unlockMode,@Min(0) Integer dueOffsetDays){
}
record LearningPathRequest(@NotBlank String code,@NotBlank String name,String description,@NotEmpty
List<@Valid LearningPathItemRequest> items){
}
record AssignLearningPathRequest(AssignmentTargetType assigneeType,UUID assigneeId,Instant dueAt){
}
record LearningPathItemResponse(UUID id,UUID courseId,int courseVersion,int sortOrder,boolean required,LearningPathUnlockMode unlockMode,int dueOffsetDays){
}
record LearningPathResponse(UUID id,String code,String name,String description,LearningPathStatus status,UUID ownerId,Instant publishedAt,Instant updatedAt,List<LearningPathItemResponse> items){
}
record LearningPathAssignmentResponse(UUID id,UUID pathId,AssignmentTargetType assigneeType,UUID assigneeId,Instant dueAt,UUID assignedBy,Instant assignedAt,LearningPathAssignmentStatus status,int expandedUsers){
}
record UserLearningPathResponse(UUID assignmentId,UUID pathId,String code,String name,String description,UserLearningPathStatus status,int progressPercent,Instant dueAt,Instant assignedAt,Instant completedAt,List<Object> items){
}
record LearningPathParticipantResponse(UUID userId,UserLearningPathStatus status,Instant dueAt,Instant assignedAt,Instant completedAt){
}
@Service
@Transactional
class LearningPathService {
    private final LearningPathRepository paths;
    private final LearningPathItemRepository items;
    private final LearningPathAssignmentRepository assignments;
    LearningPathService(LearningPathRepository p,LearningPathItemRepository i,LearningPathAssignmentRepository a){
        paths=p;
        items=i;
        assignments=a;
    }
    List<LearningPathResponse> list(){
        return paths.findAllByStatusNotOrderByUpdatedAtDesc(LearningPathStatus.ARCHIVED).stream().map(this::view).toList();
    }
    LearningPathResponse get(UUID id){
        return view(path(id));
    }
    LearningPathResponse create(LearningPathRequest i){
        String code=i.code().trim().toUpperCase(Locale.ROOT);
        if(paths.existsByCodeIgnoreCase(code))throw new ApiException(HttpStatus.CONFLICT,"LEARNING_PATH_CODE_EXISTS","Mã lộ trình đã tồn tại");
        LearningPathEntity e=new LearningPathEntity();
        e.code=code;
        e.name=i.name().trim();
        e.description=i.description();
        e.ownerId=user();
        paths.save(e);
        replace(e.id,i.items());
        return view(e);
    }
    LearningPathResponse update(UUID id,LearningPathRequest i){
        LearningPathEntity e=path(id);
        if(e.status!=LearningPathStatus.DRAFT)throw new ApiException(HttpStatus.CONFLICT,"LEARNING_PATH_IMMUTABLE","Lộ trình đã xuất bản không thể sửa trực tiếp");
        e.code=i.code().trim().toUpperCase(Locale.ROOT);
        e.name=i.name().trim();
        e.description=i.description();
        e.updatedAt=Instant.now();
        paths.save(e);
        replace(id,i.items());
        return view(e);
    }
    LearningPathResponse clonePath(UUID id){
        LearningPathEntity s=path(id);
        LearningPathRequest r=new LearningPathRequest((s.code+"-COPY-"+Instant.now().getEpochSecond()).substring(0,Math.min(80,(s.code+"-COPY-"+Instant.now().getEpochSecond()).length())),s.name+" (bản sao)",s.description,items.findAllByPathIdOrderBySortOrderAsc(id).stream().map(x->new LearningPathItemRequest(x.courseId,x.required,x.unlockMode,x.dueOffsetDays)).toList());
        return create(r);
    }
    LearningPathResponse publish(UUID id){
        LearningPathEntity e=path(id);
        if(items.findAllByPathIdOrderBySortOrderAsc(id).isEmpty())throw new ApiException(HttpStatus.CONFLICT,"LEARNING_PATH_EMPTY","Lộ trình phải có nội dung");
        e.status=LearningPathStatus.PUBLISHED;
        e.publishedAt=Instant.now();
        e.updatedAt=e.publishedAt;
        return view(paths.save(e));
    }
    LearningPathResponse archive(UUID id){
        LearningPathEntity e=path(id);
        e.status=LearningPathStatus.ARCHIVED;
        e.updatedAt=Instant.now();
        return view(paths.save(e));
    }
    LearningPathAssignmentResponse assign(UUID id,AssignLearningPathRequest i){
        LearningPathEntity p=path(id);
        if(p.status!=LearningPathStatus.PUBLISHED)throw new ApiException(HttpStatus.CONFLICT,"LEARNING_PATH_NOT_PUBLISHED","Chỉ giao lộ trình đã xuất bản");
        LearningPathAssignmentEntity a=new LearningPathAssignmentEntity();
        a.pathId=id;
        a.assigneeType=i.assigneeType();
        a.assigneeId=i.assigneeId();
        a.dueAt=i.dueAt();
        a.assignedBy=user();
        assignments.save(a);
        return assignment(a);
    }
    List<LearningPathAssignmentResponse> assignments(UUID id){
        return assignments.findAllByPathIdOrderByAssignedAtDesc(id).stream().map(this::assignment).toList();
    }
    void revoke(UUID id,UUID assignmentId){
        LearningPathAssignmentEntity a=assignments.findById(assignmentId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"LEARNING_PATH_ASSIGNMENT_NOT_FOUND","Không tìm thấy phân công"));
        if(!a.pathId.equals(id))throw new ApiException(HttpStatus.NOT_FOUND,"LEARNING_PATH_ASSIGNMENT_NOT_FOUND","Không tìm thấy phân công");
        a.status=LearningPathAssignmentStatus.CANCELLED;
        assignments.save(a);
    }
    List<UserLearningPathResponse> mine(){
        UUID u=user();
        return assignments.findAll().stream().filter(a->a.assigneeType==AssignmentTargetType.USER&&a.assigneeId.equals(u)&&a.status==LearningPathAssignmentStatus.ACTIVE).map(a->{
            LearningPathEntity p=path(a.pathId);
            return new UserLearningPathResponse(a.id,p.id,p.code,p.name,p.description,UserLearningPathStatus.ASSIGNED,0,a.dueAt,a.assignedAt,null,List.of());
        }
        ).toList();
    }
    List<LearningPathParticipantResponse> participants(UUID id){
        return assignments.findAllByPathIdOrderByAssignedAtDesc(id).stream().filter(a->a.assigneeType==AssignmentTargetType.USER).map(a->new LearningPathParticipantResponse(a.assigneeId,a.status==LearningPathAssignmentStatus.ACTIVE?UserLearningPathStatus.ASSIGNED:UserLearningPathStatus.CANCELLED,a.dueAt,a.assignedAt,null)).toList();
    }
    private void replace(UUID id,List<LearningPathItemRequest> input){
        items.deleteAllByPathId(id);
        int n=0;
        for(LearningPathItemRequest x:input){
            LearningPathItemEntity e=new LearningPathItemEntity();
            e.pathId=id;
            e.classId=x.courseId();
            e.courseId=x.courseId();
            e.sortOrder=n++;
            e.required=x.required()==null||x.required();
            e.unlockMode=x.unlockMode()==null?LearningPathUnlockMode.AFTER_PREVIOUS:x.unlockMode();
            e.dueOffsetDays=x.dueOffsetDays()==null?0:x.dueOffsetDays();
            items.save(e);
        }

    }
    private LearningPathEntity path(UUID id){
        return paths.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"LEARNING_PATH_NOT_FOUND","Không tìm thấy lộ trình"));
    }
    private LearningPathResponse view(LearningPathEntity e){
        return new LearningPathResponse(e.id,e.code,e.name,e.description,e.status,e.ownerId,e.publishedAt,e.updatedAt,items.findAllByPathIdOrderBySortOrderAsc(e.id).stream().map(x->new LearningPathItemResponse(x.id,x.courseId,x.courseVersion,x.sortOrder,x.required,x.unlockMode,x.dueOffsetDays)).toList());
    }
    private LearningPathAssignmentResponse assignment(LearningPathAssignmentEntity a){
        return new LearningPathAssignmentResponse(a.id,a.pathId,a.assigneeType,a.assigneeId,a.dueAt,a.assignedBy,a.assignedAt,a.status,a.assigneeType==AssignmentTargetType.USER?1:0);
    }
    private UUID user(){
        try{
            return CurrentUser.id();
        }
        catch(Exception e){
            return new UUID(0,1);
        }

    }

}
@RestController
@RequestMapping("/api/v1/learning-paths")
public class LearningPathApi {
    private final LearningPathService service;
    public LearningPathApi(LearningPathService s){
        service=s;
    }
    @GetMapping
    public List<LearningPathResponse> list(){
        return service.list();
    }
    @GetMapping("/{id}")
    public LearningPathResponse get(@PathVariable UUID id){
        return service.get(id);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LearningPathResponse create(@Valid
    @RequestBody LearningPathRequest i){
        return service.create(i);
    }
    @PutMapping("/{id}")
    public LearningPathResponse update(@PathVariable UUID id,@Valid
    @RequestBody LearningPathRequest i){
        return service.update(id,i);
    }
    @PostMapping("/{id}/clone")
    public LearningPathResponse clonePath(@PathVariable UUID id){
        return service.clonePath(id);
    }
    @PostMapping("/{id}/publish")
    public LearningPathResponse publish(@PathVariable UUID id){
        return service.publish(id);
    }
    @PostMapping("/{id}/archive")
    public LearningPathResponse archive(@PathVariable UUID id){
        return service.archive(id);
    }
    @PostMapping("/{id}/assignments")
    public LearningPathAssignmentResponse assign(@PathVariable UUID id,@RequestBody AssignLearningPathRequest i){
        return service.assign(id,i);
    }
    @GetMapping("/{id}/assignments")
    public List<LearningPathAssignmentResponse> assignments(@PathVariable UUID id){
        return service.assignments(id);
    }
    @DeleteMapping("/{id}/assignments/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id,@PathVariable UUID assignmentId){
        service.revoke(id,assignmentId);
    }
    @GetMapping("/me")
    public List<UserLearningPathResponse> mine(){
        return service.mine();
    }
    @GetMapping("/{id}/participants")
    public List<LearningPathParticipantResponse> participants(@PathVariable UUID id){
        return service.participants(id);
    }

}
