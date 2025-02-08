#!/bin/sh
set -e

# Wait for port to be available
timeout 5 sh -c 'until nc -z localhost 8053; do sleep 1; done'

# Check actuator health endpoint
response=$(curl -s http://localhost:8053/actuator/health)
if echo "$response" | grep -q '"status":"UP"'; then
    exit 0
fi
exit 1
