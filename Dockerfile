# Multi-stage Dockerfile for Sankhya (Java 21 + Spring Boot 3.3.5)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy pom and source files directly
COPY pom.xml .
COPY .mvn .mvn
COPY src ./src

# Build production JAR with connection pooling disabled and retries to avoid 429 rate-limiting
RUN mvn clean package -DskipTests -B \
    -Dmaven.wagon.http.pool=false \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.http.retryHandler.requestSentRetryEnabled=true

# Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create data directory and permissions
RUN addgroup -S sankhyagroup && adduser -S sankhyauser -G sankhyagroup \
    && mkdir -p /app/data \
    && chown -R sankhyauser:sankhyagroup /app

USER sankhyauser

COPY --from=builder /app/target/sankhya-platform-1.0.0-SNAPSHOT.jar app.jar

# Render supplies PORT environment variable dynamically
ENV PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx420m -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
