package com.lmspilot.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class LearningServiceApplication {
    public static void main(String[] args) { SpringApplication.run(LearningServiceApplication.class, args); }
}
