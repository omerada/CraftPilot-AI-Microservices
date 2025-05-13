FROM openjdk:21-slim as builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

COPY src ./src/
RUN ./mvnw clean package -DskipTests

FROM openjdk:21-slim

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# MongoDB connection
ENV MONGODB_URI=mongodb://craftpilot:secure_password@mongodb:27017/analytics?authSource=admin&retryWrites=true&w=majority
ENV MONGODB_DATABASE=analytics

# Eureka configuration
ENV EUREKA_SERVER_URL=http://craftpilot:13579ada@eureka-server:8761/eureka/

# Spring and application configs
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8064

EXPOSE 8064

CMD ["java", "-Xms512m", "-Xmx1g", "-jar", "app.jar"]