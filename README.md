# bank-account-service

[![CI](https://github.com/hwankim-auckland/westpac-code-assessment-2026/actions/workflows/ci.yml/badge.svg)](https://github.com/hwankim-auckland/westpac-code-assessment-2026/actions/workflows/ci.yml)

REST API for savings bank accounts. It creates a savings bank account, returns a single savings bank
account and lists every savings bank account of the authenticated customer. Accounts live in
PostgreSQL, reads are cached in Redis, and every endpoint is protected by a JWT bearer token.

## Contents

- [Overview](#overview)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [Authentication](#authentication)
- [API reference](#api-reference)
- [Error responses](#error-responses)
- [Testing](#testing)
- [Docker](#docker)
- [Continuous integration](#continuous-integration)
- [Design notes](#design-notes)

## Overview

- Creates a new savings bank account for the customer in the token subject
- Gets the savings bank account by id (served from Redis when cached)
- Gets all savings bank accounts of the customer, sorted by account number
- Validates the account nickname (5–30 characters, checked against a Redis blocklist)
- Limits a customer to five savings bank accounts
- Issues an `X-Request-Id` for every request and logs each request with `customerId` + `requestId`

## Tech stack

| Component       | Version                                              |
|-----------------|------------------------------------------------------|
| Java            | 25                                                   |
| Spring Boot     | 4.1.2 (Spring Framework 7, Jackson 3, Jakarta EE 11) |
| Build           | Maven 3.9+                                           |
| Persistence     | Spring Data JPA + Hibernate, PostgreSQL 18.x         |
| Cache           | Spring Data Redis, Redis 8.x                         |
| Migrations      | Flyway                                               |
| Security        | Spring Security OAuth2 Resource Server (JWT, RSA)    |
| Testing         | JUnit 5, AssertJ, Mockito, Testcontainers            |
| Container image | `eclipse-temurin:25-jre`                             |

## Project structure

```
.
├── .github/workflows/ci.yml        build, test, docker image
├── .locl-env/                      local PostgreSQL + Redis stack
│   ├── docker-compose.yml
│   └── init/not_allowed_nick_name_lists.csv
├── Dockerfile
├── pom.xml
└── src
    ├── main
    │   ├── java/nz/co/westpac/poc
    │   │   ├── api/                controller, mapper, request/response DTOs
    │   │   ├── entity/             Account, repository, generators, validator
    │   │   ├── error/              exceptions and ProblemDetail advice
    │   │   ├── local/              local-profile token endpoint
    │   │   ├── logging/            request id and request logging filters
    │   │   ├── security/           JWT key provider and resource server config
    │   │   └── service/            AccountService, AccountCache, CustomerService
    │   └── resources
    │       ├── application.yml
    │       └── db/migration/V1__create_accounts.sql
    └── test/java/nz/co/westpac/poc
        ├── api/                    end-to-end and mapper tests
        ├── entity/                 generator and validator tests
        ├── logging/                filter tests
        ├── service/                service and cache tests
        └── support/                TestContainerBase
```

## Getting started

### Prerequisites

- JDK 25 (`java -version` must report 25)
- Maven 3.9+
- Docker (local stack and Testcontainers)

### Local infrastructure

PostgreSQL 18.7 and Redis 8.10.3 run via Docker Compose, together with a one-shot job that loads the
nickname blocklist:

```bash
docker compose -f .locl-env/docker-compose.yml up -d
```

| Service    | Container | Port | Credentials                                            |
|------------|-----------|------|--------------------------------------------------------|
| PostgreSQL | `db`      | 5432 | `accounts_db` / `local_admin` / `some_strong_password` |
| Redis      | `cache`   | 6379 | no password                                            |

The blocklist lives in `.locl-env/init/not_allowed_nick_name_lists.csv` and is loaded into the Redis
set `not_allowed_nick_name_lists`. Re-run `up -d --force-recreate cache-seed` after editing it.

### Run the application

Create `src/main/resources/application-local.yml` (git-ignored) with the local connection values, then
start with the `local` profile:

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

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The API listens on `http://localhost:8080`, health is at `/actuator/health`.

## Configuration

Every value in `application.yml` is a placeholder resolved from the environment.

| Variable          | Required | Default | Purpose                              |
|-------------------|----------|---------|--------------------------------------|
| `DB_HOST`         | yes      | –       | PostgreSQL host                      |
| `DB_PORT`         | yes      | –       | PostgreSQL port                      |
| `DB_NAME`         | yes      | –       | Database name                        |
| `DB_USERNAME`     | yes      | –       | Database user                        |
| `DB_PASSWORD`     | yes      | –       | Database password                    |
| `DB_POOL_SIZE`    | no       | `10`    | Hikari maximum pool size             |
| `REDIS_HOST`      | yes      | –       | Redis host                           |
| `REDIS_PORT`      | yes      | –       | Redis port                           |
| `REDIS_PASSWORD`  | no       | empty   | Redis password                       |
| `CACHE_TTL`       | no       | `10m`   | TTL of a cached savings bank account |
| `JWT_PUBLIC_KEY`  | no       | empty   | RSA public key, PEM or base64 DER    |
| `JWT_PRIVATE_KEY` | no       | empty   | RSA private key, PEM or base64 DER   |
| `SERVER_PORT`     | no       | `8080`  | HTTP port                            |

When neither JWT key is configured, an RSA-2048 key pair is generated at startup. Tokens then stop
being valid after a restart, so set both variables to keep them stable. Configuring only one of the two
fails fast.

## Authentication

`/api/v1/accounts/**` requires `Authorization: Bearer <JWT>`. The customer id is taken from the token
subject and must be a UUID; anything else is rejected with `400`.

With the `local` profile a sample token can be minted for testing:

```bash
curl -s -X POST http://localhost:8080/api/v1/local/token \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"6d29ceee-e865-49b7-b34f-96f801460f2e"}'
```

```json
{"token":"eyJhbGciOiJSUzI1NiJ9..."}
```

That endpoint exists only with the `local` profile. `/actuator/health` and `/actuator/info` are open.

## API reference

| Method | Path                           | Description                                         |
|--------|--------------------------------|-----------------------------------------------------|
| POST   | `/api/v1/accounts`             | Creates a new savings bank account                  |
| GET    | `/api/v1/accounts`             | Gets all savings bank accounts of the token subject |
| GET    | `/api/v1/accounts/{accountId}` | Gets the savings bank account                       |

### Create a savings bank account

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountNickname":"holiday"}'
```

`201 Created`, `Location: /api/v1/accounts/{accountId}`

```json
{
  "accountId": "687df11e9559b1f24f2f125e79b60df2db96668a6cf1631bb36ff6b81ccc7ed5",
  "accountNumber": "03 0187 8399940 000",
  "customerName": "Hwan Kim",
  "accountNickname": "holiday"
}
```

### Get a savings bank account

```bash
curl http://localhost:8080/api/v1/accounts/{accountId} -H "Authorization: Bearer $TOKEN"
```

### Get all savings bank accounts of the customer

```bash
curl http://localhost:8080/api/v1/accounts -H "Authorization: Bearer $TOKEN"
```

```json
[
  {
    "accountId": "687df11e9559b1f24f2f125e79b60df2db96668a6cf1631bb36ff6b81ccc7ed5",
    "accountNumber": "03 0187 8399940 000",
    "customerName": "Hwan Kim",
    "accountNickname": "holiday"
  }
]
```

### Rules

| Rule                  | Detail                                                                                 |
|-----------------------|----------------------------------------------------------------------------------------|
| `accountNickname`     | optional, 5–30 characters                                                              |
| Offensive nicknames   | rejected when present in the Redis set `not_allowed_nick_name_lists`, case-insensitive |
| Accounts per customer | maximum 5                                                                              |
| `accountNumber`       | `03 0187 {7 digits} {per-customer suffix from 000}`                                    |
| `customerName`        | resolved from the customer record, not from the request                                |

### Request id

Every response carries `X-Request-Id`: the caller's value when supplied, otherwise a generated UUID.
The same id is written to the log together with the customer id.

## Error responses

Errors are returned as RFC 9457 `ProblemDetail`:

```json
{
  "type": "about:blank",
  "title": "Invalid account",
  "status": 400,
  "detail": "accountNickname must be between 5 and 30 characters",
  "instance": "/api/v1/accounts"
}
```

| Status | When                                                                    |
|--------|-------------------------------------------------------------------------|
| `400`  | nickname length, nickname not allowed, malformed JSON, non-UUID subject |
| `401`  | missing, invalid or expired JWT                                         |
| `404`  | savings bank account not found                                          |
| `409`  | customer already holds five savings bank accounts                       |
| `500`  | unexpected database error                                               |
| `503`  | PostgreSQL unavailable                                                  |

## Testing

```bash
mvn test          # tests
mvn verify        # build, tests and the packaged jar
```

Tests that start the Spring context extend `support/TestContainerBase`, which starts PostgreSQL 18.7
and Redis 8.10.3 with Testcontainers. Docker must be running; no external services are needed.

| Class                        | Covers                                                              |
|------------------------------|---------------------------------------------------------------------|
| `ApplicationTest`            | health endpoint                                                     |
| `AccountCreateE2ETest`       | create, optional nickname, five-account limit                       |
| `AccountGetE2ETest`          | get by id, Redis caching, list by token subject, 404, 401           |
| `AccountBadRequestE2ETest`   | nickname length, blocklist, malformed JSON, non-customer subject    |
| `RequestIdE2ETest`           | request-id response header                                          |
| `AccountMapperTest`          | entity to response mapping                                          |
| `AccountServiceTest`         | create, limit, missing customer, cached and uncached reads, sorting |
| `AccountCacheTest`           | cache hit, miss, unreadable payload, Redis failure, TTL             |
| `AccountValidatorTest`       | nickname length and blocklist lookups                               |
| `AccountIdGeneratorTest`     | 64-character hex ids                                                |
| `AccountNumberGeneratorTest` | account number format and per-customer suffix                       |
| `RequestIdFilterTest`        | header reuse, generation, blank/oversized values, MDC cleanup       |
| `RequestLogFilterTest`       | customer id in the logging context                                  |

## Docker

The image packages the jar produced by the build, so package first:

```bash
mvn -DskipTests package
docker build -t bank-account-service:local .
docker run --rm -p 8080:8080 \
  -e DB_HOST=localhost -e DB_PORT=5432 -e DB_NAME=accounts_db \
  -e DB_USERNAME=local_admin -e DB_PASSWORD=some_strong_password \
  -e REDIS_HOST=localhost -e REDIS_PORT=6379 \
  bank-account-service:local
```

## Continuous integration

`.github/workflows/ci.yml` runs on every push and pull request:

1. checks out the repository and installs Temurin JDK 25 with Maven caching
2. runs `mvn -B -ntp verify` (unit tests, Testcontainers integration tests, packaged jar)
3. uploads `surefire-reports` when the build fails and the jar when it succeeds
4. builds a docker image tagged `<branch>-<short-sha>` (for example `dev-a1b2c3d`)
5. prints a message instead of pushing the image — publishing to a registry is out of scope

Testcontainers uses the runner's own Docker daemon, so no Docker-in-Docker setup is required.

## Design notes

- `CustomerService` is a mock: customer data is out of scope, so every customer id resolves to the
  same sample customer name. Real deployments would call a customer service.
- `AccountNumberGenerator` is a mock of the real bank numbering rules: fixed `03 0187` branch prefix,
  seven random digits and a per-customer suffix taken from the number of existing accounts.
- Reads of a single account use cache-aside with a configurable TTL; a Redis failure is treated as a
  cache miss so the request still succeeds from PostgreSQL.
- Requests are logged, not stored: each entry is written to the application log with
  `[customerId=…, requestId=…]`, and both values are also placed in the MDC.
