#!/bin/bash

if ! pgrep -f "java -jar app.jar" > /dev/null; then
    echo "Java process is not running"
    exit 1
fi

if ! curl -sf http://localhost:8085/health > /dev/null; then
    echo "Application health check failed"
    exit 1
fi

exit 0
