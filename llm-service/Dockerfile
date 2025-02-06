# Build stage
FROM --platform=$BUILDPLATFORM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Maven build
RUN mvn clean package -DskipTests

# Run stage
FROM --platform=$TARGETPLATFORM eclipse-temurin:21-jdk

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# JVM ayarları
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV GOOGLE_APPLICATION_CREDENTIALS=/gcp-credentials.json

EXPOSE 8062
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]