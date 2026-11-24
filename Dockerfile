# Multi-stage Dockerfile for Sankhya (Java 21 + Spring Boot 3.3.5)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy Maven settings and project descriptors
COPY .mvn .mvn
COPY pom.xml .

# Install Google Cloud Maven Central mirror globally to bypass Sonatype 429 IP blocks
RUN mkdir -p /root/.m2 && cp .mvn/settings.xml /root/.m2/settings.xml

# Copy application source code
COPY src ./src

# Build production JAR using Google mirror
RUN mvn clean package -DskipTests -B -s .mvn/settings.xml

# Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create data directory and permissions
RUN addgroup -S sankhyagroup && adduser -S sankhyauser -G sankhyagroup \
    && mkdir -p /app/data \
    && chown -R sankhyauser:sankhyagroup /app

# Copy compressed dataset for automatic initial seeding
COPY data/naveen_store_unified_mart.csv.gz /app/data/
RUN chown -R sankhyauser:sankhyagroup /app/data

USER sankhyauser

COPY --from=builder /app/target/sankhya-platform-1.0.0-SNAPSHOT.jar app.jar

# Render supplies PORT environment variable dynamically
ENV PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx420m -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
