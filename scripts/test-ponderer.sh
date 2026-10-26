#!/usr/bin/env bash
# Toute la suite backend sur une instance PostgreSQL 17 jetable et indépendante de Compose.
set -euo pipefail
repo_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
test_container="gestion-eleves-ponderer-test-${RANDOM}-$$"
test_container_id=""
cleanup() {
  if [[ -n "$test_container_id" ]]; then docker rm -f "$test_container_id" >/dev/null 2>&1 || true; fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
# Aucun montage de volume ou répertoire de développement ; port choisi par Docker.
test_container_id=$(docker run --detach --rm --name "$test_container" \
  --tmpfs /var/lib/postgresql/data:rw \
  --publish 127.0.0.1::5432 \
  --env POSTGRES_DB=ponderer_test --env POSTGRES_USER=ponderer_test \
  --env POSTGRES_PASSWORD=ponderer_test postgres:17)
for attempt in {1..30}; do
  if docker exec "$test_container" pg_isready -U ponderer_test -d ponderer_test >/dev/null 2>&1; then break; fi
  if [[ "$attempt" == 30 ]]; then echo "PostgreSQL de tests indisponible." >&2; exit 1; fi
  sleep 1
done
test_port=$(docker port "$test_container" 5432/tcp)
test_port=${test_port##*:}
if [[ ! "$test_port" =~ ^[0-9]+$ || "$test_port" == 5432 ]]; then
  echo "Port de tests non isolé ; arrêt." >&2; exit 1
fi
export PONDERER_TEST_DB_URL="jdbc:postgresql://127.0.0.1:${test_port}/ponderer_test"
printf 'Tests sur %s (conteneur jetable %s)\n' "$PONDERER_TEST_DB_URL" "$test_container"
"$repo_dir/backend/mvnw" -f "$repo_dir/backend/pom.xml" test "$@"
