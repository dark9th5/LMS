package com.lmspilot.filestorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.lmspilot")
public class FileStorageServiceApplication {
    public static void main(String[] args) { SpringApplication.run(FileStorageServiceApplication.class, args); }
}
