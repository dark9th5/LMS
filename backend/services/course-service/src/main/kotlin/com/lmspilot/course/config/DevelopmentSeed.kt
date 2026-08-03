package com.lmspilot.course.config

import com.lmspilot.course.domain.CourseCategoryEntity
import com.lmspilot.course.domain.CourseCategoryRepository
import com.lmspilot.course.domain.CourseEntity
import com.lmspilot.course.domain.CourseRepository
import com.lmspilot.course.domain.CourseStatus
import com.lmspilot.course.domain.LessonEntity
import com.lmspilot.course.domain.LessonRepository
import com.lmspilot.course.domain.LessonType
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private const val DEMO_SEED_KEY = "lmspilot-demo-course-v2"
private val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000100")
private val COURSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101")
private val OWNER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
private val PDF_FILE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000121")
private val DOCX_FILE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000122")
private val VIDEO_FILE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000123")

@Component
class DevelopmentSeed(
    private val categories: CourseCategoryRepository,
    private val courses: CourseRepository,
    private val lessons: LessonRepository,
    private val jdbc: JdbcTemplate,
    @Value("\${lmspilot.seed-demo:false}") private val enabled: Boolean,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled || alreadyApplied()) return

        val now = Instant.now()
        val category = categories.findById(CATEGORY_ID).orElseGet {
            categories.save(CourseCategoryEntity(id = CATEGORY_ID, code = "DIGITAL", name = "Năng lực số"))
        }
        val course = courses.findById(COURSE_ID).orElseGet {
            CourseEntity(id = COURSE_ID, code = "LMS-000", ownerId = OWNER_ID)
        }
        course.code = "LMS-000"
        course.name = "Bài 0 - Làm quen với LMSPilot"
        course.description = "Khóa học mẫu hoàn chỉnh giúp khách hàng trải nghiệm nội dung, video, PDF, DOCX, tiến độ và bài kiểm tra."
        course.objectives = "Biết sử dụng LMSPilot theo vai trò; mở và hoàn thành bài học; làm bài kiểm tra; hiểu cách quản lý nội dung thật."
        course.targetAudience = "Khách hàng dùng thử, quản trị viên, giảng viên và học viên mới"
        course.durationMinutes = 55
        course.passingScore = 70.0
        course.completionPolicyJson = "{\"requiredLessonPercent\":100}"
        course.categoryId = category.id
        course.status = CourseStatus.PUBLISHED
        course.publishedAt = course.publishedAt ?: now
        course.publishedBy = course.publishedBy ?: OWNER_ID
        course.ownerId = OWNER_ID
        course.updatedAt = now
        courses.save(course)

        val sampleLessons = listOf(
            LessonEntity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000111"),
                courseId = COURSE_ID,
                title = "0.1 Tổng quan và cách học trên LMSPilot",
                type = LessonType.TEXT,
                textContent = "Chào mừng bạn đến với LMSPilot. Hãy học lần lượt theo mục lục: đọc phần giới thiệu, xem video, mở PDF, tải checklist DOCX, thực hành và hoàn thành bài kiểm tra. Mọi tiến độ trong khóa học này được lưu trên hệ thống thật.",
                required = true,
                sortOrder = 1,
                estimatedMinutes = 8,
            ),
            LessonEntity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000112"),
                courseId = COURSE_ID,
                title = "0.2 Video giới thiệu hành trình học",
                type = LessonType.VIDEO,
                fileId = VIDEO_FILE_ID,
                required = true,
                sortOrder = 2,
                estimatedMinutes = 5,
            ),
            LessonEntity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000113"),
                courseId = COURSE_ID,
                title = "0.3 PDF hướng dẫn nhanh cho học viên",
                type = LessonType.PDF,
                fileId = PDF_FILE_ID,
                required = true,
                sortOrder = 3,
                estimatedMinutes = 12,
            ),
            LessonEntity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000114"),
                courseId = COURSE_ID,
                title = "0.4 DOCX checklist dành cho giảng viên",
                type = LessonType.DOCX,
                fileId = DOCX_FILE_ID,
                required = false,
                sortOrder = 4,
                estimatedMinutes = 10,
            ),
            LessonEntity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000115"),
                courseId = COURSE_ID,
                title = "0.5 Thực hành: thử chỉnh sửa nội dung mẫu",
                type = LessonType.ASSIGNMENT,
                textContent = "Quản trị viên hoặc giảng viên có thể đổi tên một bài học, thêm tài nguyên thử nghiệm rồi tải lại trang để xác nhận dữ liệu đã được lưu.",
                required = false,
                sortOrder = 5,
                estimatedMinutes = 10,
            ),
            LessonEntity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000116"),
                courseId = COURSE_ID,
                title = "0.6 Bài kiểm tra làm quen với LMSPilot",
                type = LessonType.EXAM,
                textContent = "Mở chức năng Bài kiểm tra và chọn đề ‘Bài 0 - Kiểm tra làm quen LMSPilot’. Kết quả câu khách quan được chấm tự động.",
                required = true,
                sortOrder = 6,
                estimatedMinutes = 10,
            ),
        )

        sampleLessons.forEach { sample ->
            val existing = lessons.findById(sample.id).orElse(null)
            if (existing == null) {
                lessons.save(sample)
            } else {
                existing.title = sample.title
                existing.type = sample.type
                existing.textContent = sample.textContent
                existing.fileId = sample.fileId
                existing.required = sample.required
                existing.sortOrder = sample.sortOrder
                existing.estimatedMinutes = sample.estimatedMinutes
                existing.updatedAt = now
            }
        }

        jdbc.update("INSERT INTO demo_seed_history(seed_key, applied_at) VALUES (?, ?)", DEMO_SEED_KEY, java.sql.Timestamp.from(now))
    }

    private fun alreadyApplied(): Boolean = jdbc.queryForObject(
        "SELECT EXISTS(SELECT 1 FROM demo_seed_history WHERE seed_key = ?)",
        Boolean::class.java,
        DEMO_SEED_KEY,
    ) == true
}
