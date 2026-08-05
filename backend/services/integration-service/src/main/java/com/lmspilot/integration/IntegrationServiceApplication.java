package com.lmspilot.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class IntegrationServiceApplication {
    public static void main(String[] args) { SpringApplication.run(IntegrationServiceApplication.class, args); }
}
