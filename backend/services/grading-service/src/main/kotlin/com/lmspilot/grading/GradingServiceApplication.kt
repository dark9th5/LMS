package com.lmspilot.grading

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.lmspilot"])
class GradingServiceApplication

fun main(args: Array<String>) {
    runApplication<GradingServiceApplication>(*args)
}
