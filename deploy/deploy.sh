#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="$ROOT_DIR/deploy"
ENV_FILE="${DEPLOY_ENV_FILE:-$DEPLOY_DIR/.env}"
STATE_FILE="$DEPLOY_DIR/.deploy.env"
COMPOSE=(docker compose --env-file "$ENV_FILE" --env-file "$STATE_FILE" -f "$DEPLOY_DIR/docker-compose.yml")

usage() { printf 'Usage: %s {deploy|up|down|logs|status|rollback} [version]\n' "$0"; }
require_tools() { command -v docker >/dev/null || { echo 'Docker is required.' >&2; exit 1; }; docker compose version >/dev/null; }
require_env() { [[ -f "$ENV_FILE" ]] || { echo "Missing $ENV_FILE. Copy deploy/.env.example to deploy/.env and fill secrets." >&2; exit 1; }; }
validate_env() {
  if grep -Eq '(^|=)replace-with-' "$ENV_FILE"; then
    echo "Deployment secrets in $ENV_FILE still contain replace-with- placeholders." >&2
    exit 1
  fi
}
write_version() { printf 'APP_VERSION=%s\n' "$1" > "$STATE_FILE"; }

main() {
  local action="${1:-deploy}" version="${2:-$(date -u +%Y%m%d%H%M%S)}"
  [[ "$action" =~ ^(deploy|up|down|logs|status|rollback)$ ]] || { usage; exit 2; }
  require_tools; require_env; validate_env
  [[ -f "$STATE_FILE" ]] || write_version "local"
  case "$action" in
    deploy) write_version "$version"; "${COMPOSE[@]}" build --pull; "${COMPOSE[@]}" up -d --remove-orphans; "${COMPOSE[@]}" ps ;;
    up) [[ -f "$STATE_FILE" ]] || write_version "local"; "${COMPOSE[@]}" up -d --remove-orphans; "${COMPOSE[@]}" ps ;;
    down) "${COMPOSE[@]}" down ;;
    logs) if [[ -n "${2:-}" ]]; then "${COMPOSE[@]}" logs -f --tail=200 "$2"; else "${COMPOSE[@]}" logs -f --tail=200; fi ;;
    status) "${COMPOSE[@]}" ps ;;
    rollback) [[ -n "${2:-}" ]] || { echo 'rollback requires a previously built version, for example 20261025213000.' >&2; exit 2; }; write_version "$2"; "${COMPOSE[@]}" up -d --no-build --remove-orphans; "${COMPOSE[@]}" ps ;;
  esac
}
main "$@"
