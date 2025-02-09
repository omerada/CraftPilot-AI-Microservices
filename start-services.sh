#!/bin/bash

# GitHub Secrets'tan environment variables'ları al
export DOCKERHUB_USERNAME=$DOCKERHUB_USERNAME
export EUREKA_USERNAME=$EUREKA_USERNAME
export EUREKA_PASSWORD=$EUREKA_PASSWORD
export GCP_SA_KEY=$GCP_SA_KEY
export GCP_SA_KEY_PATH="/opt/craftpilot/gcp-credentials.json"

# GCP credentials dosyasını oluştur
echo "$GCP_SA_KEY" > $GCP_SA_KEY_PATH

# Utility functions
check_container() {
    local container_name=$1
    if docker ps -q -f name=^/${container_name}$ > /dev/null; then
        return 0
    fi
    return 1
}

# Infrastructure cleanup
cleanup_infrastructure() {
    echo "=== Cleaning up infrastructure ==="
    docker stop redis zookeeper kafka eureka-server 2>/dev/null || true
    docker rm redis zookeeper kafka eureka-server 2>/dev/null || true
    docker network rm craftpilot-network 2>/dev/null || true
}

# Infrastructure setup
setup_infrastructure() {
    echo "=== Setting up infrastructure ==="
    
    # 1. Network
    docker network create craftpilot-network 2>/dev/null || true
    
    # 2. Redis
    start_redis || return 1
    
    # 3. Zookeeper
    start_zookeeper || return 1
    
    # 4. Kafka (port değişikliğiyle)
    start_kafka || return 1
    
    # 5. Health Checks
    check_infrastructure || return 1
}

# Start Redis
start_redis() {
    if ! check_container "redis"; then
        echo "Starting Redis..."
        docker run -d \
            --name redis \
            --network craftpilot-network \
            -p 6379:6379 \
            -e REDIS_PASSWORD=${REDIS_PASSWORD} \
            redis:latest redis-server --requirepass ${REDIS_PASSWORD}
        sleep 10
        
        if ! docker exec redis redis-cli -a ${REDIS_PASSWORD} ping | grep -q "PONG"; then
            echo "Redis failed to start"
            return 1
        fi
    fi
}

# Start Zookeeper
start_zookeeper() {
    if ! check_container "zookeeper"; then
        echo "Starting Zookeeper..."
        docker run -d \
            --name zookeeper \
            --network craftpilot-network \
            -e ZOOKEEPER_CLIENT_PORT=2181 \
            -e ZOOKEEPER_TICK_TIME=2000 \
            confluentinc/cp-zookeeper:latest
        sleep 15
    fi
}

# Start Kafka
start_kafka() {
    if ! check_container "kafka"; then
        echo "Starting Kafka..."
        docker run -d \
            --name kafka \
            --network craftpilot-network \
            -p 9092:9092 \
            -e KAFKA_BROKER_ID=1 \
            -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
            -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:9092 \
            -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT \
            -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
            -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
            confluentinc/cp-kafka:latest
        sleep 20
    fi
}

# Check infrastructure health
check_infrastructure() {
    echo "=== Checking infrastructure health ==="
    sleep 30
    
    # Health check all services
    for service in redis zookeeper kafka; do
        if ! check_container $service; then
            echo "$service failed to start"
            return 1
        fi
    done
    
    echo "=== Infrastructure is healthy ==="
    return 0
}

# Start Eureka Server
start_eureka() {
    echo "=== Starting Eureka Server ==="
    docker run -d \
        --name eureka-server \
        --network craftpilot-network \
        -p 8761:8761 \
        -e SPRING_PROFILES_ACTIVE=prod \
        -e EUREKA_USERNAME=craftpilot \
        -e EUREKA_PASSWORD=${EUREKA_PASSWORD} \
        ${DOCKERHUB_USERNAME}/eureka-server:latest-arm64

    # Wait for Eureka
    for i in {1..30}; do
        if curl -sf "http://localhost:8761/actuator/health"; then
            echo "Eureka Server is healthy"
            return 0
        fi
        echo "Waiting for Eureka... Attempt $i/30"
        sleep 10
    done
    echo "Eureka Server failed to start"
    docker logs eureka-server
    return 1
}

# Main deployment process
main() {
    cleanup_infrastructure
    setup_infrastructure || exit 1
    start_eureka || exit 1
    
    # Core services
    docker-compose up -d eureka-server api-gateway
    sleep 30
    
    # Other services
    docker-compose up -d
}

# Run main function
main "$@"