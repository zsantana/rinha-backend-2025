#!/bin/bash

set -e

echo "🧹 Limpando e subindo stack: payment-processor"
docker compose -f docker-compose-pagamento.yml up -d --remove-orphans


echo "✅ Todos os serviços foram limpos e recriados."
