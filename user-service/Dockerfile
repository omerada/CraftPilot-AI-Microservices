# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Maven build optimization
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# Set up working directory
WORKDIR /app

# Copy POM files for dependency resolution and caching
COPY pom.xml /app/

# If parent POM exists, copy and install it first
COPY ../pom.xml /app/parent-pom.xml
RUN if [ -f "/app/parent-pom.xml" ]; then \
        mkdir -p /app/parent && \
        mv /app/parent-pom.xml /app/parent/pom.xml && \
        cd /app/parent && \
        mvn install -DskipTests; \
    fi

# Handle redis-client-lib if exists
# We use COPY with an empty target directory to avoid errors if the source doesn't exist
# The shell operator '||' doesn't work properly in this context
COPY ["../redis-client-lib", "/app/redis-client-lib/"]
RUN if [ -d "/app/redis-client-lib" ] && [ -f "/app/redis-client-lib/pom.xml" ]; then \
        cd /app/redis-client-lib && \
        mvn install -DskipTests; \
    fi

# Cache Maven dependencies
RUN mvn dependency:go-offline || echo "Some dependencies may not be available, continuing with build"

# Copy source code
COPY src /app/src/

# Package the application
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar file
COPY --from=builder /app/target/*.jar app.jar

# Add curl for health checks
RUN apk add --no-cache curl

# Configure health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose the application port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]