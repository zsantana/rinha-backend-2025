#!/bin/bash
set -e

echo "🔨 Deploy projetos produto/consumidor ..."

docker tag rinha-backend-2025-consumidor:v2 442494/rinha-backend-2025-consumidor:v4
docker tag rinha-backend-2025-produtor:v2 442494/rinha-backend-2025-produtor:v4

docker push 442494/rinha-backend-2025-produtor:v4
docker push 442494/rinha-backend-2025-consumidor:v4

echo "🔨 Deploy concluído com sucesso!"
