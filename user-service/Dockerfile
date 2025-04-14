# 1. Redis-client-lib build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS redis-lib-builder

# Set working directory for redis-client-lib
WORKDIR /redis-client-lib

# Copy redis-client-lib files
COPY redis-client-lib/pom.xml ./
COPY redis-client-lib/src ./src/

# Build and install redis-client-lib to local Maven repository
RUN mvn clean install -DskipTests

# 2. User-service build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS user-service-builderent-lib/ /app/redis-client-lib/ || true

# Maven build optimizationnstall it first
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
parent && \
# Set working directory for user-servicerent/pom.xml && \
WORKDIR /app
kipTests; \
# Copy the local Maven repository from redis-lib-builder
COPY --from=redis-lib-builder /root/.m2/repository/com/craftpilot /root/.m2/repository/com/craftpilot/
client-lib if exists in build context
# Copy user-service POM filep/redis-client-lib/pom.xml" ]; then \
COPY user-service/pom.xml ./
DskipTests; \
# Download dependencies
RUN mvn dependency:go-offline -DskipTests || echo "Some dependencies may not be available, continuing with build"

# Copy user-service source codeecho "Some dependencies may not be available, continuing with build"
COPY user-service/src ./src/

# Package the application
RUN mvn package -DskipTests

# 3. Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /appin:21-jre-alpine

# Copy the built jar fileWORKDIR /app
COPY --from=user-service-builder /app/target/*.jar app.jar
# Copy the built jar file
# Add curl for health checksCOPY --from=builder /app/target/*.jar app.jar
RUN apk add --no-cache curl
# Add curl for health checks
# Configure health checkRUN apk add --no-cache curl
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1# Configure health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
# Expose the application port  CMD curl -f http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
# Expose the application port
# Start the applicationEXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]