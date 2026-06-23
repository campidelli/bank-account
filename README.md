# Bank Account API

A Spring Boot REST API for managing savings bank accounts.

## Requirements

- Java 21+
- Maven 3.9+
- Docker & Docker Compose (for Postgres and Redis)

## Running Locally

### 1. Start infrastructure

```bash
docker-compose up -d
```

This starts:
- PostgreSQL on `localhost:5432` (database: `bankaccount`, user: `bankuser`, password: `bankpassword`)
- Redis on `localhost:6379`

### 2. Build and run

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

---

## API Endpoints

### Create Account

```
POST /api/v1/accounts
Content-Type: application/json

{
  "customerName": "John Smith",
  "accountNickName": "MySavings"
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "accountNumber": "ACC-20240623-61983282",
  "customerName": "John Smith",
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
  "accountNumber": "ACC-20240623-61983282",
  "customerName": "John Smith",
  "accountNickName": "MySavings",
  "createdAt": "2024-06-23T10:00:00"
}
```

---

## Validation Rules

| Field | Rule |
|-------|------|
| `customerName` | Mandatory, max 100 characters |
| `accountNickName` | Optional, 5–30 characters if provided |
| `accountNickName` | Must not contain offensive language |
| Per customer | Maximum 5 accounts per customer name |

---

## Error Responses

All errors follow a consistent shape:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation failed",
  "timestamp": "2024-06-23T10:00:00",
  "fieldErrors": [
    { "field": "customerName", "message": "Customer name is required" }
  ]
}
```

| HTTP Status | Scenario |
|-------------|----------|
| `400` | Request validation failure |
| `404` | Account not found |
| `422` | Customer has reached the 5-account limit |
| `503` | Database unavailable |

---

## Architecture

```
controller/       HTTP layer — request/response mapping, input validation
service/          Business logic (interface + impl)
repository/       Spring Data JPA repository
entity/           JPA entity (Account)
dto/              Request/response DTOs
validation/       Custom @NotOffensiveNickName constraint + OffensiveWordsProvider
exception/        Domain exceptions + GlobalExceptionHandler
config/           Redis cache configuration
```

### Caching

GET calls are cached in Redis with a 10-minute TTL using `@Cacheable`. The cache key is the account number.

### Database resilience

`DataAccessException` from the repository layer is caught and rethrown as `DatabaseUnavailableException`, which maps to `503 Service Unavailable`. This ensures the client receives a clear, retryable error rather than a generic 500.

---

## Running Tests

```bash
mvn test
```

Tests use an in-memory H2 database and simple cache (no Redis required).

Test coverage includes:
- `AccountControllerTest` — MockMvc tests for all endpoints and error cases
- `AccountServiceImplTest` — unit tests for business logic with mocked repository
- `AccountRepositoryTest` — `@DataJpaTest` against H2
- `CreateAccountRequestValidationTest` — constraint validation tests including offensive nick name detection

---

## Health Check

```
GET /actuator/health
```

---

## Design Decisions & TODOs

- **Account number generation**: Currently uses `ACC-YYYYMMDD-RANDOMINT`. In production, use a database sequence or distributed ID generator (e.g. Snowflake) to guarantee uniqueness under concurrent load.
- **Offensive words list**: Hardcoded in `OffensiveWordsProvider`. In production, load from a database table or integrate an external content moderation API (AWS Comprehend, Google Perspective) to allow runtime updates without redeployment.
- **Customer identity**: The 5-account limit is enforced by `customerName` string matching. In a real system this would use a customer ID from an identity/auth system.
- **Cache invalidation**: No cache eviction on writes; with a 10-minute TTL this is acceptable for a read-heavy API. Add `@CacheEvict` on mutation endpoints if needed.
