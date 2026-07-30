package com.lmspilot.reporting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.lmspilot"])
class ReportingServiceApplication

fun main(args: Array<String>) {
    runApplication<ReportingServiceApplication>(*args)
}
