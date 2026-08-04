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
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation(kotlin("test"))
}
