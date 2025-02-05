# Build stage - sadece ARM64
FROM --platform=linux/arm64 maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Maven build için platform ayarı
ARG TARGETARCH=arm64
RUN echo "Building for ARM64 architecture"
RUN mvn clean package -DskipTests

# Run stage - sadece ARM64
FROM --platform=linux/arm64 eclipse-temurin:21-jdk

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# JVM ayarları
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
RUN echo "Running on ARM64 architecture"

EXPOSE 8063
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]