#!/bin/bash

echo "⛔ Parando todos os containers relacionados a 'rinha'..."
docker ps -a --filter "name=rinha" -q | xargs -r docker stop

echo "🧹 Removendo containers relacionados a 'rinha'..."
docker ps -a --filter "name=rinha" -q | xargs -r docker rm

echo "🗑️ Removendo imagens relacionadas a 'rinha'..."
docker images | grep rinha | awk '{print $3}' | sort -u | xargs -r docker rmi -f

echo "📦 Removendo volumes não utilizados..."
docker volume prune -f

echo "✅ Limpeza concluída com sucesso!"
