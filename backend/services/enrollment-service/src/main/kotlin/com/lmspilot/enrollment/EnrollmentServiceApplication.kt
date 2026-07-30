package com.lmspilot.enrollment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.lmspilot"])
class EnrollmentServiceApplication

fun main(args: Array<String>) {
    runApplication<EnrollmentServiceApplication>(*args)
}
