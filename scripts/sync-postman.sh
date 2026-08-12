#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────────────────────
# sync-postman.sh — empuja la colección a Postman via API
#
# Uso:
#   ./scripts/sync-postman.sh
#
# Requiere:
#   export POSTMAN_API_KEY="tu-api-key-de-postman"
#   export POSTMAN_COLLECTION_UID="uid-de-la-coleccion"
#
# Cómo conseguir el UID:
#   1. Importá request/hexagonal-micro.postman_collection.json manualmente en Postman
#   2. Abrí la colección → click derecho → Copy ID (o mirá la URL)
#   3. Pegalo en POSTMAN_COLLECTION_UID
# ─────────────────────────────────────────────────────────────

COLLECTION_FILE="request/hexagonal-micro.postman_collection.json"

if [ ! -f "$COLLECTION_FILE" ]; then
  echo "ERROR: No encontré $COLLECTION_FILE"
  exit 1
fi

if [ -z "${POSTMAN_API_KEY:-}" ]; then
  echo "ERROR: Falta POSTMAN_API_KEY"
  echo "  Generala en https://postman.com/me/settings → API Keys"
  echo "  export POSTMAN_API_KEY=\"tu-key\""
  exit 1
fi

if [ -z "${POSTMAN_COLLECTION_UID:-}" ]; then
  echo "ERROR: Falta POSTMAN_COLLECTION_UID"
  echo "  Importá la colección manualmente una vez en Postman"
  echo "  Después copiá el UID de la colección (click derecho → Copy ID)"
  echo "  export POSTMAN_COLLECTION_UID=\"el-uid\""
  exit 1
fi

echo "Sincronizando $COLLECTION_FILE → Postman (UID: $POSTMAN_COLLECTION_UID)"

# Postman API requiere el JSON envuelto en { "collection": { ... } }
PAYLOAD=$(jq --null-input \
  --argjson col "$(cat "$COLLECTION_FILE")" \
  '{ "collection": { "info": { "name": ($col.info.name), "schema": ($col.info.schema) }, "item": ($col.item) } }')

RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X PUT "https://api.getpostman.com/collections/${POSTMAN_COLLECTION_UID}" \
  -H "X-Api-Key: ${POSTMAN_API_KEY}" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
  echo "OK"
  NAME=$(echo "$BODY" | jq -r '.collection.name // "unknown"')
  echo "Colección actualizada: $NAME"
else
  echo "ERROR ($HTTP_CODE)"
  echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
  exit 1
fi