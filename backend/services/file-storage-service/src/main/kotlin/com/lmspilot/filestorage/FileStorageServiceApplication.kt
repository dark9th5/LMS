package com.lmspilot.filestorage

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.lmspilot"])
class FileStorageServiceApplication

fun main(args: Array<String>) {
    runApplication<FileStorageServiceApplication>(*args)
}
