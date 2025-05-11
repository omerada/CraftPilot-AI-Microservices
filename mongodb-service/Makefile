.PHONY: help up down status logs backup restore shell mongo-shell create-network test-connection

# Yardım mesajı
help:
	@echo "Craftpilot MongoDB Servis Yönetimi"
	@echo ""
	@echo "Kullanım:"
	@echo "  make up              - MongoDB ve Mongo Express'i başlat"
	@echo "  make down            - MongoDB ve Mongo Express'i durdur"
	@echo "  make restart         - MongoDB ve Mongo Express'i yeniden başlat"
	@echo "  make status          - Container durumlarını göster"
	@echo "  make logs            - MongoDB loglarını göster"
	@echo "  make backup          - Veritabanı yedeğini al"
	@echo "  make restore file=X  - Belirtilen yedeği geri yükle (örn: make restore file=backup_20230301_120000.tar.gz)"
	@echo "  make shell           - MongoDB container'ında shell aç"
	@echo "  make mongo-shell     - MongoDB console'a bağlan"
	@echo "  make create-network  - Craftpilot ağını oluştur (eğer oluşturulmadıysa)"
	@echo "  make test-connection - MongoDB ve Mongo Express arasındaki bağlantıyı test et"

# Docker ağını oluştur
create-network:
	docker network inspect craftpilot-network >/dev/null 2>&1 || docker network create craftpilot-network

# Containerleri başlat
up: create-network
	docker-compose up -d
	@echo "MongoDB ve Mongo Express başlatıldı"
	@echo "MongoDB: localhost:27017"
	@echo "Mongo Express: http://localhost:8081"

# Containerleri durdur
down:
	docker-compose down
	@echo "MongoDB ve Mongo Express durduruldu"

# Containerleri yeniden başlat
restart: down up

# Container durumunu göster
status:
	docker-compose ps
	@echo ""
	@docker exec craftpilot-mongodb mongosh --quiet --eval "db.serverStatus().connections"

# MongoDB loglarını göster
logs:
	docker-compose logs --tail=100 -f mongodb

# Veritabanı yedeğini al
backup:
	@echo "Veritabanı yedekleniyor..."
	@cd scripts && bash backup.sh
	@echo "Yedekleme tamamlandı"

# Veritabanı yedeğini geri yükle
restore:
	@if [ -z "$(file)" ]; then \
		echo "Hata: Geri yüklenecek dosya belirtilmedi!"; \
		echo "Kullanım: make restore file=backup_20230301_120000.tar.gz"; \
		exit 1; \
	fi
	@echo "Veritabanı geri yükleniyor: $(file)"
	@cd scripts && bash restore.sh $(file)
	@echo "Geri yükleme tamamlandı"

# MongoDB container'ında shell aç
shell:
	docker exec -it craftpilot-mongodb bash

# MongoDB console'a bağlan
mongo-shell:
	docker exec -it craftpilot-mongodb mongosh -u $$(grep MONGO_ROOT_USERNAME .env | cut -d '=' -f2) -p $$(grep MONGO_ROOT_PASSWORD .env | cut -d '=' -f2) --authenticationDatabase admin

# Docker servisleri arasındaki bağlantıyı test et
test-connection:
	@echo "MongoDB ve Mongo Express arasındaki bağlantıyı test etme..."
	@docker exec craftpilot-mongo-express ping -c 3 craftpilot-mongodb || echo "Network bağlantısı başarısız"
	@echo "\nMongoDB'ye bağlantı testi..."
	@docker exec craftpilot-mongo-express curl -s craftpilot-mongodb:27017 | grep "MongoDB" || echo "MongoDB bağlantısı başarısız"
	@echo "\nKimlik doğrulama testi..."
	@docker exec craftpilot-mongodb mongosh --quiet --eval "db.version()" admin -u $$(grep MONGO_ROOT_USERNAME .env | cut -d '=' -f2) -p $$(grep MONGO_ROOT_PASSWORD .env | cut -d '=' -f2) || echo "MongoDB kimlik doğrulama başarısız"
