FROM maven:3.9.6-eclipse-temurin-21-alpine AS redis-lib-builder

# Redis Client Lib'i derle
WORKDIR /app/redis-client-lib
COPY ../redis-client-lib/pom.xml .
COPY ../redis-client-lib/src ./src
RUN mvn clean install -DskipTests

FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Maven dependency optimization
WORKDIR /app
COPY pom.xml .
# Maven repositorisi redis-client-lib'i içerecek
COPY --from=redis-lib-builder /root/.m2/repository /root/.m2/repository
RUN mvn dependency:go-offline

# Kaynak kodları kopyala ve build et
COPY src ./src
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]