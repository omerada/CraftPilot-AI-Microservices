#!/bin/bash

SERVICE_NAME="eureka-server"
PORT=8057

# Health check function
wait_for_service() {
    local max_attempts=5
    local attempt=1
    local wait_time=10

    echo "=== Waiting for Container to Start ==="
    while [ $attempt -le $max_attempts ]; do
        if curl -f -u "${EUREKA_USERNAME:-eureka}:${EUREKA_PASSWORD:-secret}" "http://localhost:$PORT/actuator/health" >/dev/null 2>&1; then
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

echo "=== Stopping Old Container ==="
docker stop ${SERVICE_NAME} || true
docker rm ${SERVICE_NAME} || true

echo "=== Starting New Container ==="
docker run -d \
    --name ${SERVICE_NAME} \
    --platform linux/arm64 \
    -p ${PORT}:${PORT} \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e EUREKA_USERNAME=${EUREKA_USERNAME:-eureka} \
    -e EUREKA_PASSWORD=${EUREKA_PASSWORD:-secret} \
    -e EUREKA_HOST=${EUREKA_HOST:-localhost} \
    --network craftpilot-network \
    ${DOCKERHUB_USERNAME}/${SERVICE_NAME}:latest-arm64

echo "=== Waiting for Container to Start ==="
wait_for_service 