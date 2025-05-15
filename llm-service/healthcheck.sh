#!/bin/bash
set -e

echo "Starting LLM Service health check..."

# Process kontrolü
if ! pgrep -f "java -jar app.jar" > /dev/null; then
    echo "Java process is not running"
    exit 1
fi

# Health endpoint kontrolü - port 8066 olarak düzeltildi
if ! curl -sf http://localhost:8066/health > /dev/null; then
    echo "Application health check failed on /health endpoint"
    
    # Alternatif olarak actuator endpoint'ini dene
    if ! curl -sf http://localhost:8066/actuator/health > /dev/null; then
        echo "Application health check also failed on /actuator/health endpoint"
        exit 1
    fi
fi

echo "Health check passed"
exit 0
