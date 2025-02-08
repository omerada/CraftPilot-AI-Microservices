#!/bin/bash

SERVICE_NAME="notification-service"
PORT=8053

# Redis'in hazır olmasını bekle
wait_for_redis() {
    echo "Waiting for Redis to be ready..."
    until nc -z redis 6379; do
        echo "Redis is unavailable - sleeping"
        sleep 2
    done
    echo "Redis is up"
}

# Ana servisin başlamasını bekle
wait_for_service() {
    echo "Waiting for service to be ready..."
    for i in {1..30}; do
        if curl -f http://localhost:${PORT}/actuator/health &>/dev/null; then
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
wait_for_redis

echo "=== Creating Firebase Credentials ==="
echo "${GCP_SA_KEY}" > /tmp/firebase-credentials.json

echo "=== Pulling ARM64 Image ==="
docker pull ${DOCKERHUB_USERNAME}/${SERVICE_NAME}:latest-arm64

echo "=== Stopping Old Container ==="
docker stop ${SERVICE_NAME} || true
docker rm ${SERVICE_NAME} || true

echo "=== Starting New Container ==="
docker run -d \
    --name ${SERVICE_NAME} \
    --network craftpilot-network \
    --restart unless-stopped \
    -p ${PORT}:${PORT} \
    -v /tmp/firebase-credentials.json:/app/firebase-credentials.json:ro \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://craftpilot:13579ada@eureka-server:8761/eureka/ \
    -e REDIS_HOST=redis \
    -e REDIS_PORT=6379 \
    -e REDIS_PASSWORD=13579ada \
    ${DOCKERHUB_USERNAME}/${SERVICE_NAME}:latest-arm64

echo "=== Waiting for Container to Start ==="
wait_for_service

echo "=== Cleanup ==="
rm -f /tmp/firebase-credentials.json 