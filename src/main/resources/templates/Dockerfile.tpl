FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle
COPY version.properties version.properties
COPY gradle.properties gradle.properties

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /app/build/libs/*.jar app.jar
COPY scripts/entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["/entrypoint.sh"]
