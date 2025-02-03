FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy parent pom.xml first
COPY ../pom.xml /app/parent/pom.xml

# Copy project files
WORKDIR /app/subscription-service
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Fix line endings and make mvnw executable
RUN sed -i 's/\r$//' mvnw && \
    chmod +x mvnw

# Download dependencies
RUN --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/subscription-service/target/*.jar app.jar

EXPOSE 8091

ENTRYPOINT ["java", "-jar", "app.jar"]