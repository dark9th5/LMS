package com.lmspilot.course.config;

import com.lmspilot.course.domain.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
public class DevelopmentSeed {
    @Bean
    CommandLineRunner seedCourses(CourseCategoryRepository categories, CourseRepository courses, LessonRepository lessons,
                                  @Value("${lmspilot.seed-demo:true}") boolean enabled) {
        return args -> {
            if (!enabled) return;

            UUID genCatId = UUID.fromString("00000000-0000-0000-0001-000000000001");
            if (categories.count() == 0) {
                CourseCategoryEntity c1 = new CourseCategoryEntity();
                c1.id = genCatId;
                c1.code = "GENERAL";
                c1.name = "Kiến thức chung & Nhập môn";
                c1.sortOrder = 1;
                categories.save(c1);

                CourseCategoryEntity c2 = new CourseCategoryEntity();
                c2.id = UUID.fromString("00000000-0000-0000-0001-000000000002");
                c2.code = "TECHNICAL";
                c2.name = "Kỹ thuật & Công nghệ thông tin";
                c2.sortOrder = 2;
                categories.save(c2);
            }

            UUID courseId1 = UUID.fromString("00000000-0000-0000-0002-000000000001");
            if (courses.count() == 0) {
                CourseEntity c1 = new CourseEntity();
                c1.id = courseId1;
                c1.code = "LMSPILOT-START";
                c1.name = "Bắt đầu với LMSPilot 0.21.0";
                c1.description = "Khóa học mẫu hướng dẫn tổng quan về các tính năng của LMSPilot 0.21.0";
                c1.objectives = "Nắm vững cách quản lý khóa học, bài thi và theo dõi tiến độ học tập";
                c1.targetAudience = "Giảng viên và Học viên mới";
                c1.durationMinutes = 60;
                c1.passingScore = 70;
                c1.categoryId = genCatId;
                c1.status = CourseStatus.PUBLISHED;
                c1.contentVersion = 1;
                c1.publishedVersion = 1;
                c1.publishedAt = Instant.now();
                c1.ownerId = UUID.fromString("00000000-0000-0000-0000-000000000002"); // Instructor
                courses.save(c1);

                if (lessons.countByCourseId(courseId1) == 0) {
                    LessonEntity l1 = new LessonEntity();
                    l1.courseId = courseId1;
                    l1.title = "Bài 1: Giới thiệu giao diện và tổng quan hệ thống";
                    l1.type = LessonType.TEXT;
                    l1.textContent = "Chào mừng bạn đến với hệ thống quản lý học tập LMSPilot 0.21.0!";
                    l1.sortOrder = 1;
                    l1.required = true;
                    l1.estimatedMinutes = 15;
                    lessons.save(l1);

                    LessonEntity l2 = new LessonEntity();
                    l2.courseId = courseId1;
                    l2.title = "Bài 2: Quy trình làm bài kiểm tra và thi trực tuyến";
                    l2.type = LessonType.TEXT;
                    l2.textContent = "Hướng dẫn chi tiết quy trình thực hiện bài kiểm tra trắc nghiệm và tự luận.";
                    l2.sortOrder = 2;
                    l2.required = true;
                    l2.estimatedMinutes = 20;
                    lessons.save(l2);
                }

                CourseEntity c2 = new CourseEntity();
                c2.id = UUID.fromString("00000000-0000-0000-0002-000000000002");
                c2.code = "JAVA-MICROSERVICES";
                c2.name = "Lập trình Microservices với Java 21 & Spring Boot 3.5";
                c2.description = "Khóa học chuyên sâu về kiến trúc microservice hiện đại";
                c2.objectives = "Xây dựng các service độc lập giao tiếp qua REST API và RabbitMQ";
                c2.targetAudience = "Developer & Architect";
                c2.durationMinutes = 120;
                c2.passingScore = 80;
                c2.categoryId = UUID.fromString("00000000-0000-0000-0001-000000000002");
                c2.status = CourseStatus.PUBLISHED;
                c2.contentVersion = 1;
                c2.publishedVersion = 1;
                c2.publishedAt = Instant.now();
                c2.ownerId = UUID.fromString("00000000-0000-0000-0000-000000000002");
                courses.save(c2);
            }
        };
    }
}
