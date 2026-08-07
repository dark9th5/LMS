package com.lmspilot.course.config;

import com.lmspilot.course.domain.*;

import java.util.*;

import org.springframework.boot.CommandLineRunner;

import org.springframework.context.annotation.*;
@Configuration
@Profile("development")
public class DevelopmentSeed {
    @Bean CommandLineRunner seedCourses(CourseCategoryRepository categories,CourseRepository courses){
        return args->{
            if(categories.count()==0){
                CourseCategoryEntity c=new CourseCategoryEntity();
                c.code="GENERAL";
                c.name="Kiến thức chung";
                categories.save(c);
            }
            if(courses.count()==0){
                CourseEntity c=new CourseEntity();
                c.code="LMSPILOT-START";
                c.name="Bắt đầu với LMSPilot";
                c.description="Khóa học mẫu cho môi trường phát triển";
                c.ownerId=new UUID(0,1);
                courses.save(c);
            }

        }
        ;
    }

}
