# bank-account-service

Skeleton Spring Boot service for savings bank accounts, backed by PostgreSQL for storage and
Redis for caching.

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
[.locl-env/README.md](.locl-env/README.md).

## Configuration

`src/main/resources/application.yml` contains no real values. Every connection setting is a
placeholder, supplied by environment variables or by a local profile.

| Variable         | Required | Purpose                         |
|------------------|----------|---------------------------------|
| `DB_HOST`        | yes      | PostgreSQL host                 |
| `DB_PORT`        | yes      | PostgreSQL port (usually 5432)  |
| `DB_NAME`        | yes      | Database name                   |
| `DB_USERNAME`    | yes      | Database user                   |
| `DB_PASSWORD`    | yes      | Database password               |
| `DB_POOL_SIZE`   | no       | Hikari pool size, default `10`  |
| `REDIS_HOST`     | yes      | Redis host                      |
| `REDIS_PORT`     | yes      | Redis port (usually 6379)       |
| `REDIS_PASSWORD` | no       | Redis password, default empty   |
| `CACHE_TTL`      | no       | Cache TTL, default `10m`        |
| `SERVER_PORT`    | no       | HTTP port, default `8080`       |

For local development create `application-local.yaml` in `src/main/resources` (git-ignored) with
concrete values, then run with the `local` profile:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bank_accounts
    username: postgres
    password: postgres
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

## Project layout

```
src/main/java/nz/co/westpac/poc/Application.java
src/main/resources/application.yml
```
