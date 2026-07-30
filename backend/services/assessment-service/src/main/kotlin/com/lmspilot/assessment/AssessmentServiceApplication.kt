package com.lmspilot.assessment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.lmspilot"])
class AssessmentServiceApplication

fun main(args: Array<String>) {
    runApplication<AssessmentServiceApplication>(*args)
}
