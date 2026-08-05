package com.lmspilot.license;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class LicenseServiceApplication {
    public static void main(String[] args) { SpringApplication.run(LicenseServiceApplication.class, args); }
}
