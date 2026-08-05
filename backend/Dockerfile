ARG SERVICE

FROM eclipse-temurin:21-jdk-alpine AS build
ARG SERVICE
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY platform-contracts ./platform-contracts
COPY service-support ./service-support
COPY services ./services
RUN chmod +x ./gradlew \
    && ./gradlew ":services:${SERVICE}:bootJar" --no-daemon --stacktrace

FROM eclipse-temurin:21-jre-alpine
ARG SERVICE
RUN apk add --no-cache curl \
    && addgroup -S lmspilot \
    && adduser -S lmspilot -G lmspilot
WORKDIR /app
COPY --from=build "/workspace/services/${SERVICE}/build/libs/" /tmp/libs/
RUN set -eux; \
    jar="$(find /tmp/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)"; \
    test -n "$jar"; \
    cp "$jar" /app/app.jar; \
    rm -rf /tmp/libs
USER lmspilot
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
