package com.lmspilot.assessment.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.assessment.domain.ExamEntity
import com.lmspilot.assessment.domain.ExamQuestionEntity
import com.lmspilot.assessment.domain.ExamQuestionRepository
import com.lmspilot.assessment.domain.ExamRepository
import com.lmspilot.assessment.domain.ExamSessionRepository
import com.lmspilot.assessment.domain.ExamStatus
import com.lmspilot.assessment.domain.QuestionEntity
import com.lmspilot.assessment.domain.QuestionRepository
import com.lmspilot.assessment.domain.QuestionStatus
import com.lmspilot.assessment.domain.QuestionType
import com.lmspilot.assessment.domain.ScoreStrategy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private const val DEMO_SEED_KEY = "lmspilot-demo-assessment-v2"
private val OWNER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
private val COURSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101")
private val EXAM_LESSON_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000116")
private val EXAM_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000303")

private data class SampleQuestion(
    val id: UUID,
    val type: QuestionType,
    val prompt: String,
    val options: List<String>,
    val correctAnswers: List<String>,
    val explanation: String,
    val tags: String,
)

@Component
class DevelopmentSeed(
    private val questions: QuestionRepository,
    private val exams: ExamRepository,
    private val examQuestions: ExamQuestionRepository,
    private val sessions: ExamSessionRepository,
    private val mapper: ObjectMapper,
    private val jdbc: JdbcTemplate,
    @Value("\${lmspilot.seed-demo:false}") private val enabled: Boolean,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled || alreadyApplied()) return
        val now = Instant.now()
        val samples = listOf(
            SampleQuestion(
                UUID.fromString("00000000-0000-0000-0000-000000000301"),
                QuestionType.SINGLE_CHOICE,
                "LMSPilot lưu nội dung khóa học và tiến độ theo cách nào?",
                listOf("Qua các service và cơ sở dữ liệu thật", "Chỉ giữ tạm trên giao diện", "Chỉ lưu trong trình duyệt", "Không lưu dữ liệu"),
                listOf("Qua các service và cơ sở dữ liệu thật"),
                "Các thao tác thật phải được lưu qua API và cơ sở dữ liệu, không dùng fallback giả.",
                "lmspilot,persistence",
            ),
            SampleQuestion(
                UUID.fromString("00000000-0000-0000-0000-000000000302"),
                QuestionType.TRUE_FALSE,
                "Khóa học đã có lịch sử học tập nên được lưu trữ thay vì xóa vật lý.",
                listOf("Đúng", "Sai"),
                listOf("Đúng"),
                "Lưu trữ giúp giữ nguyên tiến độ, điểm và dữ liệu đối soát.",
                "lmspilot,safety",
            ),
            SampleQuestion(
                UUID.fromString("00000000-0000-0000-0000-000000000304"),
                QuestionType.MULTIPLE_CHOICE,
                "Những loại tài nguyên nào có trong Bài 0?",
                listOf("Video MP4", "Tài liệu PDF", "Checklist DOCX", "Tệp thực thi EXE"),
                listOf("Video MP4", "Tài liệu PDF", "Checklist DOCX"),
                "File Storage Service cho phép các định dạng học liệu an toàn và chặn loại tệp nguy hiểm.",
                "lmspilot,files",
            ),
            SampleQuestion(
                UUID.fromString("00000000-0000-0000-0000-000000000305"),
                QuestionType.SINGLE_CHOICE,
                "Ai có thể chỉnh sửa khóa học thuộc phạm vi được phân công?",
                listOf("Chủ sở hữu hoặc quản trị viên có quyền", "Mọi học viên", "Người chưa đăng nhập", "Bất kỳ thiết bị nào trong LAN"),
                listOf("Chủ sở hữu hoặc quản trị viên có quyền"),
                "Quyền phải được kiểm tra ở backend, không chỉ ẩn nút trên giao diện.",
                "lmspilot,roles",
            ),
            SampleQuestion(
                UUID.fromString("00000000-0000-0000-0000-000000000306"),
                QuestionType.TRUE_FALSE,
                "Sau khi thêm hoặc sửa bài học, tải lại trang vẫn phải thấy dữ liệu vừa lưu.",
                listOf("Đúng", "Sai"),
                listOf("Đúng"),
                "Đây là kiểm tra đơn giản để phân biệt CRUD thật với dữ liệu chỉ tồn tại trong bộ nhớ giao diện.",
                "lmspilot,crud",
            ),
        )

        val savedQuestions = samples.map { sample -> upsertQuestion(sample, now) }
        val existingExam = exams.findById(EXAM_ID).orElse(null)
        val exam = existingExam ?: ExamEntity(id = EXAM_ID, ownerId = OWNER_ID)
        if (existingExam == null || !sessions.existsByExamId(EXAM_ID)) {
            exam.title = "Bài 0 - Kiểm tra làm quen LMSPilot"
            exam.courseId = COURSE_ID
            exam.lessonId = EXAM_LESSON_ID
            exam.durationMinutes = 10
            exam.maxAttempts = 3
            exam.waitMinutesBetweenAttempts = 0
            exam.passingScore = 70.0
            exam.shuffleQuestions = false
            exam.shuffleAnswers = false
            exam.scoreStrategy = ScoreStrategy.HIGHEST
            exam.status = ExamStatus.ACTIVE
            exam.ownerId = OWNER_ID
            exam.updatedAt = now
            exams.save(exam)

            examQuestions.deleteAllByExamId(EXAM_ID)
            examQuestions.flush()
            savedQuestions.forEachIndexed { index, question ->
                examQuestions.save(
                    ExamQuestionEntity(
                        examId = EXAM_ID,
                        questionId = question.id,
                        questionVersion = question.questionVersion,
                        type = question.type,
                        promptSnapshot = question.prompt,
                        optionsSnapshotJson = question.optionsJson,
                        correctAnswersSnapshotJson = question.correctAnswersJson,
                        points = 2.0,
                        sortOrder = index + 1,
                    )
                )
            }
        }

        jdbc.update("INSERT INTO demo_seed_history(seed_key, applied_at) VALUES (?, ?)", DEMO_SEED_KEY, now)
    }

    private fun upsertQuestion(sample: SampleQuestion, now: Instant): QuestionEntity {
        val entity = questions.findById(sample.id).orElseGet {
            QuestionEntity(id = sample.id, ownerId = OWNER_ID)
        }
        entity.ownerId = OWNER_ID
        entity.type = sample.type
        entity.prompt = sample.prompt
        entity.optionsJson = mapper.writeValueAsString(sample.options)
        entity.correctAnswersJson = mapper.writeValueAsString(sample.correctAnswers)
        entity.explanation = sample.explanation
        entity.difficulty = 1
        entity.tagsCsv = sample.tags
        entity.defaultPoints = 2.0
        entity.status = QuestionStatus.ACTIVE
        entity.updatedAt = now
        return questions.save(entity)
    }

    private fun alreadyApplied(): Boolean = jdbc.queryForObject(
        "SELECT EXISTS(SELECT 1 FROM demo_seed_history WHERE seed_key = ?)",
        Boolean::class.java,
        DEMO_SEED_KEY,
    ) == true
}
