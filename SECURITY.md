# 🔐 SECURITY NOTICE - CraftPilot AI

> **⚠️ IMPORTANT: This repository contains template configurations with placeholder passwords that MUST be changed before production deployment.**

## 🚨 Security Actions Required Before Going Public

### 1. **Environment Variables Setup**

Before deploying this application, you MUST:

- Copy `.env.example` to `.env`
- Replace ALL placeholder passwords with strong, unique values
- Set up proper secret management in your deployment environment

### 2. **Required Password Changes**

The following default passwords are currently set to `CHANGE_ME_IN_PRODUCTION` and MUST be changed:

#### 🔴 Critical Passwords to Change:

- **REDIS_PASSWORD**: Redis database password
- **EUREKA_PASSWORD**: Service discovery authentication
- **OPENROUTER_API_KEY**: AI service API key
- **FIREBASE_PROJECT_ID**: Firebase project configuration

#### 📝 Configuration Files Containing Placeholders:

```
- user-service/src/main/resources/application.yml
- llm-service/src/main/resources/application.yml
- subscription-service/src/main/resources/application.yml
- notification-service/src/main/resources/application.yml
- image-service/src/main/resources/application.yml
- api-gateway/src/main/resources/application.yml
- eureka-server/src/main/resources/application.yml
- credit-service/src/main/resources/application.yml
- lighthouse-service/src/main/resources/application.yml
```

### 3. **GitHub Actions Secrets**

For CI/CD workflows, configure these secrets in your GitHub repository:

```
REDIS_PASSWORD=your_strong_redis_password
EUREKA_PASSWORD=your_strong_eureka_password
OPENROUTER_API_KEY=your_openrouter_api_key
FIREBASE_PROJECT_ID=your_firebase_project_id
GOOGLE_APPLICATION_CREDENTIALS=your_firebase_service_account_json
```

### 4. **Firebase Security**

#### 🔥 Firebase Service Account:

- **NEVER** commit `firebase-service-account.json` to version control
- Store Firebase credentials as Kubernetes secrets or environment variables
- Use least privilege principle for service account permissions

#### 📁 Credential File Locations:

```bash
# These paths should contain your actual Firebase service account JSON:
/etc/gcp/credentials/gcp-credentials.json
/app/credentials/gcp-credentials.json
/secrets/firebase-credentials.json
```

### 5. **CI/CD Pipeline Security**

#### ⚠️ GitHub Actions Workflows:

The following workflow files contain hardcoded passwords that need GitHub Secrets:

```
.github/workflows/activity-log-service-ci-cd.yml
.github/workflows/api-gateway-ci-cd.yml
.github/workflows/credit-service-ci-cd.yml
.github/workflows/eureka-server-ci-cd.yml
.github/workflows/image-service-ci-cd.yml
.github/workflows/analytics-service-ci-cd.yml
.github/workflows/admin-service-ci-cd.yml
.github/workflows/infrastructure-services-ci-cd.yml
```

#### 🛠️ Required GitHub Secrets:

```yaml
# Set these in GitHub Repository Settings > Secrets and Variables > Actions
REDIS_PASSWORD: "your_secure_redis_password"
EUREKA_PASSWORD: "your_secure_eureka_password"
OPENROUTER_API_KEY: "your_openrouter_api_key"
FIREBASE_PROJECT_ID: "your_firebase_project_id"
GCP_SA_KEY: "your_base64_encoded_service_account_json"
```

### 6. **Production Deployment Checklist**

#### ✅ Before Production:

- [ ] All default passwords changed
- [ ] Firebase service account configured
- [ ] GitHub secrets configured
- [ ] SSL/TLS certificates configured
- [ ] Network security rules applied
- [ ] Monitoring and logging enabled
- [ ] Backup strategies implemented
- [ ] Security scanning completed

#### 🔐 Security Best Practices:

- Use strong, unique passwords (minimum 16 characters)
- Enable Redis AUTH and use encrypted connections
- Configure Firebase security rules
- Use HTTPS/TLS for all external communications
- Implement proper network segmentation
- Regular security audits and dependency updates
- Monitor for unauthorized access attempts

### 7. **Security Vulnerabilities Addressed**

#### ✅ Fixed Security Issues:

- **Hardcoded Passwords**: Replaced with environment variables
- **Firebase Credentials**: Added to .gitignore and security guidelines
- **API Keys**: Moved to environment variable configuration
- **Service Credentials**: Secured with proper secret management

#### 📋 Security Measures Added:

- Comprehensive .gitignore for sensitive files
- Environment template (.env.example)
- Security documentation and guidelines
- CI/CD secret management instructions

### 8. **Contact for Security Issues**

If you discover security vulnerabilities, please:

- **DO NOT** create public GitHub issues
- Send email to: security@craftpilot.com
- Include detailed description and reproduction steps
- We will respond within 24 hours

---

## 🛡️ Disclaimer

This is a development/demo repository. The maintainers are not responsible for security issues arising from:

- Using default/placeholder passwords in production
- Improper secret management
- Misconfigured deployment environments
- Failure to follow security guidelines

**Always perform security audits before production deployment.**

---

_Last Updated: August 21, 2025_
_Security Review Required: Every 90 days_
