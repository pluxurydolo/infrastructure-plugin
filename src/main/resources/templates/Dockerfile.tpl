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

RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app/logs && \
    chmod 755 /app/logs

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD netstat -tlnp 2>/dev/null | grep 8080 || exit 1

ENTRYPOINT ["/entrypoint.sh"]
