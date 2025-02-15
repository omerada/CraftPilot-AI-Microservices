# Build stage
FROM maven:3.9.5-eclipse-temurin-21-alpine as builder

WORKDIR /build
COPY shared-config /build/shared-config
COPY analytics-service /build/analytics-service

RUN cd /build/shared-config && mvn clean install -DskipTests
RUN cd /build/analytics-service && mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /build/analytics-service/target/*.jar app.jar

EXPOSE 8064
ENTRYPOINT ["java", "-jar", "app.jar"]