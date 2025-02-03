FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build

# Copy the project files
COPY . .

# Build the application
WORKDIR /build/eureka-server
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Create config directory and set permissions
RUN mkdir -p /app/config && \
    groupadd -r spring && \
    useradd -r -g spring spring && \
    chown -R spring:spring /app

# Copy the built artifact
COPY --from=build /build/eureka-server/target/*.jar app.jar
RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8761

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://${SPRING_SECURITY_USER_NAME}:${SPRING_SECURITY_USER_PASSWORD}@localhost:8761/actuator/health || exit 1

ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]