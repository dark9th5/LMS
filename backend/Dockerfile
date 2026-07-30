# syntax=docker/dockerfile:1.7
ARG SERVICE
FROM eclipse-temurin:21-jdk-alpine AS build
ARG SERVICE
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle chmod +x ./gradlew && \
    ./gradlew ":services:${SERVICE}:bootJar" --no-daemon --stacktrace && \
    cp "services/${SERVICE}/build/libs/"*.jar /tmp/app.jar

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl && addgroup -S lmspilot && adduser -S lmspilot -G lmspilot
WORKDIR /app
COPY --from=build /tmp/app.jar /app/app.jar
USER lmspilot
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
