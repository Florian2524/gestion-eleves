#!/usr/bin/env bash
# Recette navigateur sur des fixtures et processus jetables, indépendants de Compose.
set -euo pipefail
repo_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
runtime_dir=$(mktemp -d /tmp/gestion-eleves-e2e-XXXXXXXX)
test_container_id=""
backend_pid=""
frontend_pid=""
cleanup() {
  if [[ -n "$frontend_pid" ]]; then kill -- "-$frontend_pid" 2>/dev/null || true; wait "$frontend_pid" 2>/dev/null || true; fi
  if [[ -n "$backend_pid" ]]; then kill -- "-$backend_pid" 2>/dev/null || true; wait "$backend_pid" 2>/dev/null || true; fi
  if [[ -n "$test_container_id" ]]; then docker rm -f "$test_container_id" >/dev/null 2>&1 || true; fi
  rm -rf -- "$runtime_dir"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
free_port() { python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1]); s.close()'; }
backend_port=$(free_port)
frontend_port=$(free_port)
if [[ "$backend_port" == 8080 || "$frontend_port" == 5173 || "$backend_port" == "$frontend_port" ]]; then exit 1; fi
# Aucune connexion, aucun volume ou processus de développement n'est réutilisé.
test_container_id=$(docker run --detach --rm --tmpfs /var/lib/postgresql/data:rw \
  --publish 127.0.0.1::5432 --env POSTGRES_DB=ponderer_e2e \
  --env POSTGRES_USER=ponderer_e2e --env POSTGRES_PASSWORD=ponderer_e2e postgres:17)
for attempt in {1..30}; do
  if docker exec "$test_container_id" pg_isready -U ponderer_e2e -d ponderer_e2e >/dev/null 2>&1; then break; fi
  if [[ "$attempt" == 30 ]]; then exit 1; fi
  sleep 1
done
database_port=$(docker port "$test_container_id" 5432/tcp)
database_port=${database_port##*:}
if [[ ! "$database_port" =~ ^[0-9]+$ || "$database_port" == 5432 ]]; then exit 1; fi
database_url="jdbc:postgresql://127.0.0.1:${database_port}/ponderer_e2e"
export E2E_ADMIN_EMAIL="admin-e2e-$$@example.test"
export E2E_ADMIN_PASSWORD="IsolatedTest2026!$$"
export E2E_API_URL="http://127.0.0.1:${backend_port}"
export E2E_BASE_URL="http://127.0.0.1:${frontend_port}"
export E2E_ISOLATED_RUN=1
"$repo_dir/backend/mvnw" -f "$repo_dir/backend/pom.xml" -DskipTests package > "$runtime_dir/build.log" 2>&1 || { cat "$runtime_dir/build.log"; exit 1; }
setsid java -jar "$repo_dir/backend/target/backend-0.0.1-SNAPSHOT.jar" \
  --server.address=127.0.0.1 --server.port="$backend_port" \
  --spring.datasource.url="$database_url" --spring.flyway.url="$database_url" \
  --spring.datasource.username=ponderer_e2e --spring.datasource.password=ponderer_e2e \
  --spring.flyway.user=ponderer_e2e --spring.flyway.password=ponderer_e2e \
  --application.bootstrap.admin.email="$E2E_ADMIN_EMAIL" --application.bootstrap.admin.password="$E2E_ADMIN_PASSWORD" \
  --application.bootstrap.admin.nom=Test --application.bootstrap.admin.prenom=Admin \
  --application.upload.directory="$runtime_dir/uploads" > "$runtime_dir/backend.log" 2>&1 &
backend_pid=$!
setsid npm --prefix "$repo_dir/frontend" run dev -- --host 127.0.0.1 --port "$frontend_port" --strictPort \
  > "$runtime_dir/frontend.log" 2>&1 &
frontend_pid=$!
for attempt in {1..60}; do
  kill -0 "$backend_pid" "$frontend_pid" 2>/dev/null || { cat "$runtime_dir/backend.log" "$runtime_dir/frontend.log"; exit 1; }
  backend_status=$(curl -s -o /dev/null -w '%{http_code}' "$E2E_API_URL/matieres" || true)
  frontend_status=$(curl -s -o /dev/null -w '%{http_code}' "$E2E_BASE_URL/" || true)
  if [[ "$backend_status" == 401 && "$frontend_status" == 200 ]]; then break; fi
  if [[ "$attempt" == 60 ]]; then cat "$runtime_dir/backend.log" "$runtime_dir/frontend.log"; exit 1; fi
  sleep 1
done
printf 'Recette isolée : frontend %s, backend %s, PostgreSQL %s\n' "$E2E_BASE_URL" "$E2E_API_URL" "$database_url"
cd "$repo_dir/frontend"
npm run test:e2e -- "$@" || {
  result=$?
  mkdir -p test-results
  cp "$runtime_dir/backend.log" test-results/isolated-backend.log
  cp "$runtime_dir/frontend.log" test-results/isolated-frontend.log
  exit "$result"
}
