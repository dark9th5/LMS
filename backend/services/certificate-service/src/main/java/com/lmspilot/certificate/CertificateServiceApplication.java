package com.lmspilot.certificate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class CertificateServiceApplication {
    public static void main(String[] args) { SpringApplication.run(CertificateServiceApplication.class, args); }
}
