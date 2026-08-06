package com.lmspilot.assessment.api;
import com.fasterxml.jackson.databind.JsonNode; import com.lmspilot.assessment.domain.*; import com.lmspilot.assessment.platform.AssessmentContextType; import jakarta.validation.constraints.*; import java.time.Instant; import java.util.*;
public final class AssessmentModels { private AssessmentModels(){}
 public record QuestionRequest(QuestionType type,@NotBlank String prompt,List<String> options,List<String> correctAnswers,String explanation,@Min(1) @Max(5) int difficulty,Set<String> tags,@DecimalMin(value="0.0",inclusive=false) double defaultPoints){ public QuestionRequest { options=options==null?List.of():options; correctAnswers=correctAnswers==null?List.of():correctAnswers; tags=tags==null?Set.of():tags; }}
 public record QuestionResponse(UUID id,QuestionType type,String prompt,List<String> options,List<String> correctAnswers,String explanation,int difficulty,Set<String> tags,double defaultPoints,QuestionStatus status,int version){}
 public record ExamQuestionInput(UUID questionId,@DecimalMin(value="0.0",inclusive=false) double points,@Min(0) int sortOrder){}
 public record ExamRequest(@NotBlank @Size(max=220) String title,UUID courseId,UUID lessonId,AssessmentContextType contextType,UUID cohortId,boolean autoGrade,@Min(1) @Max(480) int durationMinutes,Instant opensAt,Instant closesAt,@Min(1) @Max(20) int maxAttempts,@Min(0) int waitMinutesBetweenAttempts,@DecimalMin("0.0") @DecimalMax("100.0") double passingScore,boolean shuffleQuestions,boolean shuffleAnswers,ScoreStrategy scoreStrategy,ExamStatus status,List<ExamQuestionInput> questions){}
 public record ExamQuestionView(UUID id,QuestionType type,String prompt,List<String> options,double points,int sortOrder){}
 public record ExamResponse(UUID id,String title,UUID courseId,UUID lessonId,AssessmentContextType contextType,UUID cohortId,boolean autoGrade,int durationMinutes,Instant opensAt,Instant closesAt,int maxAttempts,int waitMinutesBetweenAttempts,double passingScore,boolean shuffleQuestions,boolean shuffleAnswers,ScoreStrategy scoreStrategy,ExamStatus status,int version,List<ExamQuestionView> questions){}
 public record StartSessionRequest(UUID examId,UUID enrollmentId){}
 public record SaveAnswersRequest(Map<String,JsonNode> answers){ public SaveAnswersRequest{answers=answers==null?Map.of():answers;} }
 public record SessionResponse(UUID id,UUID examId,UUID enrollmentId,UUID courseId,UUID lessonId,int attemptNo,ExamSessionStatus status,Instant startedAt,Instant expiresAt,Instant graceUntil,long remainingSeconds,Instant lastHeartbeatAt,int suspiciousEventCount,Instant submittedAt,Map<String,JsonNode> answers,List<ExamQuestionView> questions){}
 public record SessionEventRequest(ExamSessionEventType type,String details,Instant occurredAt){}
 public record SessionEventResponse(UUID id,ExamSessionEventType type,String details,Instant occurredAt,Instant storedAt){}
 public record GradingQuestionPayload(UUID questionId,QuestionType type,String prompt,List<String> correctAnswers,double points){}
 public record GradingPayload(UUID sessionId,UUID examId,UUID userId,double passingScore,Map<String,JsonNode> answers,List<GradingQuestionPayload> questions,UUID enrollmentId,UUID courseId,UUID lessonId,boolean autoGrade,AssessmentContextType contextType,ScoreStrategy scoreStrategy,long durationMs,Instant submittedAt){}
 public record MarkGradedRequest(double score,boolean passed){}
 public record GeneratedQuestionImport(UUID courseId,String externalId,QuestionType type,String prompt,List<String> options,List<String> correctAnswers,String explanation,int difficulty,Set<String> tags,double points,List<Map<String,Object>> citations,List<UUID> sourceDocumentVersions,Map<String,Object> generatorMetadata){}
 public record AssignmentRequest(AssessmentAssigneeType assigneeType,UUID assigneeId,Instant availableFrom,Instant dueAt,boolean required){}
 public record AssignmentResponse(UUID id,UUID assessmentId,AssessmentAssigneeType assigneeType,UUID assigneeId,Instant availableFrom,Instant dueAt,boolean required,AssessmentAssignmentStatus status,UUID assignedBy,Instant assignedAt){}
 public record CompetitionRequest(String title,Instant registrationOpensAt,Instant registrationClosesAt,Instant opensAt,Instant closesAt,int durationMinutes,int maxAttempts,double passingScore,LeaderboardVisibility leaderboardVisibility,List<RewardRequest> rewards){}
 public record RewardRequest(int rankFrom,int rankTo,String rewardType,Map<String,Object> payload){}
 public record LeaderboardEntry(UUID userId,UUID attemptId,double score,long durationMs,Instant submittedAt,Integer rank){}
}
