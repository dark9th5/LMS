# syntax=docker/dockerfile:1.7
ARG SERVICE
FROM eclipse-temurin:21-jre-alpine
ARG SERVICE
RUN apk add --no-cache curl && addgroup -S lmspilot && adduser -S lmspilot -G lmspilot
WORKDIR /app
COPY services/${SERVICE}/build/libs/*.jar /app/app.jar
USER lmspilot
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
