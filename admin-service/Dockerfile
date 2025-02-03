FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create config directory
RUN mkdir -p /app/config

COPY --from=build /build/target/*.jar app.jar

ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8097
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]