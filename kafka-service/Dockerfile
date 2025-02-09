# Build stage - sadece ARM64
FROM --platform=linux/arm64 maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build

# Önce parent POM'u kopyala
COPY ../pom.xml parent-pom.xml
COPY pom.xml .
COPY src ./src

# Maven build için platform ayarı
ARG TARGETARCH=arm64
RUN echo "Building for ARM64 architecture"

# Parent POM'u yerel Maven repository'ye yükle
RUN mvn -f parent-pom.xml install -N -DskipTests

# Projeyi derle
RUN mvn clean package -DskipTests

# Run stage - sadece ARM64
FROM --platform=linux/arm64 eclipse-temurin:21-jdk

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# JVM ayarları
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
RUN echo "Running on ARM64 architecture"

EXPOSE 9093
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]