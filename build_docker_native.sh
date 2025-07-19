#!/bin/bash
set -e

echo "🔨 Buildando projetos produto/consumidor ..."
mvn clean package -Pnative -DskipTests

# build consumidor
cd consumidor
echo "🔨 Buildando consumidor..."
docker build --no-cache -f src/main/docker/Dockerfile.native -t rinha-backend-2025-consumidor:latest .
docker tag rinha-backend-2025-consumidor:latest 442494/rinha-backend-2025-consumidor:latest
cd ..

# build produtor
echo "🔨 Buildando produtor..."
cd produtor
docker build --no-cache -f src/main/docker/Dockerfile.native -t rinha-backend-2025-produtor:latest .
docker tag rinha-backend-2025-produtor:latest 442494/rinha-backend-2025-produtor:latest
cd ..
