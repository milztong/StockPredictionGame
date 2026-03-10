plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.projects"
version = "0.0.1-SNAPSHOT"
description = "Stock_Predictor"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web (REST API)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // JPA + Hibernate (database)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Validation (@Valid, @NotBlank, @Email, etc.)
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // PostgreSQL driver (Supabase)
    runtimeOnly("org.postgresql:postgresql")

    // OkHttp (Alpha Vantage API calls)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Jackson (JSON parsing)
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Lombok (reduces boilerplate)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
