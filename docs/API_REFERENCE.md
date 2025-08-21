# 🔌 CraftPilot AI - API Reference

## 🚪 API Gateway

**Base URL**: `http://localhost:8080`  
**Authentication**: Firebase JWT Token required

```bash
Authorization: Bearer <firebase-jwt-token>
```

## 👤 User Service APIs

### User Profile

```bash
# Get user profile
GET /api/users/profile
Authorization: Bearer <token>

# Update user profile
PUT /api/users/profile
Content-Type: application/json
{
  "displayName": "John Doe",
  "email": "john@example.com",
  "preferences": {
    "language": "en",
    "theme": "dark"
  }
}
```

### User Preferences

```bash
# Get preferences
GET /api/users/preferences

# Update preferences
PUT /api/users/preferences
{
  "defaultModel": "gpt-4",
  "maxTokens": 2000,
  "temperature": 0.7
}
```

## 🤖 LLM Service APIs

### Conversations

```bash
# Create new conversation
POST /api/llm/conversations
{
  "title": "My Conversation",
  "model": "gpt-4"
}

# Get conversations
GET /api/llm/conversations?page=0&size=10

# Get conversation by ID
GET /api/llm/conversations/{conversationId}
```

### Messages

```bash
# Send message
POST /api/llm/conversations/{conversationId}/messages
{
  "content": "Hello, how are you?",
  "role": "user"
}

# Stream response
GET /api/llm/conversations/{conversationId}/stream
Accept: text/event-stream
```

### Models

```bash
# Get available models
GET /api/llm/models

# Get model configuration
GET /api/llm/models/{modelId}/config
```

## 🖼️ Image Service APIs

### Image Generation

```bash
# Generate image
POST /api/images/generate
{
  "prompt": "A beautiful sunset over mountains",
  "size": "1024x1024",
  "quality": "hd",
  "style": "vivid"
}

# Get generation status
GET /api/images/generations/{generationId}
```

### Image Management

```bash
# Get user images
GET /api/images?page=0&size=20

# Delete image
DELETE /api/images/{imageId}
```

## 💳 Subscription Service APIs

### Plans

```bash
# Get available plans
GET /api/subscriptions/plans

# Get current subscription
GET /api/subscriptions/current

# Subscribe to plan
POST /api/subscriptions/subscribe
{
  "planId": "premium-monthly",
  "paymentMethodId": "pm_123456"
}
```

### Billing

```bash
# Get billing history
GET /api/subscriptions/billing/history

# Get current usage
GET /api/subscriptions/usage
```

## 💰 Credit Service APIs

### Credit Management

```bash
# Get credit balance
GET /api/credits/balance

# Get credit history
GET /api/credits/history?page=0&size=50

# Purchase credits
POST /api/credits/purchase
{
  "amount": 1000,
  "paymentMethodId": "pm_123456"
}
```

## 🔔 Notification Service APIs

### Notifications

```bash
# Get notifications
GET /api/notifications?unread=true

# Mark as read
PUT /api/notifications/{notificationId}/read

# Get notification preferences
GET /api/notifications/preferences
```

## 📊 Analytics Service APIs

### Usage Analytics

```bash
# Get usage statistics
GET /api/analytics/usage?period=30d

# Get model usage
GET /api/analytics/models/usage

# Get credit usage
GET /api/analytics/credits/usage
```

## 🛡️ Admin Service APIs

### User Management (Admin Only)

```bash
# Get all users
GET /api/admin/users?page=0&size=20
Authorization: Bearer <admin-token>

# Get user details
GET /api/admin/users/{userId}

# Update user status
PUT /api/admin/users/{userId}/status
{
  "status": "active|suspended|banned"
}
```

### System Monitoring

```bash
# Get system metrics
GET /api/admin/metrics

# Get service health
GET /api/admin/health

# Get audit logs
GET /api/admin/audit-logs?page=0&size=100
```

## 🧠 User Memory Service APIs

### Memory Management

```bash
# Store user context
POST /api/memory/context
{
  "context": "User prefers technical explanations",
  "category": "preference"
}

# Get user context
GET /api/memory/context

# Store conversation memory
POST /api/memory/conversations/{conversationId}/entries
{
  "content": "User mentioned working on React project",
  "importance": "high"
}
```

## 📝 Activity Log Service APIs

### Activity Logs

```bash
# Get user activities
GET /api/activities?page=0&size=50

# Log custom activity
POST /api/activities
{
  "action": "feature_used",
  "details": {
    "feature": "image_generation",
    "prompt_length": 25
  }
}
```

## 🚨 Error Responses

### Standard Error Format

```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "The request is invalid",
    "details": "Field 'email' is required",
    "timestamp": "2025-08-21T10:30:00Z",
    "path": "/api/users/profile"
  }
}
```

### Common Error Codes

- `UNAUTHORIZED` (401): Invalid or missing token
- `FORBIDDEN` (403): Insufficient permissions
- `NOT_FOUND` (404): Resource not found
- `RATE_LIMITED` (429): Too many requests
- `INSUFFICIENT_CREDITS` (402): Not enough credits
- `VALIDATION_ERROR` (400): Invalid input data

## 📊 Rate Limits

| Endpoint Category | Rate Limit    | Window   |
| ----------------- | ------------- | -------- |
| Authentication    | 10 req/min    | Per IP   |
| LLM Requests      | 60 req/min    | Per user |
| Image Generation  | 20 req/min    | Per user |
| API Calls         | 1000 req/hour | Per user |

## 🔍 Webhooks

### Subscription Events

```bash
POST /webhooks/subscription
{
  "type": "subscription.updated",
  "data": {
    "userId": "user123",
    "subscription": { ... }
  }
}
```

### Payment Events

```bash
POST /webhooks/payment
{
  "type": "payment.succeeded",
  "data": {
    "userId": "user123",
    "amount": 1000,
    "credits": 500
  }
}
```
