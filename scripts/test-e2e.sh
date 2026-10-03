#!/usr/bin/env bash

set -euo pipefail

PROJECT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="$PROJECT/backend"
FRONTEND="$PROJECT/frontend"

BACKEND_LOG="/tmp/gestion-eleves-backend.log"
FRONTEND_LOG="/tmp/gestion-eleves-frontend.log"

E2E_ADMIN_EMAIL="${E2E_ADMIN_EMAIL:-admin@gestion-eleves.local}"
E2E_ADMIN_PASSWORD="${E2E_ADMIN_PASSWORD:-AdminTest2026!}"

echo
echo "========================================"
echo "  GESTION ÉLÈVES - RECETTE E2E"
echo "========================================"

echo
echo "=== PostgreSQL ==="

cd "$PROJECT"
docker compose up -d --wait

echo "✅ PostgreSQL opérationnel"

echo
echo "=== Backend ==="

BACKEND_CODE="$(
  curl -s -o /dev/null -w "%{http_code}" \
    http://127.0.0.1:8080/matieres 2>/dev/null || true
)"

if [ "$BACKEND_CODE" != "401" ]; then
  cd "$BACKEND"

  export JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 32)}"
  export BOOTSTRAP_ADMIN_EMAIL="$E2E_ADMIN_EMAIL"
  export BOOTSTRAP_ADMIN_PASSWORD="$E2E_ADMIN_PASSWORD"
  export BOOTSTRAP_ADMIN_NOM="${BOOTSTRAP_ADMIN_NOM:-Administrateur}"
  export BOOTSTRAP_ADMIN_PRENOM="${BOOTSTRAP_ADMIN_PRENOM:-Test}"

  nohup ./mvnw spring-boot:run \
    > "$BACKEND_LOG" 2>&1 &

  echo "Backend lancé."
else
  echo "Backend déjà actif."
fi

BACKEND_READY=false

for _ in $(seq 1 60); do
  CODE="$(
    curl -s -o /dev/null -w "%{http_code}" \
      http://127.0.0.1:8080/matieres 2>/dev/null || true
  )"

  if [ "$CODE" = "401" ]; then
    BACKEND_READY=true
    break
  fi

  sleep 2
done

if [ "$BACKEND_READY" != "true" ]; then
  echo "❌ Backend indisponible"
  tail -n 100 "$BACKEND_LOG" 2>/dev/null || true
  exit 1
fi

echo "✅ Backend opérationnel"

echo
echo "=== Frontend ==="

FRONTEND_CODE="$(
  curl -s -o /dev/null -w "%{http_code}" \
    http://127.0.0.1:5173/ 2>/dev/null || true
)"

if [ "$FRONTEND_CODE" != "200" ]; then
  cd "$FRONTEND"

  nohup npm run dev -- --host 127.0.0.1 \
    > "$FRONTEND_LOG" 2>&1 &

  echo "Frontend lancé."
else
  echo "Frontend déjà actif."
fi

FRONTEND_READY=false

for _ in $(seq 1 30); do
  CODE="$(
    curl -s -o /dev/null -w "%{http_code}" \
      http://127.0.0.1:5173/ 2>/dev/null || true
  )"

  if [ "$CODE" = "200" ]; then
    FRONTEND_READY=true
    break
  fi

  sleep 2
done

if [ "$FRONTEND_READY" != "true" ]; then
  echo "❌ Frontend indisponible"
  tail -n 100 "$FRONTEND_LOG" 2>/dev/null || true
  exit 1
fi

echo "✅ Frontend opérationnel"

echo
echo "=== Playwright ==="

cd "$FRONTEND"

export E2E_ADMIN_EMAIL
export E2E_ADMIN_PASSWORD

if npm run test:e2e; then
  echo
  echo "========================================"
  echo "✅ RECETTE E2E RÉUSSIE"
  echo "========================================"
  echo "Site  : http://localhost:5173"
  echo "Admin : http://localhost:5173/espace"
else
  RESULT=$?

  echo
  echo "========================================"
  echo "❌ RECETTE E2E EN ÉCHEC"
  echo "========================================"
  echo
  echo "Rapport Playwright :"
  echo "cd $FRONTEND && npx playwright show-report"

  exit "$RESULT"
fi
