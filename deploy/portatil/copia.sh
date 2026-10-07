#!/usr/bin/env bash
# Copia de seguridad de la base real de Regalos 3D (contenedor regalos3d-db).
# Un volcado comprimido por ejecución; se conservan los 30 más recientes.
#
#   ./copia.sh                      → en ~/Copias/regalos3d
#   ./copia.sh /otra/carpeta
#
# Restaurar una copia (sustituye los datos actuales):
#   gunzip -c ARCHIVO.sql.gz | podman exec -i regalos3d-db sh -c 'exec mariadb -uroot -p"$MARIADB_ROOT_PASSWORD" regalos_db'
set -euo pipefail

destino="${1:-$HOME/Copias/regalos3d}"
mkdir -p "$destino"
chmod 700 "$destino"

archivo="$destino/regalos_db-$(date +%F_%H%M%S).sql.gz"
temporal="$archivo.parcial"
trap 'rm -f "$temporal"' EXIT

# La contraseña de root la lee el propio contenedor de su entorno: no pasa por esta terminal
podman exec regalos3d-db sh -c \
  'exec mariadb-dump -uroot -p"$MARIADB_ROOT_PASSWORD" --single-transaction --routines --triggers regalos_db' \
  | gzip > "$temporal"

chmod 600 "$temporal"
mv "$temporal" "$archivo"

# Rotación: fuera las copias a partir de la 31
ls -1t "$destino"/regalos_db-*.sql.gz | tail -n +31 | xargs -r rm --

echo "Copia guardada: $archivo ($(du -h "$archivo" | cut -f1))"
