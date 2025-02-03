FROM maven:3.9.6-eclipse-temurin-21-alpine AS build



WORKDIR /build



# Copy the project files

COPY pom.xml .

COPY image-service/pom.xml image-service/

COPY image-service/src image-service/src



# Build the application


WORKDIR /build/image-service

RUN mvn clean package -DskipTests



FROM eclipse-temurin:21-jre-alpine




WORKDIR /app



# Add non-root user

RUN addgroup -S spring && adduser -S spring -G spring

USER spring:spring



# Copy the built artifact

COPY --from=build /build/image-service/target/*.jar app.jar




EXPOSE 8099




ENTRYPOINT ["java", \

            "-XX:+UseContainerSupport", \

            "-XX:MaxRAMPercentage=75.0", \

            "-Djava.security.egd=file:/dev/./urandom", \

            "-jar", "app.jar"]
