FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Copy the project files
COPY pom.xml .
COPY aiquestion-service/pom.xml aiquestion-service/
COPY aiquestion-service/src aiquestion-service/src

# Build the application
WORKDIR /build/aiquestion-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built artifact
COPY --from=build /build/aiquestion-service/target/*.jar app.jar

EXPOSE 8095

ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]