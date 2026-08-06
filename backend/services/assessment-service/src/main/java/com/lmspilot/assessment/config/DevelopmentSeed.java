package com.lmspilot.assessment.config;

import com.lmspilot.assessment.domain.*;
import com.lmspilot.assessment.platform.AssessmentContextType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
public class DevelopmentSeed {
    @Bean
    CommandLineRunner seedAssessment(QuestionRepository questions, ExamRepository exams, AssessmentContextRepository contexts, AssessmentAssignmentRepository assignments,
                                    @Value("${lmspilot.seed-demo:true}") boolean enabled) {
        return args -> {
            if (!enabled) return;

            UUID instructorId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000003");
            UUID questionId1 = UUID.fromString("00000000-0000-0000-0003-000000000001");
            UUID examId1 = UUID.fromString("00000000-0000-0000-0003-000000000010");

            if (questions.count() == 0) {
                QuestionEntity q1 = new QuestionEntity();
                q1.id = questionId1;
                q1.ownerId = instructorId;
                q1.type = QuestionType.SINGLE_CHOICE;
                q1.prompt = "LMSPilot 0.21.0 sử dụng những công nghệ cốt lõi nào cho hệ thống Backend?";
                q1.optionsJson = "[\"Java 21 + Spring Boot 3.5\",\"Node.js + Express\",\"Python + Django\",\"PHP + Laravel\"]";
                q1.correctAnswersJson = "[\"Java 21 + Spring Boot 3.5\"]";
                q1.explanation = "Backend LMSPilot 0.21.0 được nâng cấp hoàn toàn sang Java 21 LTS và Spring Boot 3.5.16.";
                q1.difficulty = 1;
                q1.tagsCsv = "java,spring-boot,architecture";
                q1.defaultPoints = 10;
                q1.status = QuestionStatus.ACTIVE;
                questions.save(q1);

                QuestionEntity q2 = new QuestionEntity();
                q2.ownerId = instructorId;
                q2.type = QuestionType.SINGLE_CHOICE;
                q2.prompt = "Cơ sở dữ liệu chính được LMSPilot sử dụng là gì?";
                q2.optionsJson = "[\"PostgreSQL 17\",\"MySQL 8\",\"MongoDB\",\"SQLite\"]";
                q2.correctAnswersJson = "[\"PostgreSQL 17\"]";
                q2.explanation = "LMSPilot sử dụng PostgreSQL với mỗi microservice sở hữu schema riêng biệt.";
                q2.difficulty = 1;
                q2.tagsCsv = "database,postgresql";
                q2.defaultPoints = 10;
                q2.status = QuestionStatus.ACTIVE;
                questions.save(q2);
            }

            if (contexts.findById(examId1).isEmpty()) {
                AssessmentContextEntity ctx = new AssessmentContextEntity();
                ctx.assessmentId = examId1;
                ctx.contextType = AssessmentContextType.STANDALONE_EXAM;
                ctx.maxAttempts = 3;
                ctx.autoGrade = true;
                contexts.save(ctx);
            }

            if (exams.count() == 0) {
                ExamEntity e1 = new ExamEntity();
                e1.id = examId1;
                e1.title = "Bài kiểm tra Đánh giá Kiến thức Kiến trúc LMSPilot 0.21.0";
                e1.durationMinutes = 45;
                e1.passingScore = 70;
                e1.maxAttempts = 3;
                e1.status = ExamStatus.ACTIVE;
                e1.ownerId = instructorId;
                exams.save(e1);
            }

            if (assignments.count() == 0) {
                AssessmentAssignmentEntity a1 = new AssessmentAssignmentEntity();
                a1.assessmentId = examId1;
                a1.assigneeType = AssessmentAssigneeType.USER;
                a1.assigneeId = studentId;
                a1.assignedBy = instructorId;
                a1.assignedAt = Instant.now();
                assignments.save(a1);
            }
        };
    }
}
