plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    `java-library`
    id("io.spring.dependency-management")
}

kotlin { jvmToolchain(21) }

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.16") }
}

dependencies {
    api(project(":platform-contracts"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    api("org.springframework.boot:spring-boot-starter-amqp")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("io.micrometer:micrometer-registry-prometheus")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("org.jetbrains.kotlin:kotlin-reflect")
}
