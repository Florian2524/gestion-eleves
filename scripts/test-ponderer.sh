#!/usr/bin/env bash
# Toute la suite backend sur une instance PostgreSQL 17 jetable et indépendante de Compose.
set -euo pipefail
repo_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
test_container="gestion-eleves-ponderer-test-${RANDOM}-$$"
test_container_id=""
test_runtime_dir=$(mktemp -d /tmp/gestion-eleves-java-XXXXXXXX)
cleanup() {
  if [[ -n "$test_container_id" ]]; then docker rm -f "$test_container_id" >/dev/null 2>&1 || true; fi
  rm -rf -- "$test_runtime_dir"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
# Aucun montage de volume ou répertoire de développement ; port choisi par Docker.
test_container_id=$(docker run --detach --rm --name "$test_container" \
  --tmpfs /var/lib/postgresql/data:rw \
  --publish 127.0.0.1::5432 \
  --env POSTGRES_DB=postgres --env POSTGRES_USER=isolated_cluster_admin \
  --env POSTGRES_PASSWORD=isolated_cluster_only postgres:17)
for attempt in {1..30}; do
  if docker exec "$test_container" pg_isready -U isolated_cluster_admin -d postgres >/dev/null 2>&1; then break; fi
  if [[ "$attempt" == 30 ]]; then echo "PostgreSQL de tests indisponible." >&2; exit 1; fi
  sleep 1
done
# Le compte d'amorçage reste réservé à ce conteneur jetable ; Flyway et JPA ne l'utilisent pas.
docker exec -i "$test_container_id" psql -X -v ON_ERROR_STOP=1 -U isolated_cluster_admin -d postgres >/dev/null <<'SQL'
CREATE ROLE ponderer_test LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD 'ponderer_test';
CREATE DATABASE ponderer_test OWNER ponderer_test;
SQL
if [[ $(docker exec "$test_container_id" psql -X -qAt -U ponderer_test -d ponderer_test -c \
  "SELECT rolsuper OR rolcreatedb OR rolcreaterole FROM pg_roles WHERE rolname=current_user") != f ]]; then
  echo "Compte applicatif de tests trop privilégié ; arrêt." >&2; exit 1
fi
test_port=$(docker port "$test_container" 5432/tcp)
test_port=${test_port##*:}
if [[ ! "$test_port" =~ ^[0-9]+$ || "$test_port" == 5432 ]]; then
  echo "Port de tests non isolé ; arrêt." >&2; exit 1
fi
export PONDERER_TEST_DB_URL="jdbc:postgresql://127.0.0.1:${test_port}/ponderer_test"
printf 'Tests sur %s (conteneur jetable %s)\n' "$PONDERER_TEST_DB_URL" "$test_container"
"$repo_dir/backend/mvnw" -f "$repo_dir/backend/pom.xml" test \
  -Dapplication.upload.directory="$test_runtime_dir/uploads" "$@"
