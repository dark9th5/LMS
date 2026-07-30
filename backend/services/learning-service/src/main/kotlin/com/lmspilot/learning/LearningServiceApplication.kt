package com.lmspilot.learning

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.lmspilot"])
class LearningServiceApplication

fun main(args: Array<String>) {
    runApplication<LearningServiceApplication>(*args)
}
