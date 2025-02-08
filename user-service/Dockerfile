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
RUN echo "Running on ARM64 architecture"

EXPOSE 8092
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]