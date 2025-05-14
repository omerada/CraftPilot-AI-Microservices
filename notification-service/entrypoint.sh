#!/bin/sh
set -e

# Firebase kimlik bilgilerini kontrol et ve ortam değişkenlerini ayarla
if [ -f "/app/credentials/firebase-credentials.json" ]; then
  echo "Firebase credentials file found at /app/credentials/firebase-credentials.json"
  export FIREBASE_CONFIG="/app/credentials/firebase-credentials.json"
  export GOOGLE_APPLICATION_CREDENTIALS="/app/credentials/firebase-credentials.json"
  # Dosya izinlerini düzelt - okumanın yanı sıra çalıştırma izni de verelim
  chmod 644 "/app/credentials/firebase-credentials.json"
elif [ -f "/app/gcp-credentials.json" ]; then
  echo "Firebase credentials file found at /app/gcp-credentials.json"
  export FIREBASE_CONFIG="/app/gcp-credentials.json"
  export GOOGLE_APPLICATION_CREDENTIALS="/app/gcp-credentials.json"
  # Dosya izinlerini düzelt
  chmod 644 "/app/gcp-credentials.json" 
elif [ -f "/app/config/firebase-credentials.json" ]; then
  echo "Firebase credentials file found at /app/config/firebase-credentials.json"
  export FIREBASE_CONFIG="/app/config/firebase-credentials.json"
  export GOOGLE_APPLICATION_CREDENTIALS="/app/config/firebase-credentials.json"
  # Dosya izinlerini düzelt
  chmod 644 "/app/config/firebase-credentials.json"
elif [ -f "/craftpilot/gcp-credentials.json" ]; then
  echo "Firebase credentials file found at /craftpilot/gcp-credentials.json"
  export FIREBASE_CONFIG="/craftpilot/gcp-credentials.json"
  export GOOGLE_APPLICATION_CREDENTIALS="/craftpilot/gcp-credentials.json"
  # Dosya izinlerini düzelt
  chmod 644 "/craftpilot/gcp-credentials.json"
elif [ -f "/gcp-credentials.json" ]; then
  echo "Firebase credentials file found at /gcp-credentials.json"
  export FIREBASE_CONFIG="/gcp-credentials.json"
  export GOOGLE_APPLICATION_CREDENTIALS="/gcp-credentials.json"
  # Dosya izinlerini düzelt
  chmod 644 "/gcp-credentials.json"
elif [ -n "$FIREBASE_CONFIG" ]; then
  echo "Using Firebase credentials from FIREBASE_CONFIG environment variable"
  # FIREBASE_CONFIG değişkeni ayarlanmışsa, GOOGLE_APPLICATION_CREDENTIALS'e de aynı değeri atayalım
  export GOOGLE_APPLICATION_CREDENTIALS="$FIREBASE_CONFIG"
  # Dosya mevcutsa izinleri düzelt
  if [ -f "$FIREBASE_CONFIG" ]; then
    chmod 644 "$FIREBASE_CONFIG"
  fi
elif [ -n "$GOOGLE_APPLICATION_CREDENTIALS" ]; then
  echo "Using Firebase credentials from GOOGLE_APPLICATION_CREDENTIALS environment variable"
  # GOOGLE_APPLICATION_CREDENTIALS değişkeni ayarlanmışsa, FIREBASE_CONFIG'e de aynı değeri atayalım
  export FIREBASE_CONFIG="$GOOGLE_APPLICATION_CREDENTIALS"
  # Dosya mevcutsa izinleri düzelt
  if [ -f "$GOOGLE_APPLICATION_CREDENTIALS" ]; then
    chmod 644 "$GOOGLE_APPLICATION_CREDENTIALS"
  fi
else
  echo "WARNING: No Firebase credentials found. Service will run with Firebase features disabled."
  # Firebase'i devre dışı bırak
  export FIREBASE_ENABLED=false
fi

# Dosya erişim haklarını kontrol et
if [ -n "$FIREBASE_CONFIG" ] && [ -f "$FIREBASE_CONFIG" ]; then
  echo "Checking permissions for $FIREBASE_CONFIG"
  ls -la "$FIREBASE_CONFIG"
fi

if [ -n "$GOOGLE_APPLICATION_CREDENTIALS" ] && [ -f "$GOOGLE_APPLICATION_CREDENTIALS" ]; then
  echo "Checking permissions for $GOOGLE_APPLICATION_CREDENTIALS"
  ls -la "$GOOGLE_APPLICATION_CREDENTIALS"
fi

# Show environment variables for debugging (sensitive values redacted)
echo "Environment configuration:"
echo "FIREBASE_CONFIG=${FIREBASE_CONFIG}"
echo "GOOGLE_APPLICATION_CREDENTIALS=${GOOGLE_APPLICATION_CREDENTIALS}"
echo "FIREBASE_ENABLED=${FIREBASE_ENABLED:-true}"

# Start the application
echo "Starting notification service..."
exec java "${JAVA_OPTS}" -jar /app/app.jar
