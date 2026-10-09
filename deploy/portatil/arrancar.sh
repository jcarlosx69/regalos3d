#!/usr/bin/env bash
# Arranca la app real en http://localhost:8086 con la base de datos real. Ctrl+C para pararla.
set -euo pipefail
source "$(dirname "$0")/comun.sh"
cargar_env

jar="$APP_DIR/regalos3d.jar"
[[ -f "$jar" ]] || { echo "No existe $jar: ejecuta antes ./construir.sh" >&2; exit 1; }
podman container exists "$CONTENEDOR_BD" || { echo "No existe la base: ejecuta antes ./crear-base.sh" >&2; exit 1; }

# La base puede estar parada (por ejemplo, tras reiniciar el portátil): se arranca y se espera a que responda
podman start "$CONTENEDOR_BD" >/dev/null
for _ in $(seq 60); do
  podman healthcheck run "$CONTENEDOR_BD" >/dev/null 2>&1 && break
  sleep 1
done

export DB_URL="jdbc:mariadb://127.0.0.1:3308/regalos_db"
export DB_USER="regalos"
export SERVER_ADDRESS="127.0.0.1"    # solo desde el propio portátil
export SERVER_PORT="8086"            # el 8085 queda para desarrollar
export MANYFOLD_URL="${MANYFOLD_URL:-http://localhost:3214}"   # se define en .env
unset DB_ROOT_PASSWORD               # la app no la necesita

echo "Regalos 3D en http://localhost:8086  (Ctrl+C para parar)"
exec java -Xms128m -Xmx320m -Duser.timezone=Atlantic/Canary -jar "$jar"
