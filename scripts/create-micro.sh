#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# create-micro.sh
# Scaffold de un micro hexagonal para el ecosistema ecommerce.
#
# Uso:
#   ./create-micro.sh <nombre> [--producer] [--port N] [--no-compile]
#
# Ejemplos:
#   ./create-micro.sh payment-service
#   ./create-micro.sh notification-service --producer --port 8085
#
# Responsabilidades:
#   - Este script = LOGICA (orquestar, derivar nombres, elegir puerto).
#   - scripts/templates/ = CONTENIDO (las plantillas con placeholders __X__).
#   Separar las dos cosas permite tocar una sin romper la otra.
# ============================================================

# Directorio donde vive este script (funciona aunque lo llames desde otro lado)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATES_DIR="$SCRIPT_DIR/templates"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# Fuente del wrapper de Maven: un micro que YA sabemos que compila
TEMPLATE_SOURCE="$ROOT_DIR/order-service"

# ---------- 1. Argumentos ----------
MICRO="${1:-}"
PRODUCER=false
PORT=""
COMPILE=true

if [[ -z "$MICRO" ]]; then
  echo "Uso: $0 <nombre-del-micro> [--producer] [--port N] [--no-compile]"
  echo "Ejemplo: $0 payment-service"
  exit 1
fi
shift

while [[ $# -gt 0 ]]; do
  case "$1" in
    --producer)   PRODUCER=true ;;
    --port)       PORT="$2"; shift ;;
    --no-compile) COMPILE=false ;;
    *) echo "Flag desconocido: $1"; exit 1 ;;
  esac
  shift
done

# ---------- 2. Derivados del nombre ----------
# payment-service -> base=payment -> pkg=payment -> class=PaymentServiceApplication
BASE="${MICRO%-service}"
PKG="$(printf '%s' "$BASE" | tr -d '-')"
PASCAL="$(printf '%s' "$BASE" | sed -E 's/-([a-z])/\U\1/g; s/^([a-z])/\U\1/')"
CLASS="${PASCAL}ServiceApplication"
DESC="${PASCAL} microservice. Hexagonal Architecture + DDD."

# ---------- 3. Seguridad: no pisar nada ----------
if [[ -d "$ROOT_DIR/$MICRO" ]]; then
  echo "ERROR: $ROOT_DIR/$MICRO ya existe. No se sobrescribe."
  exit 1
fi

# ---------- 4. Puerto: el máximo de los existentes + 1 ----------
if [[ -z "$PORT" ]]; then
  MAX_PORT="$(grep -rhoE 'port: *[0-9]+' "$ROOT_DIR"/*/src/main/resources/application.yml 2>/dev/null \
    | grep -oE '[0-9]+' | sort -n | tail -1 || true)"
  PORT=$(( ${MAX_PORT:-8080} + 1 ))
fi

echo ">>> micro   : $MICRO"
echo ">>> package : com.ecommerce.$PKG"
echo ">>> clase   : $CLASS"
echo ">>> puerto  : $PORT"

# ---------- 5. Carpetas del micro ----------
mkdir -p "$ROOT_DIR/$MICRO"
cd "$ROOT_DIR/$MICRO"

# ---------- 6. Wrapper de Maven (copiado, no inventado) ----------
cp "$TEMPLATE_SOURCE/mvnw" .
cp "$TEMPLATE_SOURCE/mvnw.cmd" .
cp -r "$TEMPLATE_SOURCE/.mvn" .

# ---------- 7. Mini motor de plantillas ----------
# Reemplaza los placeholders __X__ de una plantilla y escribe el resultado
# en la salida estándar. El "|" como separador de sed evita chocar con
# los "/" que hay en rutas y nombres de paquetes.
render() {
  sed -e "s|__MICRO__|$MICRO|g" \
      -e "s|__PKG__|$PKG|g" \
      -e "s|__PASCAL__|$PASCAL|g" \
      -e "s|__CLASS__|$CLASS|g" \
      -e "s|__DESC__|$DESC|g" \
      -e "s|__PORT__|$PORT|g" \
      "$TEMPLATES_DIR/$1"
}

# ---------- 8. Render de los archivos ----------
render pom.xml.txt > pom.xml

mkdir -p src/main/resources
if [[ "$PRODUCER" == true ]]; then
  render application-producer.yml.txt > src/main/resources/application.yml
else
  render application-consumer.yml.txt > src/main/resources/application.yml
fi

mkdir -p "src/main/java/com/ecommerce/$PKG"
render MainClass.java.txt > "src/main/java/com/ecommerce/$PKG/$CLASS.java"

render Dockerfile.txt > Dockerfile

# ---------- 9. Punto de control ----------
if [[ "$COMPILE" == true ]]; then
  echo ">>> Punto de control: compilando $MICRO..."
  ./mvnw -q compile
  echo ">>> BUILD SUCCESS"
fi

echo
echo "Listo. Próximos pasos:"
echo "  1. IntelliJ: Maven tool window -> '+' -> $MICRO/pom.xml"
echo "  2. Capa domain -> application -> infrastructure (receta hexagonal)"
echo "  3. ./mvnw verify cuando haya tests"
