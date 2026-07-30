package com.lmspilot.assessment.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.assessment.domain.ExamEntity
import com.lmspilot.assessment.domain.ExamQuestionEntity
import com.lmspilot.assessment.domain.ExamQuestionRepository
import com.lmspilot.assessment.domain.ExamRepository
import com.lmspilot.assessment.domain.ExamStatus
import com.lmspilot.assessment.domain.QuestionEntity
import com.lmspilot.assessment.domain.QuestionRepository
import com.lmspilot.assessment.domain.QuestionType
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class DevelopmentSeed(
    private val questions: QuestionRepository,
    private val exams: ExamRepository,
    private val examQuestions: ExamQuestionRepository,
    private val mapper: ObjectMapper,
    @Value("\${lmspilot.seed-demo:false}") private val enabled: Boolean,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled || exams.count() > 0) return
        val ownerId = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val q1 = questions.save(
            QuestionEntity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000301"),
                ownerId = ownerId,
                type = QuestionType.SINGLE_CHOICE,
                prompt = "LMSPilot được thiết kế vận hành chính trong môi trường nào?",
                optionsJson = mapper.writeValueAsString(listOf("Mạng LAN nội bộ", "Mạng xã hội", "Sàn thương mại điện tử", "Thiết bị không có máy chủ")),
                correctAnswersJson = mapper.writeValueAsString(listOf("Mạng LAN nội bộ")),
                difficulty = 1,
                tagsCsv = "on-premise,lan",
            )
        )
        val q2 = questions.save(
            QuestionEntity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000302"),
                ownerId = ownerId,
                type = QuestionType.TRUE_FALSE,
                prompt = "Kết quả do AI local tạo ra phải được giảng viên duyệt trước khi sử dụng.",
                optionsJson = mapper.writeValueAsString(listOf("Đúng", "Sai")),
                correctAnswersJson = mapper.writeValueAsString(listOf("Đúng")),
                difficulty = 1,
                tagsCsv = "ai,review",
            )
        )
        val exam = exams.save(
            ExamEntity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000303"),
                title = "Kiểm tra làm quen LMSPilot",
                courseId = UUID.fromString("00000000-0000-0000-0000-000000000101"),
                durationMinutes = 15,
                maxAttempts = 3,
                passingScore = 70.0,
                status = ExamStatus.ACTIVE,
                ownerId = ownerId,
            )
        )
        listOf(q1, q2).forEachIndexed { index, question ->
            examQuestions.save(
                ExamQuestionEntity(
                    examId = exam.id,
                    questionId = question.id,
                    questionVersion = question.questionVersion,
                    type = question.type,
                    promptSnapshot = question.prompt,
                    optionsSnapshotJson = question.optionsJson,
                    correctAnswersSnapshotJson = question.correctAnswersJson,
                    points = 5.0,
                    sortOrder = index + 1,
                )
            )
        }
    }
}
