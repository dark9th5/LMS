package com.lmspilot.competency

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.lmspilot"])
class CompetencyServiceApplication

fun main(args: Array<String>) {
    runApplication<CompetencyServiceApplication>(*args)
}
