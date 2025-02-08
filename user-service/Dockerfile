# Build stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21 AS build

WORKDIR /build

# Tüm projeyi kopyala
COPY . .

# Maven kurulumu ve versiyonu belirleme
RUN yum install -y curl tar gzip
RUN curl -o apache-maven-3.9.7-bin.tar.gz https://dlcdn.apache.org/maven/maven-3/3.9.7/binaries/apache-maven-3.9.7-bin.tar.gz
RUN tar -xzf apache-maven-3.9.7-bin.tar.gz -C /opt/
RUN ln -s /opt/apache-maven-3.9.7 /opt/maven

ENV M2_HOME=/opt/maven
ENV MAVEN_HOME=/opt/maven
ENV PATH=${M2_HOME}/bin:${PATH}

# ARM64 uyumluluğu için Java seçeneklerini ayarla
ENV JAVA_TOOL_OPTIONS="-Dos.arch=aarch64"

# Projeyi derle
RUN mvn clean package -DskipTests

# Run stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# JVM ayarları
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_PORT=8080

RUN echo "Running on ARM64 architecture"

EXPOSE ${SERVER_PORT}
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${SERVER_PORT} -Dspring.profiles.active=prod -jar app.jar"]