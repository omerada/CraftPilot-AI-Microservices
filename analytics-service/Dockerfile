# Build stage
FROM --platform=$BUILDPLATFORM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Maven build için platform ayarı
ARG TARGETARCH
RUN echo "Building for $TARGETARCH"
RUN mvn clean package -DskipTests

# Run stage
FROM --platform=$TARGETPLATFORM eclipse-temurin:21-jre-slim

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# Platform-specific JVM ayarları
ARG TARGETARCH
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
RUN echo "Running on $TARGETARCH architecture"

EXPOSE 8096
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]