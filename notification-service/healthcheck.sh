#!/bin/sh
set -e

# Debug mode
echo "Starting health check..."

# Wait for Redis
nc -z redis 6379 || exit 1

# Wait for port to be available
timeout 5 sh -c 'until nc -z localhost 8053; do sleep 1; done'

# Check actuator health endpoint
response=$(curl -s -m 5 http://localhost:8053/actuator/health)
if echo "$response" | grep -q '"status":"UP"'; then
    echo "Health check passed"
    exit 0
fi

echo "Health check failed"
exit 1
