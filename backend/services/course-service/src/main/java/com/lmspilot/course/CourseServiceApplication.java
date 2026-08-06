package com.lmspilot.course;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class CourseServiceApplication {
    public static void main(String[] args) { SpringApplication.run(CourseServiceApplication.class, args); }
}
