# Local environment

Docker Compose environment for running the service locally against PostgreSQL and Redis.

Compose project name: `account-service-local-env`.

## Services

| Service  | Container | Image           | Published port | Connection details                                                  |
|----------|-----------|-----------------|----------------|---------------------------------------------------------------------|
| postgres | `db`      | `postgres:18.7` | `5432:5432`    | database `accounts_db`, user `local_admin`, password `some_strong_password` |
| redis    | `cache`   | `redis:8.10.3`  | `6379:6379`    | no authentication                                                   |

Volumes:

- `postgres-data` mounted at `/var/lib/postgresql` — the PostgreSQL 18 image keeps `PGDATA` at
  `/var/lib/postgresql/18/docker`, so this is the directory to persist.
- `redis-data` mounted at `/data`.

Both services define healthchecks, so `docker ps` reports `(healthy)` once they are accepting
connections.

## Usage

```bash
# start
docker compose -f .locl-env/docker-compose.yml up -d

# status
docker compose -f .locl-env/docker-compose.yml ps

# follow logs
docker compose -f .locl-env/docker-compose.yml logs -f

# stop (keeps data)
docker compose -f .locl-env/docker-compose.yml down

# stop and delete data volumes
docker compose -f .locl-env/docker-compose.yml down -v
```

## Connecting

```bash
docker exec -it db psql -U local_admin -d accounts_db
docker exec -it cache redis-cli
```

`src/main/resources/application-local.yml` (git-ignored) matches these values:

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

Run the application against it with the `local` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
