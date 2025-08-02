#!/bin/bash
set -e

echo "🔨 Deploy projetos produto/consumidor ..."

docker tag rinha-backend-2025-consumidor:v2 442494/rinha-backend-2025-consumidor:v2
docker tag rinha-backend-2025-produtor:v2 442494/rinha-backend-2025-produtor:v2

docker push 442494/rinha-backend-2025-produtor:v2
docker push 442494/rinha-backend-2025-consumidor:v2

echo "🔨 Deploy concluído com sucesso!"
