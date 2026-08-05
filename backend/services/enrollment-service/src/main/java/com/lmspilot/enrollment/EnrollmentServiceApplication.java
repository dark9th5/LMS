package com.lmspilot.enrollment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class EnrollmentServiceApplication {
    public static void main(String[] args) { SpringApplication.run(EnrollmentServiceApplication.class, args); }
}
