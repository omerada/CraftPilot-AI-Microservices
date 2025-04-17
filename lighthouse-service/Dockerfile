FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x ./mvnw && ./mvnw package -DskipTests

FROM adoptopenjdk/openjdk17:alpine-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/app.jar

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 CMD wget -q -O /dev/null http://localhost:8085/health || exit 1

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]