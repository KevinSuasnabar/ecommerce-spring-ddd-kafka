#!/usr/bin/env bash
# serve-postman.sh — sirve la colección por HTTP para que Postman la linkee
# Postman detecta cambios y muestra "Update available"
set -euo pipefail

DIR="request"
PORT=9999

echo "Sirviendo $DIR en http://localhost:$PORT"
echo ""
echo "En Postman: Import → Link → http://localhost:$PORT/hexagonal-micro.postman_collection.json"
echo ""
echo "Ctrl+C para detener"
echo ""

cd "$DIR"
python3 -m http.server "$PORT"