FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create necessary directories
RUN mkdir -p /app/config

COPY --from=build /build/target/*.jar app.jar

# Environment variables will be provided during runtime
ENV SPRING_PROFILES_ACTIVE=prod \
    EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://${EUREKA_USERNAME}:${EUREKA_PASSWORD}@eureka-server:8761/eureka/ \
    SPRING_REDIS_HOST=${REDIS_HOST} \
    SPRING_REDIS_PORT=${REDIS_PORT} \
    SPRING_REDIS_PASSWORD=${REDIS_PASSWORD} \
    GCP_PROJECT_ID=${GCP_PROJECT_ID} \
    OPENAI_API_KEY=${OPENAI_API_KEY} \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8096
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]