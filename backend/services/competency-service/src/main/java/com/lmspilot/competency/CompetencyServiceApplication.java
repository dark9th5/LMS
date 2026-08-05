package com.lmspilot.competency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class CompetencyServiceApplication {
    public static void main(String[] args) { SpringApplication.run(CompetencyServiceApplication.class, args); }
}
