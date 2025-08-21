# 🔐 CraftPilot AI - Security Guide

## 🚨 Security Overview

CraftPilot AI implements enterprise-grade security with Firebase Authentication, JWT tokens, and role-based access control.

## 🔑 Authentication

### Firebase Authentication

- **JWT Token** based authentication
- **Google, Email/Password** sign-in methods
- **Token validation** on every request
- **Automatic token refresh**

### API Authentication

```bash
# All API calls require Authorization header
Authorization: Bearer <firebase-jwt-token>
```

## 🛡️ Authorization

### Role-Based Access Control (RBAC)

- **USER**: Basic user operations
- **PREMIUM**: Enhanced features
- **ADMIN**: Administrative functions
- **SUPER_ADMIN**: System administration

### Endpoint Security

```java
// Example endpoint security
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public Mono<List<User>> getAllUsers() { ... }
```

## 🔒 Security Configuration

### API Gateway Security

```yaml
# Rate limiting
rate-limiting:
  enabled: true
  limit: 100
  period: 60s

# CORS configuration
cors:
  allowed-origins: ["https://app.craftpilot.io"]
  allowed-methods: ["GET", "POST", "PUT", "DELETE"]
```

### Service Security

- **HTTPS only** in production
- **JWT token validation** on each service
- **Service-to-service** authentication
- **Input validation** and sanitization

## 🔐 Data Protection

### Encryption

- **TLS 1.3** for data in transit
- **AES-256** for sensitive data at rest
- **Password hashing** with bcrypt
- **JWT signing** with RS256

### Privacy

- **PII encryption** in database
- **GDPR compliance** features
- **Data anonymization** for analytics
- **User data export/deletion**

## 🚫 Security Measures

### Input Validation

```java
@Valid @RequestBody UserCreateRequest request
```

### SQL Injection Prevention

- **Parameterized queries** only
- **ORM usage** (Spring Data)
- **Input sanitization**

### XSS Protection

```java
// Content Security Policy
.contentSecurityPolicy("default-src 'self'")
```

### CSRF Protection

- **CSRF tokens** for state-changing operations
- **SameSite cookies**
- **Origin validation**

## 🔍 Security Monitoring

### Audit Logging

- **Authentication attempts**
- **Authorization failures**
- **Sensitive operations**
- **Data access patterns**

### Security Metrics

```java
// Custom security metrics
@Counter(name = "security_violations_total")
@Timer(name = "authentication_duration")
```

### Alerts

- **Failed login attempts**
- **Suspicious activity patterns**
- **Rate limit violations**
- **Unauthorized access attempts**

## 🛠️ Security Testing

### Security Scans

```bash
# OWASP dependency check
mvn org.owasp:dependency-check-maven:check

# Static analysis
mvn spotbugs:check
```

### Penetration Testing

- **API security testing**
- **Authentication bypass attempts**
- **Authorization testing**
- **Input validation testing**

## 🔧 Security Configuration

### Environment Variables

```bash
# Production security settings
FIREBASE_PROJECT_ID=your-project-id
JWT_SECRET=your-super-secret-key
REDIS_PASSWORD=your-redis-password
DB_ENCRYPTION_KEY=your-encryption-key
```

### Firebase Setup

```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "key-id",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...",
  "client_email": "firebase-adminsdk@your-project.iam.gserviceaccount.com"
}
```

## 🚀 Security Best Practices

### Development

- **Never commit secrets** to version control
- **Use environment variables** for configurations
- **Regular dependency updates**
- **Code review** for security issues

### Production

- **Secure secret management** (Kubernetes secrets, HashiCorp Vault)
- **Regular security updates**
- **Network isolation**
- **Backup encryption**

### Monitoring

- **Real-time security monitoring**
- **Automated incident response**
- **Regular security audits**
- **Vulnerability assessments**

## 🆘 Security Incident Response

### Incident Types

- **Data breach**
- **Unauthorized access**
- **Service compromise**
- **DDoS attacks**

### Response Steps

1. **Immediate containment**
2. **Impact assessment**
3. **Evidence collection**
4. **System recovery**
5. **Post-incident review**

## 📞 Security Contacts

- **Security Team**: security@craftpilot.com
- **Incident Response**: incident@craftpilot.com
- **Bug Bounty**: https://craftpilot.com/security
