package com.lmspilot.grading.api;

import com.fasterxml.jackson.databind.*;

import com.lmspilot.contracts.*;

import com.lmspilot.grading.domain.*;

import com.lmspilot.support.api.ApiException;

import com.lmspilot.support.events.DomainEventPublisher;

import com.lmspilot.support.security.CurrentUser;

import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.client.RestClient;

import java.math.*;

import java.time.Instant;

import java.util.*;
@Service
public class GradingService{
    private final GradeResultRepository repository;
    private final GradeRevisionRepository revisions;
    private final GradeAppealRepository appeals;
    private final ObjectMapper mapper;
    private final DomainEventPublisher publisher;
    private final RestClient assessment;
    private final String token;
    public GradingService(GradeResultRepository r,GradeRevisionRepository v,GradeAppealRepository a,ObjectMapper m,DomainEventPublisher p,@Value("${services.assessment-url:http://localhost:8086}")String url,@Value("${lmspilot.internal-token}")String t){
        repository=r;
        revisions=v;
        appeals=a;
        mapper=m;
        publisher=p;
        assessment=RestClient.builder().baseUrl(url).build();
        token=t;
    }
    @RabbitListener(queues="grading.exam-submitted")
    @Transactional
    public void onExamSubmitted(DomainEventEnvelope e){
        gradeAutomatically(mapper.convertValue(e.payload(),ExamSubmittedPayload.class).sessionId());
    }
    @Transactional
    public GradeResponse gradeAutomatically(UUID sid){
        repository.lockSession(sid.toString());
        var old=repository.findBySessionId(sid);
        if(old!=null){
            if(old.getStatus()==GradeStatus.COMPLETED)publish(old);
            return response(old);
        }
        var p=assessment.get().uri("/internal/v1/assessment/sessions/{id}/grading-payload",sid).header("X-Service-Token",token).retrieve().body(GradingPayload.class);
        if(p==null)throw new ApiException(HttpStatus.BAD_GATEWAY,"ASSESSMENT_UNAVAILABLE","Không lấy được dữ liệu bài thi");
        double earned=0,max=0;
        boolean manual=false;
        List<GradeDetail>d=new ArrayList<>();
        for(var q:p.questions()){
            max+=q.points();
            JsonNode ans=p.answers().get(q.questionId().toString());
            boolean man=!p.autoGrade()||q.type().equals("ESSAY")||q.type().equals("SHORT_TEXT");
            double aw=man?0:(matches(ans,q.correctAnswers(),q.type())?q.points():0);
            earned+=aw;
            manual|=man;
            d.add(new GradeDetail(q.questionId(),q.type(),aw,q.points(),man,q.prompt(),ans));
        }
        double pct=max==0?0:round(earned*100/max);
        var e=repository.save(new GradeResultEntity(p.sessionId(),p.examId(),p.enrollmentId(),p.courseId(),p.lessonId(),p.userId(),round(earned),round(max),pct,p.passingScore(),!manual&&pct>=p.passingScore(),p.scoreStrategy(),manual?GradeStatus.PENDING_MANUAL:GradeStatus.COMPLETED,write(d)));
        if(!manual){
            publish(e);
            if("COMPETITION".equals(p.contextType()))recordCompetitionResult(e,p);
        }
        return response(e);
    }
    @Transactional(readOnly=true)
    public List<GradeResponse>myGrades(){
        return repository.findAllByUserIdOrderByCreatedAtDesc(CurrentUser.id()).stream().map(this::response).toList();
    }
    @Transactional(readOnly=true)
    public List<GradeResponse>queue(){
        Set<UUID>a=manageableExamIds(CurrentUser.id());
        return repository.findAllByStatusOrderByCreatedAtAsc(GradeStatus.PENDING_MANUAL).stream().filter(x->a.contains(x.getExamId())).map(this::response).toList();
    }
    @Transactional
    public GradeResponse completeManual(UUID id,ManualGradeRequest i){
        var e=repository.findById(id).orElseThrow(()->notFound("GRADE_NOT_FOUND","Không tìm thấy kết quả"));
        requireManageable(e.getExamId());
        if(e.getStatus()!=GradeStatus.PENDING_MANUAL)throw new ApiException(HttpStatus.CONFLICT,"GRADE_NOT_PENDING","Kết quả không còn ở trạng thái chờ chấm");
        if(i.score()>e.getMaxScore())throw new ApiException(HttpStatus.BAD_REQUEST,"GRADE_EXCEEDS_MAX","Điểm không được vượt quá điểm tối đa");
        double ps=e.getScore(),pp=e.getPercentage();
        e.setScore(round(i.score()));
        e.setPercentage(e.getMaxScore()==0?0:round(e.getScore()*100/e.getMaxScore()));
        e.setPassed(e.getPercentage()>=e.getPassingScore());
        e.setStatus(GradeStatus.COMPLETED);
        e.setFeedback(i.feedback());
        e.setGradedBy(CurrentUser.id());
        e.setUpdatedAt(Instant.now());
        revisions.save(new GradeRevisionEntity(e.getId(),ps,e.getScore(),pp,e.getPercentage(),GradeRevisionType.MANUAL_GRADE,blankDefault(i.reason(),"Chấm thủ công"),CurrentUser.id()));
        publish(e);
        return response(e);
    }
    @Transactional
    public GradeAppealResponse createAppeal(UUID gid,CreateGradeAppealRequest i){
        var g=repository.findById(gid).orElseThrow(()->notFound("GRADE_NOT_FOUND","Không tìm thấy kết quả"));
        UUID u=CurrentUser.id();
        if(!g.getUserId().equals(u))throw new ApiException(HttpStatus.FORBIDDEN,"GRADE_OWNER_MISMATCH","Không thể phúc khảo kết quả của người khác");
        if(g.getStatus()!=GradeStatus.COMPLETED)throw new ApiException(HttpStatus.CONFLICT,"GRADE_NOT_FINAL","Kết quả chưa được chấm hoàn tất");
        String r=i.reason()==null?"":i.reason().trim();
        if(r.length()<10||r.length()>4000)throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_APPEAL_REASON","Lý do phúc khảo phải từ 10 đến 4000 ký tự");
        var existing=appeals.findByGradeIdAndUserIdAndActiveKey(gid,u,"ACTIVE");
        if(existing!=null)return appealResponse(existing);
        var a=appeals.save(new GradeAppealEntity(gid,u,r));
        publisher.publish(EventTypes.GRADE_APPEAL_OPENED,"grading-service",a.getId().toString(),Map.of("appealId",a.getId(),"gradeId",gid,"userId",u));
        return appealResponse(a);
    }
    @Transactional(readOnly=true)
    public List<GradeAppealResponse>myAppeals(){
        return appeals.findAllByUserIdOrderByCreatedAtDesc(CurrentUser.id()).stream().map(this::appealResponse).toList();
    }
    @Transactional(readOnly=true)
    public List<GradeAppealResponse>appealQueue(){
        var open=appeals.findAllByStatusInOrderByCreatedAtAsc(List.of(GradeAppealStatus.OPEN,GradeAppealStatus.UNDER_REVIEW));
        Set<UUID>allowed=manageableExamIds(CurrentUser.id());
        Map<UUID,GradeResultEntity>grades=new HashMap<>();
        repository.findAllById(open.stream().map(GradeAppealEntity::getGradeId).toList()).forEach(g->grades.put(g.getId(),g));
        return open.stream().filter(a->grades.containsKey(a.getGradeId())&&allowed.contains(grades.get(a.getGradeId()).getExamId())).map(this::appealResponse).toList();
    }
    @Transactional
    public GradeAppealResponse resolveAppeal(UUID id,ResolveGradeAppealRequest i){
        if(i.status()!=GradeAppealStatus.APPROVED&&i.status()!=GradeAppealStatus.REJECTED)throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_APPEAL_RESOLUTION","Chỉ có thể phê duyệt hoặc từ chối phúc khảo");
        var a=appeals.findById(id).orElseThrow(()->notFound("APPEAL_NOT_FOUND","Không tìm thấy yêu cầu phúc khảo"));
        if(a.getStatus()!=GradeAppealStatus.OPEN&&a.getStatus()!=GradeAppealStatus.UNDER_REVIEW)throw new ApiException(HttpStatus.CONFLICT,"APPEAL_ALREADY_RESOLVED","Yêu cầu phúc khảo đã được xử lý");
        var g=repository.findById(a.getGradeId()).orElseThrow(()->notFound("GRADE_NOT_FOUND","Không tìm thấy kết quả"));
        requireManageable(g.getExamId());
        String res=i.resolution()==null?"":i.resolution().trim();
        if(res.length()<3||res.length()>4000)throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_APPEAL_RESOLUTION","Nội dung xử lý không hợp lệ");
        if(i.status()==GradeAppealStatus.APPROVED&&i.correctedScore()!=null){
            if(i.correctedScore()>g.getMaxScore())throw new ApiException(HttpStatus.BAD_REQUEST,"GRADE_EXCEEDS_MAX","Điểm không được vượt quá điểm tối đa");
            double ps=g.getScore(),pp=g.getPercentage();
            g.setScore(round(i.correctedScore()));
            g.setPercentage(g.getMaxScore()==0?0:round(g.getScore()*100/g.getMaxScore()));
            g.setPassed(g.getPercentage()>=g.getPassingScore());
            g.setGradedBy(CurrentUser.id());
            g.setUpdatedAt(Instant.now());
            revisions.save(new GradeRevisionEntity(g.getId(),ps,g.getScore(),pp,g.getPercentage(),GradeRevisionType.APPEAL_CORRECTION,res,CurrentUser.id()));
            publish(g);
        }
        a.setStatus(i.status());
        a.setActiveKey("CLOSED-"+a.getId());
        a.setResolution(res);
        a.setResolvedBy(CurrentUser.id());
        a.setResolvedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        publisher.publish(EventTypes.GRADE_APPEAL_RESOLVED,"grading-service",a.getId().toString(),Map.of("appealId",a.getId(),"gradeId",g.getId(),"userId",a.getUserId(),"status",a.getStatus().name(),"resolvedBy",a.getResolvedBy()));
        return appealResponse(a);
    }
    @Transactional(readOnly=true)
    public List<GradeRevisionResponse>history(UUID id){
        var g=repository.findById(id).orElseThrow(()->notFound("GRADE_NOT_FOUND","Không tìm thấy kết quả"));
        if(!g.getUserId().equals(CurrentUser.id()))requireManageable(g.getExamId());
        return revisions.findAllByGradeIdOrderByCreatedAtDesc(id).stream().map(this::revisionResponse).toList();
    }
    private void recordCompetitionResult(GradeResultEntity e,GradingPayload p){
        assessment.post().uri("/internal/v1/competitions/{id}/results",e.getExamId()).header("X-Service-Token",token).body(Map.of("userId",e.getUserId(),"attemptId",e.getSessionId(),"score",e.getPercentage(),"durationMs",p.durationMs(),"submittedAt",p.submittedAt()==null?Instant.now():p.submittedAt())).retrieve().toBodilessEntity();
    }
    private Set<UUID>manageableExamIds(UUID u){
        String[]v=assessment.get().uri("/internal/v1/assessment/exams/manageable/{userId}",u).header("X-Service-Token",token).retrieve().body(String[].class);
        Set<UUID>r=new HashSet<>();
        if(v!=null)for(String s:v)r.add(UUID.fromString(s));
        return r;
    }
    private void requireManageable(UUID id){
        if(!manageableExamIds(CurrentUser.id()).contains(id))throw new ApiException(HttpStatus.FORBIDDEN,"GRADE_OUT_OF_SCOPE","Kết quả ngoài phạm vi được phân công");
    }
    private boolean matches(JsonNode a,List<String>expected,String type){
        if(a==null||a.isNull())return false;
        Set<String>x=new LinkedHashSet<>();
        if(a.isArray())a.forEach(n->x.add(n.asText().trim()));
        else x.add(a.asText().trim());
        Set<String>t=new LinkedHashSet<>();
        expected.forEach(s->t.add(s.trim()));
        if("MULTIPLE_CHOICE".equals(type))return x.equals(t);
        return !x.isEmpty()&&!t.isEmpty()&&x.iterator().next().equalsIgnoreCase(t.iterator().next());
    }
    private void publish(GradeResultEntity e){
        List<GradeResultEntity>attempts=new ArrayList<>();
        if(e.getEnrollmentId()!=null){
            attempts.addAll(repository.findAllByExamIdAndEnrollmentIdOrderByCreatedAtAsc(e.getExamId(),e.getEnrollmentId()));
            repository.findAllByExamIdAndUserIdAndEnrollmentIdIsNullOrderByCreatedAtAsc(e.getExamId(),e.getUserId()).stream().filter(x->Objects.equals(x.getCourseId(),e.getCourseId())).forEach(attempts::add);
        }
        else attempts.addAll(repository.findAllByExamIdAndUserIdAndEnrollmentIdIsNullOrderByCreatedAtAsc(e.getExamId(),e.getUserId()));
        Map<UUID,GradeResultEntity>uniq=new LinkedHashMap<>();
        attempts.stream().sorted(Comparator.comparing(GradeResultEntity::getCreatedAt)).forEach(x->uniq.put(x.getId(),x));
        var completed=uniq.values().stream().filter(x->x.getStatus()==GradeStatus.COMPLETED).toList();
        double effective=e.getPercentage();
        if("HIGHEST".equals(e.getScoreStrategy()))effective=completed.stream().mapToDouble(GradeResultEntity::getPercentage).max().orElse(effective);
        else if("AVERAGE".equals(e.getScoreStrategy())&&!completed.isEmpty())effective=completed.stream().mapToDouble(GradeResultEntity::getPercentage).average().orElse(effective);
        else if(!completed.isEmpty())effective=completed.get(completed.size()-1).getPercentage();
        effective=round(effective);
        boolean passed=effective>=e.getPassingScore();
        publisher.publish(EventTypes.EXAM_GRADED,"grading-service",e.getId().toString(),new ExamGradedPayload(e.getSessionId(),e.getExamId(),e.getUserId(),e.getScore(),e.getMaxScore(),e.isPassed(),e.getStatus().name(),e.getEnrollmentId(),e.getCourseId(),e.getLessonId(),passed,effective,e.getScoreStrategy()));
        assessment.post().uri("/internal/v1/assessment/sessions/{id}/graded",e.getSessionId()).header("X-Service-Token",token).retrieve().toBodilessEntity();
    }
    private double round(double v){
        return BigDecimal.valueOf(v).setScale(2,RoundingMode.HALF_UP).doubleValue();
    }
    private String write(Object o){
        try{
            return mapper.writeValueAsString(o);
        }
        catch(Exception e){
            throw new IllegalStateException(e);
        }

    }
    private GradeResponse response(GradeResultEntity e){
        try{
            var type=mapper.getTypeFactory().constructCollectionType(List.class,GradeDetail.class);
            List<GradeDetail>d=mapper.readValue(e.getDetailsJson(),type);
            return new GradeResponse(e.getId(),e.getSessionId(),e.getExamId(),e.getEnrollmentId(),e.getCourseId(),e.getLessonId(),e.getUserId(),e.getScore(),e.getMaxScore(),e.getPercentage(),e.isPassed(),e.getStatus(),d,e.getFeedback(),e.getUpdatedAt());
        }
        catch(Exception x){
            throw new IllegalStateException(x);
        }

    }
    private GradeRevisionResponse revisionResponse(GradeRevisionEntity e){
        return new GradeRevisionResponse(e.getId(),e.getPreviousScore(),e.getNewScore(),e.getPreviousPercentage(),e.getNewPercentage(),e.getType(),e.getReason(),e.getChangedBy(),e.getCreatedAt());
    }
    private GradeAppealResponse appealResponse(GradeAppealEntity e){
        return new GradeAppealResponse(e.getId(),e.getGradeId(),e.getUserId(),e.getReason(),e.getStatus(),e.getResolution(),e.getResolvedBy(),e.getCreatedAt(),e.getUpdatedAt(),e.getResolvedAt());
    }
    private ApiException notFound(String c,String m){
        return new ApiException(HttpStatus.NOT_FOUND,c,m);
    }
    private String blankDefault(String s,String d){
        return s==null||s.trim().isBlank()?d:s.trim();
    }

}
