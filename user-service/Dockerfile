# Build stage-client-lib stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builderib-builder

# Maven build optimization for redis-client-lib
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# Set up working directoryles (this will come from the GitHub Action)
WORKDIR /applient-lib/pom.xml ./
COPY redis-client-lib/src ./src/
# Create custom Maven settings with local filesystem repository
RUN echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \l redis-client-lib to local Maven repository
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \RUN mvn clean install -DskipTests
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"> \
  <localRepository>/usr/share/maven/ref/repository</localRepository> \
  <profiles> \
    <profile> \
      <id>local-lib</id> \mization
      <repositories> \X:+TieredCompilation -XX:TieredStopAtLevel=1"
        <repository> \
          <id>local-maven-repo</id> \y
          <url>file:///app/maven-repo</url> \
        </repository> \
      </repositories> \l Maven repository from redis-lib-builder
    </profile> \t/.m2/repository/com/craftpilot /root/.m2/repository/com/craftpilot/
  </profiles> \
  <activeProfiles> \iles for dependency resolution
    <activeProfile>local-lib</activeProfile> \COPY pom.xml /app/
  </activeProfiles> \
</settings>' > /usr/share/maven/ref/settings.xmllib which is already in local .m2)
RUN mvn dependency:go-offline -DskipTests || echo "Some dependencies may not be available, continuing with build"
# Copy local Maven repo with redis-client-lib
COPY maven-repo/ /app/maven-repo/
COPY src /app/src/
# Copy POM files for dependency resolution
COPY pom.xml /app/

# Try to resolve dependencies with local repository
RUN mvn -s /usr/share/maven/ref/settings.xml dependency:go-offline -DskipTests || echo "Continuing with build"
e-temurin:21-jre-alpine
# Copy source code
COPY src /app/src/
























ENTRYPOINT ["java", "-jar", "app.jar"]# Start the applicationEXPOSE 8080# Expose the application port  CMD curl -f http://localhost:8080/actuator/health || exit 1HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \# Configure health checkRUN apk add --no-cache curl# Add curl for health checksCOPY --from=builder /app/target/*.jar app.jar# Copy the built jar fileWORKDIR /appFROM eclipse-temurin:21-jre-alpine# Runtime stageRUN mvn -s /usr/share/maven/ref/settings.xml package -DskipTests# Package the application# Copy the built jar file
COPY --from=user-service-builder /app/target/*.jar app.jar

# Add curl for health checks
RUN apk add --no-cache curl

# Configure health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose the application port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]