#!/bin/bash

# GitHub Secrets'tan environment variables'ları al
export DOCKERHUB_USERNAME=$DOCKERHUB_USERNAME
export EUREKA_USERNAME=$EUREKA_USERNAME
export EUREKA_PASSWORD=$EUREKA_PASSWORD
export GCP_SA_KEY=$GCP_SA_KEY
export GCP_CREDENTIALS_PATH="/opt/craftpilot/gcp-credentials.json"

# GCP credentials dosyasını oluştur
echo "$GCP_SA_KEY" > $GCP_CREDENTIALS_PATH

# Docker compose dosyasını çalıştır
docker-compose up -d

# Servislerin başlamasını bekle
echo "Servislerin başlaması bekleniyor..."
sleep 30

# Servislerin durumunu kontrol et
docker-compose ps

# Logları kontrol et
echo "Son loglar kontrol ediliyor..."
docker-compose logs --tail=50 