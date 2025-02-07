#!/bin/bash

SERVICE_NAME="llm-service"
PORT=8062

# Kafka'nın hazır olmasını bekle
wait_for_kafka() {
    echo "Waiting for Kafka to be ready..."
    until nc -z kafka 9092; do
        echo "Kafka is unavailable - sleeping"
        sleep 2
    done
    echo "Kafka is up"
}

# Ana servisin başlamasını bekle
wait_for_service() {
    echo "Waiting for service to be ready..."
    for i in {1..30}; do
        if curl -f http://localhost:8062/actuator/health &>/dev/null; then
            echo "Service is up"
            return 0
        fi
        echo "Service is unavailable - attempt $i - sleeping"
        sleep 2
    done
    echo "Service failed to start"
    return 1
}

echo "=== Starting Dependencies ==="
wait_for_kafka

echo "=== Starting Service ==="
java -jar app.jar &

echo "=== Waiting for Service to Start ==="
wait_for_service

echo "=== Creating GCP Credentials ==="
echo "${GCP_CREDENTIALS}" > /tmp/gcp-credentials.json

echo "=== Pulling ARM64 Image ==="
docker pull ${DOCKERHUB_USERNAME}/${SERVICE_NAME}:latest-arm64

echo "=== Stopping Old Container ==="
docker stop ${SERVICE_NAME} || true
docker rm ${SERVICE_NAME} || true

echo "=== Starting New Container ==="
docker run -d \
    --name ${SERVICE_NAME} \
    --platform linux/arm64 \
    -p ${PORT}:${PORT} \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://${EUREKA_USERNAME}:${EUREKA_PASSWORD}@eureka-server:8057/eureka/ \
    -e GOOGLE_APPLICATION_CREDENTIALS=/gcp-credentials.json \
    -v /tmp/gcp-credentials.json:/gcp-credentials.json:ro \
    --network craftpilot-network \
    ${DOCKERHUB_USERNAME}/${SERVICE_NAME}:latest-arm64

echo "=== Waiting for Container to Start ==="
wait_for_service 