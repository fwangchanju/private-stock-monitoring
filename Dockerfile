FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon -Dorg.gradle.jvmargs="-Xmx512m -Xms128m"

FROM eclipse-temurin:21-jre-jammy
LABEL org.opencontainers.image.source=https://github.com/fwangchanju/private-stock-monitoring
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
