package com.lmspilot.integration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.lmspilot"])
class IntegrationServiceApplication

fun main(args: Array<String>) {
    runApplication<IntegrationServiceApplication>(*args)
}
