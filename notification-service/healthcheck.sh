#!/bin/sh
set -e

# Check if GCP credentials file exists
if [ ! -f "/app/config/gcp-credentials.json" ]; then
    echo "GCP credentials file not found"
    exit 1
fi

# Check if Firebase credentials file exists
if [ ! -f "/app/config/firebase-service-account.json" ]; then
    echo "Firebase credentials file not found"
    exit 1
fi

# Wait for port to be available
timeout 5 sh -c 'until nc -z localhost 8053; do sleep 1; done'

# Check actuator health endpoint
response=$(curl -s http://localhost:8053/actuator/health)
if echo "$response" | grep -q '"status":"UP"'; then
    exit 0
fi
exit 1
