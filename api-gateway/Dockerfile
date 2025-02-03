FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Copy the project files
COPY . .

# Build the application
WORKDIR /build/api-gateway
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create config directory and set permissions
RUN mkdir -p /app/config && \
    addgroup -S spring && \
    adduser -S spring -G spring && \
    chown -R spring:spring /app

# Copy the built artifact and config files
COPY --from=build /build/api-gateway/target/*.jar app.jar
COPY --from=build /build/api-gateway/src/main/resources/firebase-service-account.json /app/config/
RUN chown spring:spring /app/config/firebase-service-account.json

USER spring:spring

EXPOSE 8090

ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dfirebase.credentials.path=/app/config/firebase-service-account.json", \
            "-jar", "app.jar"]