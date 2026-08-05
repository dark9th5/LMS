package com.lmspilot.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class ConfigurationServiceApplication {
    public static void main(String[] args) { SpringApplication.run(ConfigurationServiceApplication.class, args); }
}
