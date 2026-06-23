# Bank Account API

A Spring Boot REST API for managing savings bank accounts with PostgreSQL, Redis caching, and circuit breaker resilience.

## Features

- **Account Management**: Create and retrieve savings accounts
- **Account Number Generation**: NZ-style account numbers (BB-bbbb-AAAAAAA-SS format)
- **Account Nicknames**: Optional custom nicknames with profanity filtering
- **Caching**: Redis-based caching with 10-minute TTL for GET requests
- **Circuit Breaker**: Resilience4j circuit breaker for database resilience with cache fallback
- **Validation**: Jakarta Bean Validation with custom profanity filter
- **OpenAPI/Swagger**: Interactive API documentation
- **Health Checks**: Spring Boot Actuator endpoints

## Requirements

- Java 25
- Maven 3.9+
- Docker & Docker Compose (for PostgreSQL and Redis)

## Running Locally

### 1. Start infrastructure

```bash
docker-compose up -d
```

This starts:
- PostgreSQL 17 on `localhost:5432`
- Redis 7 on `localhost:6379`

### 2. Configure environment (optional)

Copy `.env.example` to `.env` and customize:

```bash
cp .env.example .env
```

### 3. Build and run

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

## API Documentation

Swagger UI is available at: `http://localhost:8080/swagger-ui.html`

OpenAPI spec at: `http://localhost:8080/v3/api-docs`

## API Endpoints

### Create Account

```
POST /api/v1/accounts
Content-Type: application/json

{
  "bankCode": "03",
  "branchCode": "0473",
  "customerName": "John Smith",
  "accountNickName": "MySavings"
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "accountNumber": "03-0473-1234567-00",
  "bankCode": "03",
  "branchCode": "0473",
  "accountType": "SAVINGS",
  "customerName": "JOHN SMITH",
  "accountNickName": "MySavings",
  "createdAt": "2024-06-23T10:00:00"
}
```

### Get Account

```
GET /api/v1/accounts/{accountNumber}
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "accountNumber": "03-0473-1234567-00",
  "bankCode": "03",
  "branchCode": "0473",
  "accountType": "SAVINGS",
  "customerName": "JOHN SMITH",
  "accountNickName": "MySavings",
  "createdAt": "2024-06-23T10:00:00"
}
```

## Business Rules

### Account Number Format
- Format: `BB-bbbb-AAAAAAA-SS`
- `BB`: 2-digit bank code
- `bbbb`: 4-digit branch code
- `AAAAAAA`: 7-digit account base (randomly generated, unique per bank/branch)
- `SS`: 2-digit suffix (00 for first account, increments for subsequent accounts)

### Account Limits
- Maximum **5 accounts per customer** per bank/branch
- Maximum **99 suffixes per account base** (00-99)

### Account Nicknames
- Optional field (5-30 characters if provided)
- Must be **unique per customer/bank/branch** (case-insensitive)
- Blank strings are treated as null
- Must not contain offensive language (profanity filter)
- Normalized to null if blank

### Customer Name
- Converted to uppercase for storage and comparison

## Error Responses

All errors follow RFC 7807 Problem Details format:

```json
{
  "type": "https://api.bank.com/problems/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/accounts"
}
```

| HTTP Status | Type | Scenario |
|-------------|------|----------|
| `400` | `validation-failed` | Request validation failure |
| `400` | `duplicate-account-nickname` | Account nickname already exists for customer/bank/branch |
| `404` | `account-not-found` | Account not found |
| `422` | `account-limit-exceeded` | Customer has reached the 5-account limit |
| `503` | `database-unavailable` | Database unavailable (circuit breaker open) |

## Architecture

```
domain/
├── model/           Domain entities (Account, AccountType)
├── service/         Business logic with circuit breaker
├── port/
│   ├── in/          Use case interfaces (CreateAccountUseCase, GetAccountUseCase)
│   └── out/         Port interfaces (AccountPort, AccountCachePort)
└── exception/       Domain exceptions

adapter/
├── in/
│   ├── web/         REST controller, DTOs, validation, exception handler
│   └── web/dto/     Request/response DTOs
└── out/
    ├── persistence/ JPA repository implementation
    └── cache/       Redis cache implementation

infrastructure/
└── config/          Spring configuration (Redis, etc.)
```

### Key Design Patterns

- **Hexagonal Architecture**: Domain layer independent of infrastructure
- **Circuit Breaker**: Service layer uses Resilience4j with cache fallback
- **Caching**: Redis for GET requests with 10-minute TTL
- **Repository Pattern**: Clean separation between domain and persistence

## Dependencies

### Runtime
- Spring Boot 4.1.0
- Spring Data JPA
- PostgreSQL 17
- Redis 7
- Resilience4j 2.3.0
- SpringDoc OpenAPI 2.8.6
- Profanity Filter 1.0.1

### Test
- Spring Boot Test
- Testcontainers 1.20.4 (PostgreSQL)
- JUnit 5
- Mockito

## Running Tests

```bash
mvn test
```

### Test Infrastructure

Tests use **Testcontainers** with PostgreSQL 17 (not H2) for realistic integration testing.

Test configuration:
- PostgreSQL 17 container automatically started by Testcontainers
- Redis disabled in tests (simple cache used)
- 44 tests covering:
  - Controller layer (`AccountControllerTest`)
  - Service layer (`AccountServiceTest`)
  - Persistence layer (`AccountJpaRepositoryTest`, `AccountPersistenceAdapterTest`)
  - Validation (`CreateAccountRequestValidationTest`)

## Health Check

```
GET /actuator/health
```

Includes circuit breaker status:
```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "account-service": {
          "status": "UP",
          "details": {
            "failureRate": "0.0%",
            "slowCallRate": "0.0%",
            "state": "CLOSED"
          }
        }
      }
    }
  }
}
```

## Circuit Breaker Configuration

- **Name**: `account-service`
- **Sliding Window Type**: Count-based
- **Sliding Window Size**: 10 calls
- **Failure Rate Threshold**: 50%
- **Wait Duration in Open State**: 30s
- **Permitted Calls in Half-Open State**: 3

When the circuit breaker is open, the service falls back to Redis cache for GET requests. If cache miss, returns 503 Service Unavailable.

## Assumptions & Design Decisions

- **Account Base Generation**: Random 7-digit number (1,000,000 - 9,999,999) with uniqueness check. In production, consider a database sequence or distributed ID generator for guaranteed uniqueness under high concurrency.
- **Customer Identity**: The 5-account limit is enforced by `customerName` string matching. In a real system, this would use a customer ID from an identity/auth system.
- **Cache Invalidation**: No explicit cache eviction on writes. With 10-minute TTL, this is acceptable for read-heavy workloads. Consider adding `@CacheEvict` on mutation endpoints if stronger consistency is required.
- **Profanity Filter**: Uses a hardcoded list. In production, load from a database table or integrate an external content moderation API (AWS Comprehend, Google Perspective) for runtime updates without redeployment.
- **Account Nickname Uniqueness**: Enforced per customer/bank/branch combination, case-insensitive. Blank nicknames are normalized to null and not validated.
