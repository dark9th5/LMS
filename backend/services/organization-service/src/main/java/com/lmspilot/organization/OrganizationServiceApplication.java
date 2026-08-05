package com.lmspilot.organization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class OrganizationServiceApplication {
    public static void main(String[] args) { SpringApplication.run(OrganizationServiceApplication.class, args); }
}
