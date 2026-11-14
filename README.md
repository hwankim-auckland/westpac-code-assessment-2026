# bank-account-service

API for savings bank accounts:

- creates a new savings bank account
- gets the savings bank account
- gets all savings bank accounts of the signed-in customer

PostgreSQL stores the accounts, Redis caches account reads and holds the not-allowed nickname list.
Every endpoint is JWT protected; the customer id is taken from the token subject.

## Stack

| Component   | Version                                               |
|-------------|-------------------------------------------------------|
| Java        | 25                                                    |
| Spring Boot | 4.1.2 (Spring Framework 7, Jackson 3, Jakarta EE 11)  |
| Build       | Maven 3.9+                                            |
| PostgreSQL  | 18.x                                                  |
| Redis       | 8.x                                                   |

## Prerequisites

- JDK 25 (`java -version` must report 25)
- Maven 3.9+
- PostgreSQL 18 and Redis 8 running locally or reachable over the network

## Local environment

PostgreSQL 18 and Redis 8 for local development run via Docker Compose — see
[.locl-env/README.md](.locl-env/README.md). The stack seeds the not-allowed nickname list from
`.locl-env/init/not_allowed_nick_name_lists.csv`.

## Configuration

`src/main/resources/application.yml` contains no real values. Every setting is a placeholder,
supplied by environment variables or by the `local` profile.

| Variable         | Required | Purpose                                  |
|------------------|----------|------------------------------------------|
| `DB_HOST`        | yes      | PostgreSQL host                          |
| `DB_PORT`        | yes      | PostgreSQL port (usually 5432)           |
| `DB_NAME`        | yes      | Database name                            |
| `DB_USERNAME`    | yes      | Database user                            |
| `DB_PASSWORD`    | yes      | Database password                        |
| `DB_POOL_SIZE`   | no       | Hikari pool size, default `10`           |
| `REDIS_HOST`     | yes      | Redis host                               |
| `REDIS_PORT`     | yes      | Redis port (usually 6379)                |
| `REDIS_PASSWORD` | no       | Redis password, default empty            |
| `CACHE_TTL`      | no       | Savings account cache TTL, default `10m` |
| `JWT_PUBLIC_KEY` | no       | RSA public key (PEM or base64 DER)       |
| `JWT_PRIVATE_KEY`| no       | RSA private key (PEM or base64 DER)      |
| `SERVER_PORT`    | no       | HTTP port, default `8080`                |

When no JWT keys are configured, an RSA key pair is generated at application startup. Tokens then
stop being valid after a restart; set both key variables to keep them stable.

`application-local.yml` (git-ignored) holds the local connection values:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/accounts_db
    username: local_admin
    password: some_strong_password
  data:
    redis:
      host: localhost
      port: 6379
```

## Run

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
# or
mvn clean package
java -jar target/bank-account-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Health check: `GET http://localhost:8080/actuator/health`.

## Authentication

`/api/v1/accounts/**` requires `Authorization: Bearer <JWT>`; the token subject is the customer id.

With the `local` profile a sample token can be minted for local testing:

```bash
curl -X POST http://localhost:8080/api/v1/local/token \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"6d29ceee-e865-49b7-b34f-96f801460f2e"}'
```

That endpoint exists only with the `local` profile. `.locl-env/local-api.http` runs the whole flow
(token, create, bad requests, get, list) from an IDE.

## API

| Method | Path                          | Description                                        |
|--------|-------------------------------|----------------------------------------------------|
| POST   | `/api/v1/accounts`            | Creates a new savings bank account                 |
| GET    | `/api/v1/accounts/{accountId}`| Gets the savings bank account                      |
| GET    | `/api/v1/accounts`            | Gets all savings bank accounts of the token subject|

### Create a savings bank account

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"customerName":"Jane Tan","accountNickname":"holiday"}'
```

`201 Created` with a `Location` header:

```json
{
  "accountId": "687df11e9559b1f24f2f125e79b60df2db96668a6cf1631bb36ff6b81ccc7ed5",
  "accountNumber": "03 0187 8399940 000",
  "customerId": "6d29ceee-e865-49b7-b34f-96f801460f2e",
  "customerName": "Jane Tan",
  "accountNickname": "holiday"
}
```

### Get the savings bank account

```bash
curl http://localhost:8080/api/v1/accounts/{accountId} -H "Authorization: Bearer $TOKEN"
```

Returns the savings bank account, served from Redis when cached, otherwise from PostgreSQL.

### Get all savings bank accounts of the customer

```bash
curl http://localhost:8080/api/v1/accounts -H "Authorization: Bearer $TOKEN"
```

Returns the savings bank accounts owned by the token subject, sorted by account number.

## Rules and error responses

- `customerName` is required (max 120 characters); `accountNickname` is optional
- `accountNickname` must be 5–30 characters and must not be in the Redis set
  `not_allowed_nick_name_lists`
- a customer cannot hold more than 5 savings bank accounts

| Status | When                                                              |
|--------|-------------------------------------------------------------------|
| `400`  | validation failure, nickname too short/long, nickname not allowed |
| `401`  | missing, invalid or expired JWT                                   |
| `404`  | savings bank account not found                                    |
| `409`  | customer already holds 5 savings bank accounts                    |
| `503`  | PostgreSQL is unavailable                                         |

Errors are returned as RFC 9457 `ProblemDetail`.

## Project layout

```
src/main/java/nz/co/westpac/poc
├── Application.java
├── api/          AccountController, mapper, request/response DTOs
├── entity/       Account, AccountRepository, generator/, validator/
├── error/        exceptions and ProblemDetail advice
├── local/        local-profile token endpoint
├── security/     JWT key provider and resource-server config
└── service/      AccountService, AccountCache
src/main/resources
├── application.yml
└── db/migration/V1__create_accounts.sql
```

## Tests

```bash
mvn test
```

End-to-end tests extend `support/TestContainerBase`, which starts PostgreSQL 18.7 and Redis 8.10.3
once with Testcontainers and shares the HTTP client, JWT helper and autowired beans, so Docker must
be running:

| Class                      | Covers                                                            |
|----------------------------|-------------------------------------------------------------------|
| `ApplicationTest`          | health endpoint                                                   |
| `AccountCreateE2ETest`     | creating a savings bank account, optional nickname, 5-account limit|
| `AccountGetE2ETest`        | get by id, Redis caching, list by token subject, 404 and 401      |
| `AccountBadRequestE2ETest` | nickname length, blocklist, malformed JSON, non-customer subject  |
| `RequestIdE2ETest`         | request-id response header                                        |

Unit tests cover the generators, validator, mapper, service, cache, exception handler, JWT keys,
both request filters and the local token controller.
