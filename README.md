![GARDENER-LOGO](docs/gardener_logo.png)
# CARP Gardener

CARP Gardener collects wearable activity and health data from third-party services and optionally transforms the payloads before publishing them. The repository contains the reusable framework plus a ready-to-run CARP-oriented service.

## Modules
- `gardener-core` (`:core`): framework for OAuth-based authorization, data collection, and transformer registration. See docs/core_readme.md for architecture and extension points.
- `gardener-caws-implementation` (`:caws-implementation`): Ktor service that wires the core to PostgreSQL, RabbitMQ, and CARP payload transformers. See docs/impl_readme.md for deployment details and docs/ktor-routing.md for HTTP endpoints.

## Supported data sources
- Fitbit (OAuth2)
- Garmin (OAuth1)
- Withings (OAuth2)
- Dexcom (OAuth2)

## Prerequisites
- JDK 21 (the Gradle wrapper configures toolchains automatically).
- Docker + Docker Compose if you want to run the service in a container.
- The carp-webservices stack running: Gardener shares its PostgreSQL and RabbitMQ (containers `carp-postgres-<profile>` and `carp-rabbitmq-<profile>` on network `carp-<profile>`).
- OAuth credentials for the vendors you plan to enable (fill them into `gardener-caws-implementation/.env`).

## Build and test
Run the Gradle wrapper from the repository root:
```
./gradlew build                # builds everything and runs tests
./gradlew :core:test           # framework-only tests
./gradlew :caws-implementation:test
```

## Run the CARP implementation
- **With Docker Compose**  
  1) Create `gardener-caws-implementation/.env` (gitignored) with your secrets. Config keys map to variables by upper-casing and replacing `.` with `__`, e.g. `fitbit.client.id` → `FITBIT__CLIENT__ID`.  
  2) Start the stack:  
  `docker compose -f gardener-caws-implementation/docker/docker-compose.yml --env-file gardener-caws-implementation/.env up --build`  
  This starts the service on port `8444`. It connects to the shared PostgreSQL and RabbitMQ of the carp-webservices stack, which must already be running; `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `RABBITMQ_USER` and `RABBITMQ_PASSWORD` in `.env` select the database and credentials (defaults `gardener` and `admin` / `admin`). Gardener's tables, exchange and queue are created on first start if missing.

- **Locally with Gradle**  
  Ensure the carp-webservices stack is running (its PostgreSQL and RabbitMQ are published on `localhost`). Keep the `.env` file in `gardener-caws-implementation/` for overrides. Then run:  
  `cd gardener-caws-implementation && profile=local ../gradlew :caws-implementation:run`  
  Configuration is loaded from `conf/config-<profile>.json` with environment variables (or `.env`) overriding the defaults.

## Documentation
- docs/core_readme.md – core framework architecture and extension hooks.
- docs/impl_readme.md – deployment architecture and RabbitMQ consumer helper.
- docs/ktor-routing.md – HTTP endpoints exposed by the CARP implementation.
- gardener-caws-implementation/src/main/resources/db/postgres-schema.sql – database schema, applied automatically on startup.

## License
MIT License. See LICENSE for details.
