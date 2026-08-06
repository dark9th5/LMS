plugins {
    id("org.springframework.boot") version "3.5.16" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.lmspilot"
    version = "0.23.0"
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:deprecation", "-Xlint:unchecked"))
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
