# Stage 1: Build the app with Gradle
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy gradle files first (better layer caching)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle/
COPY gradlew ./

# Download dependencies (cached if build files unchanged)
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# Stage 2: Run the app with a minimal JRE
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from stage 1
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
