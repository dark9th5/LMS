package com.lmspilot.course.config

import com.lmspilot.course.domain.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class DevelopmentSeed(
    private val categories: CourseCategoryRepository,
    private val courses: CourseRepository,
    private val lessons: LessonRepository,
    @Value("\${lmspilot.seed-demo:false}") private val enabled: Boolean,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled || courses.count() > 0) return
        val category = categories.save(CourseCategoryEntity(id = UUID.fromString("00000000-0000-0000-0000-000000000100"), code = "DIGITAL", name = "Năng lực số"))
        val owner = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val course = courses.save(CourseEntity(id = UUID.fromString("00000000-0000-0000-0000-000000000101"), code = "LMS-101", name = "Làm quen với LMSPilot", description = "Khóa học hướng dẫn sử dụng hệ thống", objectives = "Hiểu luồng học, bài kiểm tra và chứng chỉ", durationMinutes = 45, categoryId = category.id, ownerId = owner, status = CourseStatus.PUBLISHED, publishedAt = java.time.Instant.now(), publishedBy = owner))
        lessons.save(LessonEntity(id = UUID.fromString("00000000-0000-0000-0000-000000000111"), courseId = course.id, title = "Tổng quan hệ thống", type = LessonType.TEXT, textContent = "LMSPilot hỗ trợ toàn bộ vòng đời đào tạo trong mạng LAN.", sortOrder = 1, estimatedMinutes = 10))
        lessons.save(LessonEntity(id = UUID.fromString("00000000-0000-0000-0000-000000000112"), courseId = course.id, title = "Thực hành học tập", type = LessonType.TEXT, textContent = "Mở bài học, hoàn thành nội dung và theo dõi tiến độ.", sortOrder = 2, estimatedMinutes = 15))
    }
}
