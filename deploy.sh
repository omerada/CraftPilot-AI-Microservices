#!/bin/bash

# GitHub'dan en son değişiklikleri çek
git pull origin master

# .env dosyasını oluştur
cat << EOF > .env
DOCKERHUB_USERNAME=${DOCKERHUB_USERNAME}
EUREKA_USERNAME=${EUREKA_USERNAME}
EUREKA_PASSWORD=${EUREKA_PASSWORD}
GCP_SA_KEY_PATH=/opt/craftpilot/gcp-credentials.json
GCP_PROJECT_ID=${GCP_PROJECT_ID}
REDIS_PASSWORD=${REDIS_PASSWORD}
GMAIL_SERVICE_ACCOUNT=${GMAIL_SERVICE_ACCOUNT}
GRAFANA_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD}
VERSION=latest
EOF

# GCP credentials dosyasını oluştur
echo "${GCP_SA_KEY}" > /opt/craftpilot/gcp-credentials.json
chmod 600 /opt/craftpilot/gcp-credentials.json

# Docker Compose ile servisleri başlat
docker-compose pull
docker-compose up -d

# Servislerin durumunu kontrol et
docker-compose ps
