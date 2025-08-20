#!/bin/bash
set -e

echo "🔨 Deploy projetos produto/consumidor ..."

docker tag rinha-backend-2025-consumidor:v7 442494/rinha-backend-2025-consumidor:v7
docker tag rinha-backend-2025-produtor:v7 442494/rinha-backend-2025-produtor:v7

docker push 442494/rinha-backend-2025-produtor:v7
docker push 442494/rinha-backend-2025-consumidor:v7

echo "🔨 Deploy concluído com sucesso!"
