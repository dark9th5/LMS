package com.lmspilot.course.config;

import com.lmspilot.course.application.interfaces.repository.ICourseCategoryRepository;
import com.lmspilot.course.application.interfaces.repository.ICourseRepository;
import com.lmspilot.course.domain.model.Course;
import com.lmspilot.course.domain.model.CourseCategory;

import java.util.*;

import org.springframework.boot.CommandLineRunner;

import org.springframework.context.annotation.*;

@Configuration
@Profile("development")
public class DevelopmentSeed {
    @Bean
    CommandLineRunner seedCourses(ICourseCategoryRepository categories, ICourseRepository courses) {
        return args -> {
            if (categories.count() == 0) {
                CourseCategory c = new CourseCategory();
                c.code = "GENERAL";
                c.name = "Kiến thức chung";
                categories.save(c);
            }
            if (courses.count() == 0) {
                Course c = new Course();
                c.code = "LMSPILOT-START";
                c.name = "Bắt đầu với LMSPilot";
                c.description = "Khóa học mẫu cho môi trường phát triển";
                c.ownerId = new UUID(0, 1);
                courses.save(c);
            }

        };
    }

}
