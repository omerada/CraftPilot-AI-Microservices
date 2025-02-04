# Build stage
FROM --platform=$BUILDPLATFORM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM --platform=$TARGETPLATFORM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8096
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]