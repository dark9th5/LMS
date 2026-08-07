plugins {
    `java-library`
    id("io.spring.dependency-management")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.16") }
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
