package com.lmspilot.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class AssessmentServiceApplication {
    public static void main(String[] args) { SpringApplication.run(AssessmentServiceApplication.class, args); }
}
