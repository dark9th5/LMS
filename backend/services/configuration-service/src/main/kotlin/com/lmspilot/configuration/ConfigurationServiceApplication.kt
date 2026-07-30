package com.lmspilot.configuration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.lmspilot"])
class ConfigurationServiceApplication

fun main(args: Array<String>) {
    runApplication<ConfigurationServiceApplication>(*args)
}
