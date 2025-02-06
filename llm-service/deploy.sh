#!/bin/bash

SERVICE_NAME="llm-service"
PORT=8062

# Health check function
wait_for_service() {
    local max_attempts=5
    local attempt=1
    local wait_time=10

    echo "=== Waiting for Container to Start ==="
    while [ $attempt -le $max_attempts ]; do
        if curl -f "http://localhost:$PORT/actuator/health" >/dev/null 2>&1; then
            echo "Service is healthy"
            return 0
        fi
        echo "Attempt $attempt: Waiting for service to start..."
        sleep $wait_time
        attempt=$((attempt + 1))
    done
    echo "Service failed health check"
    return 1
}

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