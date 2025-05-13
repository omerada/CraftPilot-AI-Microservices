#!/bin/bash
set -e

# Env dosyasını yükle
if [ -f "../.env" ]; then
  source ../.env
else
  echo "Uyarı: .env dosyası bulunamadı, varsayılan değerler kullanılıyor..."
  MONGO_ROOT_USERNAME=${MONGO_ROOT_USERNAME:-craftpilot}
  MONGO_ROOT_PASSWORD=${MONGO_ROOT_PASSWORD:-secure_password}
  MONGO_INITDB_DATABASE=${MONGO_INITDB_DATABASE:-craftpilot}
  MONGO_PORT=${MONGO_PORT:-27017}
  MONGODB_HOST=${MONGODB_HOST:-craftpilot-mongodb}
fi

# Yedekleme dizini oluştur
BACKUP_DIR="../backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_PATH="$BACKUP_DIR/backup_$TIMESTAMP"

mkdir -p $BACKUP_DIR
mkdir -p $BACKUP_PATH

echo "MongoDB yedeklemesi başlatılıyor: $BACKUP_PATH"

# Docker container üzerinden mongodump çalıştır
docker exec craftpilot-mongodb mongodump \
  --host localhost \
  --port 27017 \
  --username $MONGO_ROOT_USERNAME \
  --password $MONGO_ROOT_PASSWORD \
  --authenticationDatabase admin \
  --db $MONGO_INITDB_DATABASE \
  --out /data/db/backup

# Yedeklenen dosyaları host sistemine kopyala
docker cp craftpilot-mongodb:/data/db/backup/. $BACKUP_PATH

# Container içindeki yedek dosyalarını temizle
docker exec craftpilot-mongodb rm -rf /data/db/backup

# Yedekleri sıkıştır
cd $BACKUP_DIR
tar -czf "backup_$TIMESTAMP.tar.gz" "backup_$TIMESTAMP"
rm -rf "backup_$TIMESTAMP"

echo "Yedekleme tamamlandı: $BACKUP_DIR/backup_$TIMESTAMP.tar.gz"

# Eski yedekleri temizle (30 günden eski)
find $BACKUP_DIR -name "backup_*.tar.gz" -type f -mtime +30 -delete
echo "30 günden eski yedekler temizlendi"
