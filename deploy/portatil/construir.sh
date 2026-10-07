#!/usr/bin/env bash
# Compila la app (frontal Angular dentro del backend) y deja el resultado en ~/Apps/regalos3d/regalos3d.jar.
# Hazlo cada vez que cambie el código. Los tests van aparte: mvn verify.
set -euo pipefail
source "$(dirname "$0")/comun.sh"

echo "== Frontal"
cd "$RAIZ/frontend"
if [[ ! -d node_modules ]]; then
  # npm ci exige package-lock.json; sin él, npm install lo genera
  if [[ -f package-lock.json ]]; then npm ci; else npm install; fi
fi
npm run build:spring

echo "== Backend"
cd "$RAIZ"
mvn -q clean package -DskipTests

mkdir -p "$APP_DIR"
cp target/regalos3d-*.jar "$APP_DIR/regalos3d.jar"
echo "Listo: $APP_DIR/regalos3d.jar"
