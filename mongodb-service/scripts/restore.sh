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

# Yedekleme dosyasını kontrol et
if [ -z "$1" ]; then
  echo "Hata: Geri yüklenecek yedek dosyası belirtilmedi!"
  echo "Kullanım: $0 backup_dosyası.tar.gz"
  exit 1
fi

BACKUP_FILE=$1
BACKUP_DIR="../backups"
TEMP_DIR="$BACKUP_DIR/temp_restore"

# Dosyanın varlığını kontrol et
if [ ! -f "$BACKUP_DIR/$BACKUP_FILE" ]; then
  echo "Hata: $BACKUP_DIR/$BACKUP_FILE bulunamadı!"
  exit 1
fi

echo "MongoDB geri yükleme işlemi başlatılıyor: $BACKUP_FILE"

# Geçici dizini temizle ve oluştur
rm -rf $TEMP_DIR
mkdir -p $TEMP_DIR

# Yedek dosyasını aç
tar -xzf "$BACKUP_DIR/$BACKUP_FILE" -C $TEMP_DIR

# Geri yüklenecek veritabanını belirle
RESTORE_DB_DIR=$(find $TEMP_DIR -type d -name "$MONGO_INITDB_DATABASE")

if [ -z "$RESTORE_DB_DIR" ]; then
  echo "Hata: Yedek içinde $MONGO_INITDB_DATABASE veritabanı bulunamadı!"
  rm -rf $TEMP_DIR
  exit 1
fi

# Veritabanını Docker container'ına kopyala
docker exec -it craftpilot-mongodb mkdir -p /data/db/restore
docker cp $RESTORE_DB_DIR/. craftpilot-mongodb:/data/db/restore

# mongorestore komutunu çalıştır
docker exec -it craftpilot-mongodb mongorestore \
  --host localhost \
  --port 27017 \
  --username $MONGO_ROOT_USERNAME \
  --password $MONGO_ROOT_PASSWORD \
  --authenticationDatabase admin \
  --db $MONGO_INITDB_DATABASE \
  --drop \
  /data/db/restore

# Geçici dizinleri temizle
docker exec craftpilot-mongodb rm -rf /data/db/restore
rm -rf $TEMP_DIR

echo "Geri yükleme işlemi tamamlandı: $BACKUP_FILE -> $MONGO_INITDB_DATABASE"
