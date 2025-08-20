#!/bin/bash

# Duração em segundos
DURATION=10
END=$((SECONDS+DURATION))

while [ $SECONDS -lt $END ]; do
  UUID=$(uuidgen)

  curl -s -X POST http://localhost:9999/payments \
    -H "Content-Type: application/json" \
    -d "{
      \"correlationId\": \"$UUID\",
      \"amount\": 1
    }" >/dev/null &

done

# Espera todas as requisições terminarem
wait
