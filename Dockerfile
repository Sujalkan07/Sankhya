# Multi-stage Dockerfile for Sankhya (Java 21 + Spring Boot 3.3.5)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
COPY .mvn .mvn
RUN mvn dependency:go-offline -B

# Build production JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

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
