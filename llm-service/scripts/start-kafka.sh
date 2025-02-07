#!/bin/bash

echo "=== Checking Docker Network ==="
if ! docker network ls | grep -q craftpilot-network; then
    echo "Creating craftpilot-network..."
    docker network create craftpilot-network
fi

echo "=== Stopping Existing Containers ==="
docker stop zookeeper kafka || true
docker rm zookeeper kafka || true

echo "=== Starting Zookeeper ==="
docker run -d \
    --name zookeeper \
    --network craftpilot-network \
    -e ZOOKEEPER_CLIENT_PORT=2181 \
    -e ZOOKEEPER_TICK_TIME=2000 \
    confluentinc/cp-zookeeper:latest

echo "=== Waiting for Zookeeper ==="
sleep 10

echo "=== Starting Kafka ==="
docker run -d \
    --name kafka \
    --network craftpilot-network \
    -p 9092:9092 \
    -e KAFKA_BROKER_ID=1 \
    -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092 \
    -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT \
    -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
    -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
    -e KAFKA_AUTO_CREATE_TOPICS_ENABLE="true" \
    confluentinc/cp-kafka:latest

echo "=== Waiting for Kafka ==="
sleep 20

echo "=== Checking Kafka Status ==="
if docker ps | grep -q kafka; then
    echo "Kafka is running"
else
    echo "Failed to start Kafka"
    exit 1
fi 