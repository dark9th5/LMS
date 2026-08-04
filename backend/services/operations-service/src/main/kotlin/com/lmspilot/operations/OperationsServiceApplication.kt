package com.lmspilot.operations
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
@EnableScheduling
@SpringBootApplication(scanBasePackages=["com.lmspilot"])
class OperationsServiceApplication
fun main(args:Array<String>) { runApplication<OperationsServiceApplication>(*args) }
