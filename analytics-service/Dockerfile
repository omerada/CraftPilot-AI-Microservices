# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Cache Maven dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline

# Build the application
COPY src ./src/
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:21-slim

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# MongoDB connection
ENV MONGODB_URI=mongodb://craftpilot:secure_password@mongodb:27017/analytics?authSource=admin&retryWrites=true&w=majority
ENV MONGODB_DATABASE=analytics

# Eureka configuration
ENV EUREKA_SERVER_URL=http://craftpilot:13579ada@eureka-server:8761/eureka/

# Spring and application configs
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8064

# Healthcheck for container orchestration
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8064/actuator/health || exit 1

EXPOSE 8064

CMD ["java", "-Xms512m", "-Xmx1g", "-jar", "app.jar"]