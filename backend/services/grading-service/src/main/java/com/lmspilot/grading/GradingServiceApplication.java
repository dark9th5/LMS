package com.lmspilot.grading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class GradingServiceApplication {
    public static void main(String[] args) { SpringApplication.run(GradingServiceApplication.class, args); }
}
