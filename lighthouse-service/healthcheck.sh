#!/bin/bash

# Check if Java process is running
if ! pgrep -f "java -jar app.jar" > /dev/null; then
    echo "Java process is not running"
    exit 1
fi

# Check if the application is responding on the health endpoint
if ! curl -f http://localhost:8085/health -m 10 > /dev/null 2>&1; then
    echo "Application health check failed"
    exit 1
fi

# Check if Lighthouse CLI is accessible
if ! which lighthouse > /dev/null 2>&1; then
    echo "Lighthouse CLI not found in PATH"
    exit 1
fi

# Check if Node.js is accessible
if ! which node > /dev/null 2>&1; then
    echo "Node.js not found in PATH"
    exit 1
fi

echo "Container is healthy"
exit 0
