# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Gerekli araçları yükle
RUN yum install -y tar gzip

# Maven wrapper ve bağımlılıkları kopyala
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./ 
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Kaynak kodları kopyala ve derle
COPY src ./src/
RUN ./mvnw clean package -DskipTests

# Copy the local Maven repository from the build context
COPY target/*.jar app.jar

# Final stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built artifact from builder stage
COPY --from=builder app.jar app.jar

# Set the entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]