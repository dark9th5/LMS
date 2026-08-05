package com.lmspilot.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class AiServiceApplication {
    public static void main(String[] args) { SpringApplication.run(AiServiceApplication.class, args); }
}
