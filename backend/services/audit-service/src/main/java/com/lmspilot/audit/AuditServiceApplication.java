package com.lmspilot.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class AuditServiceApplication {
    public static void main(String[] args) { SpringApplication.run(AuditServiceApplication.class, args); }
}
