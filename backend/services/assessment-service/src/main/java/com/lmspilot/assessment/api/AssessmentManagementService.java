package com.lmspilot.assessment.api;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.lmspilot.assessment.api.AssessmentModels.*;

import com.lmspilot.assessment.domain.*;

import com.lmspilot.assessment.platform.*;

import com.lmspilot.contracts.EventTypes;

import com.lmspilot.contracts.ExamSubmittedPayload;

import com.lmspilot.support.api.ApiException;

import com.lmspilot.support.events.DomainEventPublisher;

import com.lmspilot.support.security.CurrentUser;

import java.time.*;

import java.util.*;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
@Service
public class AssessmentManagementService {
    private final QuestionRepository questions;
    private final ExamRepository exams;
    private final ExamQuestionRepository examQuestions;
    private final ExamSessionRepository sessions;
    private final ExamSessionEventRepository sessionEvents;
    private final AssessmentContextRepository contexts;
    private final AssessmentAssignmentRepository assignments;
    private final CompetitionRepository competitions;
    private final CompetitionLeaderboardRepository leaderboard;
    private final CompetitionRewardRepository rewards;
    private final RewardLedgerRepository ledger;
    private final QuestionProvenanceRepository provenance;
    private final ObjectMapper mapper;
    private final DomainEventPublisher publisher;
    public AssessmentManagementService(QuestionRepository questions,ExamRepository exams,ExamQuestionRepository examQuestions,ExamSessionRepository sessions,ExamSessionEventRepository sessionEvents,AssessmentContextRepository contexts,AssessmentAssignmentRepository assignments,CompetitionRepository competitions,CompetitionLeaderboardRepository leaderboard,CompetitionRewardRepository rewards,RewardLedgerRepository ledger,QuestionProvenanceRepository provenance,ObjectMapper mapper,DomainEventPublisher publisher){
        this.questions=questions;
        this.exams=exams;
        this.examQuestions=examQuestions;
        this.sessions=sessions;
        this.sessionEvents=sessionEvents;
        this.contexts=contexts;
        this.assignments=assignments;
        this.competitions=competitions;
        this.leaderboard=leaderboard;
        this.rewards=rewards;
        this.ledger=ledger;
        this.provenance=provenance;
        this.mapper=mapper;
        this.publisher=publisher;
    }
    private UUID actor(){
        return CurrentUser.id();
    }
    private ApiException missing(String code,String message){
        return new ApiException(HttpStatus.NOT_FOUND,code,message);
    }
    private List<String> list(String json){
        try{
            return mapper.readValue(json,new TypeReference<List<String>>(){
            }
            );
        }
        catch(Exception e){
            return List.of();
        }

    }
    private Map<String,JsonNode> answers(String json){
        try{
            return mapper.readValue(json,new TypeReference<Map<String,JsonNode>>(){
            }
            );
        }
        catch(Exception e){
            return Map.of();
        }

    }
    private String json(Object value){
        try{
            return mapper.writeValueAsString(value);
        }
        catch(Exception e){
            throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_JSON","Dữ liệu không hợp lệ");
        }

    }
    private List<ExamQuestionView> sessionQuestions(ExamSessionEntity session){
        try{
            List<ExamQuestionView> value=mapper.readValue(session.questionsSnapshotJson,new TypeReference<List<ExamQuestionView>>(){
            }
            );
            if(value!=null&&!value.isEmpty())return value;
        }
        catch(Exception ignored){
        }
        return questionViews(examQuestions.findAllByExamIdOrderBySortOrderAsc(session.examId));
    }
    private List<GradingQuestionPayload> sessionGradingQuestions(ExamSessionEntity session){
        try{
            List<GradingQuestionPayload> value=mapper.readValue(session.gradingSnapshotJson,new TypeReference<List<GradingQuestionPayload>>(){
            }
            );
            if(value!=null&&!value.isEmpty())return value;
        }
        catch(Exception ignored){
        }
        return examQuestions.findAllByExamIdOrderBySortOrderAsc(session.examId).stream().map(q->new GradingQuestionPayload(q.questionId,q.type,q.promptSnapshot,list(q.correctAnswersSnapshotJson),q.points)).toList();
    }
    private void captureSessionSnapshot(ExamSessionEntity session,ExamEntity exam,AssessmentContextEntity context){
        List<ExamQuestionEntity> rows=examQuestions.findAllByExamIdOrderBySortOrderAsc(exam.id);
        if(rows.isEmpty())throw new ApiException(HttpStatus.CONFLICT,"EXAM_HAS_NO_QUESTIONS","Đề thi chưa có câu hỏi nên không thể bắt đầu");
        List<ExamQuestionView> candidate=new ArrayList<>(questionViews(rows));
        if(exam.shuffleQuestions)Collections.shuffle(candidate,new Random(session.id.getMostSignificantBits()^session.id.getLeastSignificantBits()));
        if(exam.shuffleAnswers){
            List<ExamQuestionView> shuffled=new ArrayList<>();
            int index=0;
            for(ExamQuestionView question:candidate){
                List<String> options=new ArrayList<>(question.options());
                Collections.shuffle(options,new Random(session.id.hashCode()+index++));
                shuffled.add(new ExamQuestionView(question.id(),question.type(),question.prompt(),List.copyOf(options),question.points(),question.sortOrder()));
            }
            candidate=shuffled;
        }
        List<GradingQuestionPayload> grading=rows.stream().map(q->new GradingQuestionPayload(q.questionId,q.type,q.promptSnapshot,list(q.correctAnswersSnapshotJson),q.points)).toList();
        session.questionsSnapshotJson=json(candidate);
        session.gradingSnapshotJson=json(grading);
        session.passingScoreSnapshot=exam.passingScore;
        session.autoGradeSnapshot=context==null||context.autoGrade;
        session.contextTypeSnapshot=context==null?inferredWithoutLookup(exam):context.contextType;
        session.scoreStrategySnapshot=exam.scoreStrategy;
    }
    private void owner(UUID id){
        if(!Objects.equals(id,actor())) throw new ApiException(HttpStatus.FORBIDDEN,"OUT_OF_SCOPE","Dữ liệu ngoài phạm vi quản lý");
    }
    private void validateExamLifecycle(ExamRequest i){
        if(i==null)throw new ApiException(HttpStatus.BAD_REQUEST,"EXAM_REQUEST_REQUIRED","Thiếu cấu hình bài thi");
        if(i.opensAt()!=null&&i.closesAt()!=null&&!i.closesAt().isAfter(i.opensAt()))throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_EXAM_WINDOW","Thời gian đóng phải sau thời gian mở");
        if(i.status()==ExamStatus.ACTIVE&&(i.questions()==null||i.questions().isEmpty()))throw new ApiException(HttpStatus.BAD_REQUEST,"EXAM_HAS_NO_QUESTIONS","Không thể xuất bản đề thi chưa có câu hỏi");
    }
    private boolean expireIfPastGrace(ExamSessionEntity s,Instant now){
        if(s.status==ExamSessionStatus.IN_PROGRESS&&now.isAfter(s.graceUntil)){
            s.status=ExamSessionStatus.EXPIRED;
            s.updatedAt=now;
            return true;
        }
        return false;
    }
    private Instant attemptFinishedAt(ExamSessionEntity s){
        return s.submittedAt!=null?s.submittedAt:s.updatedAt;
    }
    private void validateQuestion(QuestionRequest i){
        if(i.type()==null||i.prompt()==null||i.prompt().isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_QUESTION","Câu hỏi không hợp lệ");
        if((i.type()==QuestionType.SINGLE_CHOICE||i.type()==QuestionType.MULTIPLE_CHOICE||i.type()==QuestionType.TRUE_FALSE)&&(i.options().size()<2||i.correctAnswers().isEmpty())) throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_OPTIONS","Câu hỏi khách quan cần phương án và đáp án đúng");
        if(!i.options().containsAll(i.correctAnswers())) throw new ApiException(HttpStatus.BAD_REQUEST,"ANSWER_NOT_IN_OPTIONS","Đáp án đúng phải thuộc danh sách phương án");
    }
    private QuestionResponse q(QuestionEntity e){
        return new QuestionResponse(e.id,e.type,e.prompt,list(e.optionsJson),list(e.correctAnswersJson),e.explanation,e.difficulty,Arrays.stream(e.tagsCsv.split(",")).filter(s->!s.isBlank()).collect(Collectors.toCollection(LinkedHashSet::new)),e.defaultPoints,e.status,e.questionVersion);
    }
    @Transactional(readOnly=true)
    public List<QuestionResponse> listQuestions(){
        return questions.findAllByOwnerIdOrderByUpdatedAtDesc(actor()).stream().filter(x->x.status!=QuestionStatus.ARCHIVED).map(this::q).toList();
    }
    @Transactional
    public QuestionResponse createQuestion(QuestionRequest i){
        validateQuestion(i);
        QuestionEntity e=new QuestionEntity();
        e.ownerId=actor();
        e.type=i.type();
        e.prompt=i.prompt().trim();
        e.optionsJson=json(i.options().stream().map(String::trim).toList());
        e.correctAnswersJson=json(i.correctAnswers().stream().map(String::trim).toList());
        e.explanation=i.explanation();
        e.difficulty=i.difficulty();
        e.tagsCsv=String.join(",",i.tags());
        e.defaultPoints=i.defaultPoints();
        return q(questions.save(e));
    }
    @Transactional
    public QuestionResponse updateQuestion(UUID id,QuestionRequest i){
        validateQuestion(i);
        QuestionEntity e=questions.findById(id).orElseThrow(()->missing("QUESTION_NOT_FOUND","Không tìm thấy câu hỏi"));
        owner(e.ownerId);
        e.type=i.type();
        e.prompt=i.prompt().trim();
        e.optionsJson=json(i.options());
        e.correctAnswersJson=json(i.correctAnswers());
        e.explanation=i.explanation();
        e.difficulty=i.difficulty();
        e.tagsCsv=String.join(",",i.tags());
        e.defaultPoints=i.defaultPoints();
        e.questionVersion++;
        e.updatedAt=Instant.now();
        return q(e);
    }
    @Transactional
    public void archiveQuestion(UUID id){
        QuestionEntity e=questions.findById(id).orElseThrow(()->missing("QUESTION_NOT_FOUND","Không tìm thấy câu hỏi"));
        owner(e.ownerId);
        e.status=QuestionStatus.ARCHIVED;
        e.updatedAt=Instant.now();
    }
    private AssessmentContextType inferred(ExamEntity e){
        return contexts.findById(e.id).map(c->c.contextType).orElse(inferredWithoutLookup(e));
    }
    private AssessmentContextType inferredWithoutLookup(ExamEntity e){
        return e.courseId==null?AssessmentContextType.STANDALONE_EXAM:AssessmentContextType.COURSE_QUIZ;
    }
    private List<ExamQuestionView> questionViews(List<ExamQuestionEntity> rows){
        return rows.stream().map(x->new ExamQuestionView(x.questionId,x.type,x.promptSnapshot,list(x.optionsSnapshotJson),x.points,x.sortOrder)).toList();
    }
    private ExamResponse exam(ExamEntity e,AssessmentContextEntity c,List<ExamQuestionEntity> rows){
        List<ExamQuestionView> views=questionViews(rows);
        return new ExamResponse(e.id,e.title,e.courseId,e.lessonId,c==null?inferredWithoutLookup(e):c.contextType,c==null?null:c.cohortId,c==null||c.autoGrade,e.durationMinutes,e.opensAt,e.closesAt,e.maxAttempts,e.waitMinutesBetweenAttempts,e.passingScore,e.shuffleQuestions,e.shuffleAnswers,e.scoreStrategy,e.status,e.examVersion,views);
    }
    private ExamResponse exam(ExamEntity e){
        AssessmentContextEntity c=contexts.findById(e.id).orElse(null);
        return exam(e,c,examQuestions.findAllByExamIdOrderBySortOrderAsc(e.id));
    }
    @Transactional(readOnly=true)
    public List<ExamResponse> listExams(){
        List<ExamEntity> visible=(CurrentUser.hasRole("STUDENT")?exams.findAllByStatusOrderByUpdatedAtDesc(ExamStatus.ACTIVE):exams.findAll()).stream().filter(e->e.status!=ExamStatus.ARCHIVED).sorted(Comparator.comparing((ExamEntity e)->e.updatedAt).reversed()).toList();
        if(visible.isEmpty())return List.of();
        Set<UUID>ids=visible.stream().map(e->e.id).collect(Collectors.toSet());
        Map<UUID,AssessmentContextEntity>contextById=contexts.findAllByAssessmentIdIn(ids).stream().collect(Collectors.toMap(c->c.assessmentId,c->c));
        Map<UUID,List<ExamQuestionEntity>>questionsByExam=examQuestions.findAllByExamIdInOrderByExamIdAscSortOrderAsc(ids).stream().collect(Collectors.groupingBy(q->q.examId));
        return visible.stream().map(e->exam(e,contextById.get(e.id),questionsByExam.getOrDefault(e.id,List.of()))).toList();
    }
    @Transactional(readOnly=true)
    public ExamResponse getExam(UUID id){
        return exam(exams.findById(id).orElseThrow(()->missing("EXAM_NOT_FOUND","Không tìm thấy bài thi")));
    }
    private AssessmentContextSpec spec(ExamRequest i){
        AssessmentContextType t=i.contextType()!=null?i.contextType():(i.courseId()!=null?AssessmentContextType.COURSE_QUIZ:AssessmentContextType.STANDALONE_EXAM);
        try{
            return new AssessmentContextSpec(t,i.courseId(),i.cohortId(),i.opensAt(),i.closesAt(),i.maxAttempts(),i.autoGrade());
        }
        catch(IllegalArgumentException e){
            throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_ASSESSMENT_CONTEXT",e.getMessage());
        }

    }
    private void apply(ExamEntity e,ExamRequest i){
        AssessmentContextSpec s=spec(i);
        e.title=i.title().trim();
        e.courseId=i.courseId();
        e.lessonId=i.lessonId();
        e.durationMinutes=i.durationMinutes();
        e.opensAt=i.opensAt();
        e.closesAt=i.closesAt();
        e.maxAttempts=i.maxAttempts();
        e.waitMinutesBetweenAttempts=i.waitMinutesBetweenAttempts();
        e.passingScore=i.passingScore();
        e.shuffleQuestions=i.shuffleQuestions();
        e.shuffleAnswers=i.shuffleAnswers();
        e.scoreStrategy=i.scoreStrategy()==null?ScoreStrategy.HIGHEST:i.scoreStrategy();
        e.status=i.status()==null?ExamStatus.DRAFT:i.status();
        e.updatedAt=Instant.now();
        AssessmentContextEntity c=contexts.findById(e.id).orElseGet(AssessmentContextEntity::new);
        c.assessmentId=e.id;
        c.contextType=s.type();
        c.courseId=s.courseId();
        c.cohortId=s.cohortId();
        c.opensAt=s.opensAt();
        c.closesAt=s.closesAt();
        c.maxAttempts=s.maxAttempts();
        c.autoGrade=s.autoGrade();
        contexts.save(c);
    }
    private void snapshot(ExamEntity e,List<ExamQuestionInput> inputs){
        examQuestions.deleteAllByExamId(e.id);
        if(inputs==null)return;
        Set<UUID> seen=new HashSet<>();
        for(ExamQuestionInput in:inputs){
            if(!seen.add(in.questionId()))throw new ApiException(HttpStatus.BAD_REQUEST,"DUPLICATE_QUESTION","Một câu hỏi xuất hiện nhiều lần");
            QuestionEntity q=questions.findById(in.questionId()).orElseThrow(()->missing("QUESTION_NOT_FOUND","Không tìm thấy câu hỏi"));
            ExamQuestionEntity x=new ExamQuestionEntity();
            x.examId=e.id;
            x.questionId=q.id;
            x.questionVersion=q.questionVersion;
            x.type=q.type;
            x.promptSnapshot=q.prompt;
            x.optionsSnapshotJson=q.optionsJson;
            x.correctAnswersSnapshotJson=q.correctAnswersJson;
            x.points=in.points();
            x.sortOrder=in.sortOrder();
            examQuestions.save(x);
        }

    }
    @Transactional
    public ExamResponse createExam(ExamRequest i){
        validateExamLifecycle(i);
        ExamEntity e=new ExamEntity();
        e.ownerId=actor();
        apply(e,i);
        exams.save(e);
        snapshot(e,i.questions());
        if(inferred(e)==AssessmentContextType.COMPETITION){
            CompetitionEntity c=new CompetitionEntity();
            c.assessmentId=e.id;
            competitions.save(c);
        }
        return exam(e);
    }
    @Transactional
    public ExamResponse updateExam(UUID id,ExamRequest i){
        validateExamLifecycle(i);
        ExamEntity e=exams.findById(id).orElseThrow(()->missing("EXAM_NOT_FOUND","Không tìm thấy bài thi"));
        owner(e.ownerId);
        if(sessions.existsByExamId(id)) throw new ApiException(HttpStatus.CONFLICT,"EXAM_ALREADY_ATTEMPTED","Bài thi đã có lượt làm; hãy nhân bản để tạo phiên bản mới");
        apply(e,i);
        e.examVersion++;
        snapshot(e,i.questions());
        return exam(e);
    }
    @Transactional
    public void archiveExam(UUID id){
        ExamEntity e=exams.findById(id).orElseThrow(()->missing("EXAM_NOT_FOUND","Không tìm thấy bài thi"));
        owner(e.ownerId);
        e.status=ExamStatus.ARCHIVED;
        e.updatedAt=Instant.now();
    }
    @Transactional
    public SessionResponse start(StartSessionRequest i){
        if(i==null||i.examId()==null)throw new ApiException(HttpStatus.BAD_REQUEST,"EXAM_ID_REQUIRED","Thiếu mã bài thi");
        ExamEntity e=exams.findStartSnapshotById(i.examId()).orElseThrow(()->missing("EXAM_NOT_FOUND","Không tìm thấy bài thi"));
        Instant now=Instant.now();
        if(e.status!=ExamStatus.ACTIVE||e.opensAt!=null&&e.opensAt.isAfter(now)||e.closesAt!=null&&e.closesAt.isBefore(now))throw new ApiException(HttpStatus.CONFLICT,"EXAM_NOT_OPEN","Bài thi chưa mở hoặc đã đóng");
        if(examQuestions.countByExamId(e.id)==0)throw new ApiException(HttpStatus.CONFLICT,"EXAM_HAS_NO_QUESTIONS","Đề thi chưa có câu hỏi nên không thể bắt đầu");
        UUID u=actor();
        List<ExamSessionEntity> history=i.enrollmentId()==null?sessions.findAllByExamIdAndUserIdAndEnrollmentIdIsNullOrderByAttemptNoAsc(e.id,u):sessions.findAllByExamIdAndEnrollmentIdAndUserIdOrderByAttemptNoAsc(e.id,i.enrollmentId(),u);
        for(ExamSessionEntity attempt:history){
            if(attempt.status==ExamSessionStatus.IN_PROGRESS&&!now.isBefore(attempt.expiresAt)){
                attempt.status=ExamSessionStatus.EXPIRED;
                attempt.updatedAt=now;
                continue;
            }
            expireIfPastGrace(attempt,now);
            if(attempt.status==ExamSessionStatus.IN_PROGRESS)return session(attempt);
        }
        if(history.size()>=e.maxAttempts)throw new ApiException(HttpStatus.CONFLICT,"ATTEMPT_LIMIT","Bạn đã hết số lần làm bài");
        if(!history.isEmpty()&&e.waitMinutesBetweenAttempts>0){
            ExamSessionEntity latest=history.get(history.size()-1);
            Instant nextAllowed=attemptFinishedAt(latest).plusSeconds(e.waitMinutesBetweenAttempts*60L);
            if(now.isBefore(nextAllowed))throw new ApiException(HttpStatus.CONFLICT,"ATTEMPT_WAIT_REQUIRED","Bạn cần chờ trước khi bắt đầu lần làm tiếp theo");
        }
        ExamSessionEntity s=new ExamSessionEntity();
        s.examId=e.id;
        s.examVersion=e.examVersion;
        s.userId=u;
        s.enrollmentId=i.enrollmentId();
        s.courseId=e.courseId;
        s.lessonId=e.lessonId;
        s.attemptNo=history.size()+1;
        s.startedAt=now;
        s.expiresAt=now.plusSeconds(e.durationMinutes*60L);
        s.graceUntil=s.expiresAt.plusSeconds(120);
        s.lastHeartbeatAt=now;
        captureSessionSnapshot(s,e,contexts.findById(e.id).orElse(null));
        return session(sessions.save(s));
    }
    private SessionResponse session(ExamSessionEntity s){
        List<ExamQuestionView> snapshot=sessionQuestions(s);
        long remaining=s.status==ExamSessionStatus.IN_PROGRESS?Math.max(0,Duration.between(Instant.now(),s.expiresAt).getSeconds()):0;
        return new SessionResponse(s.id,s.examId,s.enrollmentId,s.courseId,s.lessonId,s.attemptNo,s.status,s.startedAt,s.expiresAt,s.graceUntil,remaining,s.lastHeartbeatAt,s.suspiciousEventCount,s.submittedAt,answers(s.answersJson),snapshot);
    }
    private ExamSessionEntity owned(UUID id){
        ExamSessionEntity s=sessions.findForUpdateById(id).orElseThrow(()->missing("SESSION_NOT_FOUND","Không tìm thấy phiên thi"));
        if(!Objects.equals(s.userId,actor()))throw new ApiException(HttpStatus.FORBIDDEN,"SESSION_FORBIDDEN","Phiên thi không thuộc tài khoản hiện tại");
        return s;
    }
    @Transactional
    public SessionResponse resume(UUID id){
        ExamSessionEntity s=owned(id);
        expireIfPastGrace(s,Instant.now());
        return session(s);
    }
    @Transactional
    public SessionResponse heartbeat(UUID id){
        ExamSessionEntity s=owned(id);
        Instant now=Instant.now();
        if(!expireIfPastGrace(s,now)&&s.status==ExamSessionStatus.IN_PROGRESS){
            s.lastHeartbeatAt=now;
            s.updatedAt=now;
        }
        return session(s);
    }
    @Transactional
    public SessionResponse save(UUID id,SaveAnswersRequest i){
        ExamSessionEntity s=owned(id);
        Instant now=Instant.now();
        if(expireIfPastGrace(s,now))return session(s);
        if(s.status!=ExamSessionStatus.IN_PROGRESS)throw new ApiException(HttpStatus.CONFLICT,"SESSION_LOCKED","Phiên thi đã khóa");
        if(i==null)throw new ApiException(HttpStatus.BAD_REQUEST,"ANSWERS_REQUIRED","Thiếu dữ liệu đáp án");
        Set<String> allowed=sessionQuestions(s).stream().map(x->x.id().toString()).collect(Collectors.toSet());
        if(allowed.isEmpty())throw new ApiException(HttpStatus.CONFLICT,"EXAM_HAS_NO_QUESTIONS","Đề thi không có câu hỏi");
        if(!allowed.containsAll(i.answers().keySet()))throw new ApiException(HttpStatus.BAD_REQUEST,"ANSWER_QUESTION_MISMATCH","Đáp án chứa câu hỏi không thuộc phiên thi");
        String value=json(i.answers());
        if(value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>1_048_576)throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"ANSWERS_TOO_LARGE","Dữ liệu đáp án vượt quá 1 MiB");
        s.answersJson=value;
        s.updatedAt=now;
        return session(s);
    }
    @Transactional
    public SessionResponse submit(UUID id,String key){
        if(key==null||key.isBlank()||key.length()>160)throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_IDEMPOTENCY_KEY","Idempotency-Key không hợp lệ");
        Optional<ExamSessionEntity> existing=sessions.findBySubmitIdempotencyKey(key);
        if(existing.isPresent()){
            ExamSessionEntity previous=existing.get();
            if(!previous.id.equals(id)||!previous.userId.equals(actor()))throw new ApiException(HttpStatus.CONFLICT,"IDEMPOTENCY_KEY_REUSED","Idempotency-Key đã được dùng cho phiên khác");
            return session(previous);
        }
        ExamSessionEntity s=owned(id);
        Instant now=Instant.now();
        if(expireIfPastGrace(s,now))return session(s);
        if(s.status!=ExamSessionStatus.IN_PROGRESS)throw new ApiException(HttpStatus.CONFLICT,"SESSION_ALREADY_FINISHED","Phiên thi đã được kết thúc trước đó");
        if(sessionQuestions(s).isEmpty())throw new ApiException(HttpStatus.CONFLICT,"EXAM_HAS_NO_QUESTIONS","Không thể nộp một đề thi rỗng");
        s.status=ExamSessionStatus.SUBMITTED;
        s.submittedAt=now;
        s.submitIdempotencyKey=key;
        s.updatedAt=now;
        publisher.publish(EventTypes.EXAM_SUBMITTED,"assessment-service",s.id.toString(),new ExamSubmittedPayload(s.id,s.examId,s.userId,now));
        return session(s);
    }
    @Transactional
    public SessionEventResponse event(UUID id,SessionEventRequest i){
        ExamSessionEntity s=owned(id);
        ExamSessionEventEntity e=new ExamSessionEventEntity();
        e.sessionId=s.id;
        e.userId=s.userId;
        e.type=i.type();
        e.details=i.details();
        e.occurredAt=i.occurredAt()==null?Instant.now():i.occurredAt();
        if(e.type!=ExamSessionEventType.HEARTBEAT)s.suspiciousEventCount++;
        sessionEvents.save(e);
        return new SessionEventResponse(e.id,e.type,e.details,e.occurredAt,e.storedAt);
    }
    @Transactional(readOnly=true)
    public List<SessionEventResponse> events(UUID id){
        ExamSessionEntity s=owned(id);
        return sessionEvents.findAllBySessionIdOrderByOccurredAtAsc(s.id).stream().map(e->new SessionEventResponse(e.id,e.type,e.details,e.occurredAt,e.storedAt)).toList();
    }
    @Transactional(readOnly=true)
    public GradingPayload grading(UUID id){
        ExamSessionEntity s=sessions.findById(id).orElseThrow(()->missing("SESSION_NOT_FOUND","Không tìm thấy phiên thi"));
        if(s.status!=ExamSessionStatus.SUBMITTED&&s.status!=ExamSessionStatus.GRADED)throw new ApiException(HttpStatus.CONFLICT,"SESSION_NOT_SUBMITTED","Phiên thi chưa được nộp");
        ExamEntity e=exams.findById(s.examId).orElseThrow();
        AssessmentContextEntity c=contexts.findById(e.id).orElse(null);
        List<GradingQuestionPayload> qs=sessionGradingQuestions(s);
        if(qs.isEmpty())throw new ApiException(HttpStatus.CONFLICT,"EXAM_HAS_NO_QUESTIONS","Không thể chấm một đề thi rỗng");
        double passing=s.passingScoreSnapshot==null?e.passingScore:s.passingScoreSnapshot;
        boolean autoGrade=s.autoGradeSnapshot==null?(c==null||c.autoGrade):s.autoGradeSnapshot;
        AssessmentContextType context=s.contextTypeSnapshot==null?(c==null?inferred(e):c.contextType):s.contextTypeSnapshot;
        ScoreStrategy strategy=s.scoreStrategySnapshot==null?e.scoreStrategy:s.scoreStrategySnapshot;
        return new GradingPayload(s.id,e.id,s.userId,passing,answers(s.answersJson),qs,s.enrollmentId,s.courseId,s.lessonId,autoGrade,context,strategy,Duration.between(s.startedAt,s.submittedAt).toMillis(),s.submittedAt);
    }
    @Transactional
    public void markGraded(UUID id){
        ExamSessionEntity s=sessions.findById(id).orElseThrow(()->missing("SESSION_NOT_FOUND","Không tìm thấy phiên thi"));
        if(s.status==ExamSessionStatus.GRADED)return;
        if(s.status!=ExamSessionStatus.SUBMITTED)throw new ApiException(HttpStatus.CONFLICT,"SESSION_NOT_SUBMITTED","Chỉ có thể hoàn tất phiên đã nộp");
        s.status=ExamSessionStatus.GRADED;
        s.updatedAt=Instant.now();
    }
    @Transactional
    public List<QuestionResponse> importGenerated(List<GeneratedQuestionImport> items){
        List<QuestionResponse> result=new ArrayList<>();
        for(GeneratedQuestionImport i:items){
            QuestionRequest qr=new QuestionRequest(i.type(),i.prompt(),i.options(),i.correctAnswers(),i.explanation(),i.difficulty(),i.tags(),i.points());
            QuestionResponse saved=createQuestion(qr);
            QuestionProvenanceEntity p=new QuestionProvenanceEntity();
            p.questionId=saved.id();
            p.courseId=i.courseId();
            p.externalId=i.externalId();
            p.citationsJson=json(i.citations());
            p.sourceDocumentVersionsJson=json(i.sourceDocumentVersions());
            p.generatorMetadataJson=json(i.generatorMetadata());
            p.importedBy=actor();
            provenance.save(p);
            result.add(saved);
        }
        return result;
    }
    @Transactional
    public AssignmentResponse assign(UUID assessmentId,AssignmentRequest i){
        if(i.dueAt()!=null&&i.availableFrom()!=null&&!i.dueAt().isAfter(i.availableFrom()))throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_ASSIGNMENT_WINDOW","Hạn nộp phải sau thời gian mở");
        AssessmentAssignmentEntity e=assignments.findByAssessmentIdAndAssigneeTypeAndAssigneeId(assessmentId,i.assigneeType(),i.assigneeId()).orElseGet(AssessmentAssignmentEntity::new);
        e.assessmentId=assessmentId;
        e.assigneeType=i.assigneeType();
        e.assigneeId=i.assigneeId();
        e.availableFrom=i.availableFrom();
        e.dueAt=i.dueAt();
        e.required=i.required();
        e.status=AssessmentAssignmentStatus.ACTIVE;
        e.assignedBy=actor();
        e.updatedAt=Instant.now();
        return assignment(assignments.save(e));
    }
    private AssignmentResponse assignment(AssessmentAssignmentEntity e){
        return new AssignmentResponse(e.id,e.assessmentId,e.assigneeType,e.assigneeId,e.availableFrom,e.dueAt,e.required,e.status,e.assignedBy,e.assignedAt);
    }
    @Transactional(readOnly=true)
    public List<AssignmentResponse> assignments(UUID id){
        return assignments.findAllByAssessmentIdOrderByAssignedAtDesc(id).stream().map(this::assignment).toList();
    }
    @Transactional
    public void revokeAssignment(UUID id){
        AssessmentAssignmentEntity e=assignments.findById(id).orElseThrow(()->missing("ASSIGNMENT_NOT_FOUND","Không tìm thấy phân công"));
        e.status=AssessmentAssignmentStatus.REVOKED;
        e.updatedAt=Instant.now();
    }
    @Transactional
    public ExamResponse createCompetition(CompetitionRequest i){
        ExamRequest e=new ExamRequest(i.title(),null,null,AssessmentContextType.COMPETITION,null,true,i.durationMinutes(),i.opensAt(),i.closesAt(),i.maxAttempts(),0,i.passingScore(),true,true,ScoreStrategy.HIGHEST,ExamStatus.DRAFT,List.of());
        ExamResponse out=createExam(e);
        CompetitionEntity c=competitions.findById(out.id()).orElseGet(CompetitionEntity::new);
        c.assessmentId=out.id();
        c.registrationOpensAt=i.registrationOpensAt();
        c.registrationClosesAt=i.registrationClosesAt();
        c.leaderboardVisibility=i.leaderboardVisibility()==null?LeaderboardVisibility.AFTER_CLOSE:i.leaderboardVisibility();
        competitions.save(c);
        rewards.deleteAllByCompetitionId(out.id());
        if(i.rewards()!=null)for(RewardRequest r:i.rewards()){
            CompetitionRewardEntity x=new CompetitionRewardEntity();
            x.competitionId=out.id();
            x.rankFrom=r.rankFrom();
            x.rankTo=r.rankTo();
            x.rewardType=r.rewardType();
            x.rewardPayloadJson=json(r.payload());
            rewards.save(x);
        }
        return out;
    }
    @Transactional(readOnly=true)
    public List<LeaderboardEntry> leaderboard(UUID id){
        return leaderboard.findAllByCompetitionIdOrderByRankAsc(id).stream().map(e->new LeaderboardEntry(e.userId,e.attemptId,e.score.doubleValue(),e.durationMs,e.submittedAt,e.rank)).toList();
    }
    @Transactional
    public void publishCompetition(UUID id){
        CompetitionEntity c=competitions.findById(id).orElseThrow(()->missing("COMPETITION_NOT_FOUND","Không tìm thấy cuộc thi"));
        c.resultStatus=CompetitionResultStatus.PUBLISHED;
        c.publishedAt=Instant.now();
        c.publishedBy=actor();
    }

}
