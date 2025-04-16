#!/bin/bash

# Check if Java process is running
if ! pgrep -f "java -jar app.jar" > /dev/null; then
    echo "Java process is not running"
    exit 1
fi

# Check if the application is responding on the health endpoint
if ! curl -f http://localhost:8085/actuator/health > /dev/null 2>&1; then
    echo "Application health check failed"
    exit 1
fi

echo "Container is healthy"
exit 0
