# Funciones compartidas por los scripts de esta carpeta (no se ejecuta solo).

CARPETA="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ="$(cd "$CARPETA/../.." && pwd)"
APP_DIR="${REGALOS3D_APP:-$HOME/Apps/regalos3d}"
CONTENEDOR_BD="regalos3d-db"

cargar_env() {
  if [[ ! -f "$CARPETA/.env" ]]; then
    echo "Falta $CARPETA/.env  →  cp env.example .env  y rellénalo" >&2
    exit 1
  fi
  set -a
  # shellcheck source=/dev/null
  source "$CARPETA/.env"
  set +a
  if grep -q CAMBIAR "$CARPETA/.env"; then
    echo "El .env aún tiene valores CAMBIAR_…: rellénalo antes de seguir" >&2
    exit 1
  fi
}
