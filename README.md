# Project Relay

Spring Boot service for detecting multi-account transfer layering chains.

## Stack

- Java 17, Spring Boot 3.5, Maven
- PostgreSQL 16 and Flyway
- H2 for fast local tests

## Run locally

```bash
docker compose -f compose.yml up -d
mvn spring-boot:run
```

## Run tests

```bash
mvn test
```

The default test profile uses an in-memory H2 database in PostgreSQL compatibility mode and does not require Docker.

## Configuration

Detection settings are under `layering.*` in `src/main/resources/application.yml`. Database credentials can be overridden with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

## Architecture

The initial project skeleton establishes the Spring application, configuration, PostgreSQL schema and test setup. Transfer ingestion, chain detection, alert APIs, streaming and UI are implemented in subsequent increments.

## Next steps

1. Implement validated, idempotent transfer ingestion.
2. Implement bounded backwards chain search and deterministic tie-breaking.
3. Persist and expose masked alerts.
4. Add seeded synthetic data and rule-boundary tests.
