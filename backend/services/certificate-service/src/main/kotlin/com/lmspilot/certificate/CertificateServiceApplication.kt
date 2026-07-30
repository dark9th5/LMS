package com.lmspilot.certificate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.lmspilot"])
class CertificateServiceApplication

fun main(args: Array<String>) {
    runApplication<CertificateServiceApplication>(*args)
}
