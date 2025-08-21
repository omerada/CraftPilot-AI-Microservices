# 🛡️ Deployment Security Guide - CraftPilot AI

## 🚨 Critical Security Steps for Production Deployment

### 1. **Pre-Deployment Security Checklist**

#### ✅ Password & Secrets Management

```bash
# ❌ NEVER use these default values in production:
REDIS_PASSWORD=CHANGE_ME_IN_PRODUCTION
EUREKA_PASSWORD=CHANGE_ME_IN_PRODUCTION

# ✅ Generate strong passwords:
REDIS_PASSWORD=$(openssl rand -base64 32)
EUREKA_PASSWORD=$(openssl rand -base64 32)
OPENROUTER_API_KEY=your_actual_api_key_here
```

#### ✅ Environment Configuration

```bash
# Copy template and configure
cp .env.example .env

# Edit .env with your secure values
nano .env
```

#### ✅ Firebase Security

```bash
# Secure Firebase service account placement
sudo mkdir -p /etc/gcp/credentials/
sudo chmod 700 /etc/gcp/credentials/
sudo cp your-firebase-service-account.json /etc/gcp/credentials/gcp-credentials.json
sudo chmod 600 /etc/gcp/credentials/gcp-credentials.json
sudo chown root:root /etc/gcp/credentials/gcp-credentials.json
```

### 2. **Docker Deployment Security**

#### 🔐 Secure Docker Compose Example

```yaml
version: "3.8"
services:
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    environment:
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
    networks:
      - craftpilot-network
    restart: unless-stopped

  eureka-server:
    image: craftpilot/eureka-server:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - EUREKA_PASSWORD=${EUREKA_PASSWORD}
      - SPRING_SECURITY_USER_PASSWORD=${EUREKA_PASSWORD}
    networks:
      - craftpilot-network
    restart: unless-stopped

  api-gateway:
    image: craftpilot/api-gateway:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - EUREKA_PASSWORD=${EUREKA_PASSWORD}
      - FIREBASE_PROJECT_ID=${FIREBASE_PROJECT_ID}
    volumes:
      - /etc/gcp/credentials:/etc/gcp/credentials:ro
    networks:
      - craftpilot-network
    restart: unless-stopped
    depends_on:
      - redis
      - eureka-server

networks:
  craftpilot-network:
    driver: bridge

volumes:
  redis-data:
```

### 3. **Kubernetes Security Configuration**

#### 🗝️ Kubernetes Secrets

```yaml
# secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: craftpilot-secrets
  namespace: craftpilot
type: Opaque
data:
  redis-password: $(echo -n "your_secure_redis_password" | base64)
  eureka-password: $(echo -n "your_secure_eureka_password" | base64)
  openrouter-api-key: $(echo -n "your_openrouter_api_key" | base64)

---
apiVersion: v1
kind: Secret
metadata:
  name: firebase-credentials
  namespace: craftpilot
type: Opaque
data:
  service-account.json: $(cat firebase-service-account.json | base64 -w 0)
```

#### 🚀 Secure Deployment Example

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: craftpilot
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
        - name: api-gateway
          image: craftpilot/api-gateway:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: REDIS_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: craftpilot-secrets
                  key: redis-password
            - name: EUREKA_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: craftpilot-secrets
                  key: eureka-password
            - name: FIREBASE_PROJECT_ID
              value: "your-firebase-project-id"
          volumeMounts:
            - name: firebase-credentials
              mountPath: /etc/gcp/credentials
              readOnly: true
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
      volumes:
        - name: firebase-credentials
          secret:
            secretName: firebase-credentials
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
```

### 4. **Network Security**

#### 🔒 Firewall Rules

```bash
# Allow only necessary ports
sudo ufw enable
sudo ufw default deny incoming
sudo ufw default allow outgoing

# API Gateway (public)
sudo ufw allow 443/tcp
sudo ufw allow 80/tcp

# Internal services (restrict to internal network)
sudo ufw allow from 10.0.0.0/8 to any port 8761  # Eureka
sudo ufw allow from 10.0.0.0/8 to any port 6379  # Redis
sudo ufw allow from 10.0.0.0/8 to any port 9092  # Kafka

# Monitoring (restrict to monitoring network)
sudo ufw allow from 10.0.1.0/24 to any port 9090  # Prometheus
sudo ufw allow from 10.0.1.0/24 to any port 3000  # Grafana
```

#### 🌐 SSL/TLS Configuration

```nginx
# nginx.conf for SSL termination
server {
    listen 443 ssl http2;
    server_name api.craftpilot.io;

    ssl_certificate /etc/ssl/certs/craftpilot.crt;
    ssl_certificate_key /etc/ssl/private/craftpilot.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;

    location / {
        proxy_pass http://api-gateway:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 5. **Security Monitoring**

#### 📊 Security Logging

```yaml
# promtail-config.yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: security-logs
    static_configs:
      - targets:
          - localhost
        labels:
          job: security-audit
          __path__: /var/log/craftpilot/*.log
    pipeline_stages:
      - match:
          selector: '{job="security-audit"}'
          stages:
            - regex:
                expression: ".*(?P<level>ERROR|WARN|SECURITY).*"
            - labels:
                level:
```

#### 🚨 Security Alerts

```yaml
# prometheus-alerts.yaml
groups:
  - name: craftpilot-security
    rules:
      - alert: UnauthorizedAccessAttempt
        expr: rate(http_requests_total{status=~"401|403"}[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High rate of unauthorized access attempts"

      - alert: SuspiciousServiceCommunication
        expr: rate(service_requests_total{status="500"}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Unusual service communication patterns detected"
```

### 6. **Backup & Recovery Security**

#### 💾 Secure Backup Strategy

```bash
#!/bin/bash
# secure-backup.sh

# Encrypt backups
gpg --cipher-algo AES256 --compress-algo 1 --s2k-mode 3 \
    --s2k-digest-algo SHA512 --s2k-count 65536 --symmetric \
    --output redis-backup-$(date +%Y%m%d).gpg redis-dump.rdb

# Secure transfer
rsync -avz --progress redis-backup-$(date +%Y%m%d).gpg \
    backup-server:/secure-backups/craftpilot/

# Clean local encrypted backup after successful transfer
rm redis-backup-$(date +%Y%m%d).gpg
```

### 7. **Incident Response**

#### 🚨 Security Incident Playbook

```bash
# If security breach detected:

# 1. Immediate isolation
kubectl scale deployment --replicas=0 --all -n craftpilot

# 2. Rotate all credentials
kubectl delete secret craftpilot-secrets -n craftpilot
# Create new secrets with fresh credentials

# 3. Review logs
kubectl logs -l app=api-gateway -n craftpilot --since=24h | grep -i "error\|unauthorized\|attack"

# 4. Restore from secure backup
# Follow backup restoration procedures

# 5. Gradual service restoration
kubectl scale deployment api-gateway --replicas=1 -n craftpilot
# Monitor and scale up gradually
```

### 8. **Security Validation Commands**

```bash
# Test Redis security
redis-cli -h localhost -p 6379 -a $REDIS_PASSWORD ping

# Verify Firebase authentication
curl -H "Authorization: Bearer invalid_token" \
     http://localhost:8080/api/users/profile
# Should return 401 Unauthorized

# Check SSL configuration
openssl s_client -connect api.craftpilot.io:443 -servername api.craftpilot.io

# Verify network isolation
nmap -p 6379,8761,9092 localhost
# Internal ports should not be accessible externally
```

---

## 🔗 Additional Resources

- [OWASP Security Guidelines](https://owasp.org/www-project-top-ten/)
- [Spring Security Best Practices](https://spring.io/guides/topicals/spring-security-architecture/)
- [Docker Security](https://docs.docker.com/engine/security/)
- [Kubernetes Security](https://kubernetes.io/docs/concepts/security/)

---

**⚠️ Remember: Security is an ongoing process, not a one-time setup. Regularly review and update your security measures.**
