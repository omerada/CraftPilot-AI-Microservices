#!/bin/bash

# Firebase kimlik bilgilerini ortam değişkeninden dosyaya çıkart (varsa)
if [ ! -z "$FIREBASE_CREDENTIALS_BASE64" ]; then
    echo "Decoding Firebase credentials from environment variable..."
    echo $FIREBASE_CREDENTIALS_BASE64 | base64 -d > /craftpilot/gcp-credentials.json
    chmod 600 /craftpilot/gcp-credentials.json
    echo "Firebase credentials saved to /craftpilot/gcp-credentials.json"
fi

# Uygulama jar dosyasını çalıştır
exec java ${JAVA_OPTS} -jar /app/app.jar
