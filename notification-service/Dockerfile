# Build stage
FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR /app
COPY notification-service/pom.xml .
COPY notification-service/src ./src
COPY pom.xml ../
RUN apk add --no-cache maven && \
    mvn package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN apk add --no-cache curl
EXPOSE 8094
ENTRYPOINT ["java", "-jar", "app.jar"]