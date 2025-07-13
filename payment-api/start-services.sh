#!/bin/bash

set -e

echo "🧹 Limpando e subindo stack: payment-processor"
docker compose -f docker-compose-payment-processor.yml up -d --remove-orphans

echo "🧹 Limpando e subindo stack: produtor-pagamento"
docker compose -f docker-compose.yml up -d

echo "✅ Todos os serviços foram limpos e recriados."
