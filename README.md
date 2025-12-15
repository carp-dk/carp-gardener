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
- Docker + Docker Compose if you want the bundled PostgreSQL/RabbitMQ stack.
- OAuth credentials for the vendors you plan to enable (fill them into `gardener-caws-implementation/.env`).

## Build and test
Run the Gradle wrapper from the repository root:
```
./gradlew build                # builds everything and runs tests
./gradlew :core:test           # framework-only tests
./gradlew :caws-implementation:test
```

## Run the CARP implementation
- **With Docker Compose (full stack)**  
  1) Copy the sample env file: `cp gardener-caws-implementation/_.local.env gardener-caws-implementation/.env` and add your secrets.  
  2) Start the stack:  
  `docker compose -f gardener-caws-implementation/docker/docker-compose.yml --env-file gardener-caws-implementation/.env up --build`  
  This brings up PostgreSQL, RabbitMQ, and the service on port `8444` (RabbitMQ UI on `15673`).

- **Locally with Gradle (reuse external Postgres/RabbitMQ)**  
  Ensure the dependencies are reachable (the Compose stack above works). Keep the `.env` file in `gardener-caws-implementation/` for overrides. Then run:  
  `cd gardener-caws-implementation && profile=local ../gradlew :caws-implementation:run`  
  Configuration is loaded from `conf/config-<profile>.json` with environment variables (or `.env`) overriding the defaults.

## Documentation
- docs/core_readme.md – core framework architecture and extension hooks.
- docs/impl_readme.md – deployment architecture and RabbitMQ consumer helper.
- docs/ktor-routing.md – HTTP endpoints exposed by the CARP implementation.
- docs/postgres-schema.sql – database schema used by the implementation.

## License
MIT License. See LICENSE for details.
