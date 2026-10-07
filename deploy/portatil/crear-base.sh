#!/usr/bin/env bash
# Crea, UNA SOLA VEZ, la base de datos real: contenedor MariaDB "regalos3d-db" en 127.0.0.1:3308,
# con los datos en el volumen "regalos3d-datos". La de desarrollo (3307) no se toca.
set -euo pipefail
source "$(dirname "$0")/comun.sh"
cargar_env

if podman container exists "$CONTENEDOR_BD"; then
  echo "La base ya existe ($CONTENEDOR_BD). Para arrancarla: podman start $CONTENEDOR_BD"
  exit 0
fi

podman run -d --name "$CONTENEDOR_BD" --restart unless-stopped \
  -e MARIADB_DATABASE=regalos_db \
  -e MARIADB_USER=regalos \
  -e MARIADB_PASSWORD="$DB_PASSWORD" \
  -e MARIADB_ROOT_PASSWORD="$DB_ROOT_PASSWORD" \
  -p 127.0.0.1:3308:3306 \
  -v regalos3d-datos:/var/lib/mysql \
  --health-cmd 'healthcheck.sh --connect --innodb_initialized' --health-interval 10s \
  docker.io/library/mariadb:10.11

echo "Base creada. Las tablas las crea la app la primera vez que arranque (./arrancar.sh)."
