#!/usr/bin/env bash
# Deploys the Gardener image that CI pushed for this server's profile.
# Fill gardener-caws-implementation/.env on the server first; the carp-webservices stack must already be running.
set -euo pipefail

cd "$(dirname "$0")"

ENV="gardener-caws-implementation/.env"
COMPOSE="gardener-caws-implementation/docker/docker-compose.yml"

error() {
  echo "Error:" "$@" 1>&2
  exit 1
}

[[ -f "${ENV}" ]] || error "${ENV} not found; create it with this server's settings."

# Read single keys rather than sourcing the file, so secrets with shell characters stay intact.
env_value() {
  sed -n "s/^$1=//p" "${ENV}" | tail -n 1 | tr -d "\"'\r"
}
PROFILE="$(env_value PROFILE)"
CONFIG_PROFILE="$(env_value CONFIG_PROFILE)"
[[ -n "${PROFILE:-}" ]] || error "Set PROFILE in ${ENV} (e.g. development or production)."
[[ -n "${CONFIG_PROFILE:-}" ]] || error "Set CONFIG_PROFILE in ${ENV} (e.g. prod)."
# Compose falls back to admin/admin for local runs; a server must use the carp-webservices credentials.
[[ -n "$(env_value ADMIN_USER)" && -n "$(env_value ADMIN_PASSWORD)" ]] ||
  error "Set ADMIN_USER and ADMIN_PASSWORD in ${ENV} to those of the carp-webservices .env."

NETWORK="carp-${PROFILE}"
docker network inspect "${NETWORK}" >/dev/null 2>&1 \
  || error "Network ${NETWORK} not found; start the carp-webservices ${PROFILE} stack first."

compose() {
  docker compose -f "${COMPOSE}" --env-file "${ENV}" -p "carp-gardener-${PROFILE}" "$@"
}

compose pull
# --remove-orphans drops containers of services renamed or removed since the last deploy.
compose up -d --no-build --remove-orphans
docker image prune -f >/dev/null
