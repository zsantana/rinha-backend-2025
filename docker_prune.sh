#!/bin/bash
set -e

echo "🛑 Parando todos os containers..."
docker stop $(docker ps -aq) 2>/dev/null || true

echo "🗑️ Removendo todos os containers..."
docker rm -f $(docker ps -aq) 2>/dev/null || true

echo "🖼️ Removendo todas as imagens..."
docker rmi -f $(docker images -q) 2>/dev/null || true

echo "📦 Removendo todos os volumes..."
docker volume rm $(docker volume ls -q) 2>/dev/null || true

echo "🌐 Removendo todas as redes (exceto as padrão)..."
docker network rm $(docker network ls -q | grep -vE "^(|bridge|host|none)$") 2>/dev/null || true

echo "✅ Docker zerado com sucesso!"
