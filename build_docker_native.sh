#!/bin/bash
set -e

./clean_docker_rinha.sh

echo "🔨 Buildando projetos produto/consumidor ..."
mvn clean package -Pnative -DskipTests

# build consumidor
cd consumidor
echo "🔨 Buildando consumidor..."
docker build --no-cache -f src/main/docker/Dockerfile.native -t rinha-backend-2025-consumidor:v7 .
cd ..

# build produtor
echo "🔨 Buildando produtor..."
cd produtor
docker build --no-cache -f src/main/docker/Dockerfile.native -t rinha-backend-2025-produtor:v7 .
cd ..
