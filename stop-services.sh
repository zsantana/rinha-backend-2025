#!/bin/bash

set -e

echo "⚠️ Parando e removendo stack: payment-processor (com volumes)"
docker compose -f docker-compose-payment-processor.yml down --volumes --remove-orphans || true

echo "⚠️ Parando e removendo stack: produtor-pagamento (com volumes)"
docker compose -f docker-compose.yml down --volumes --remove-orphans || true

echo "ℹ️  Redes compartilhadas podem não ser removidas se ainda estiverem em uso por outros containers."
echo "✅ Todos os containers dos stacks foram parados."
