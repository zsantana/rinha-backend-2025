#!/bin/bash
set -e

# build consumidor
echo "🔨 Buildando consumidor..."
cd consumidor
mvn clean package -Pnative -DskipTests
docker build --no-cache -f src/main/docker/Dockerfile.native -t rinha-backend-2025-consumidor:latest .
docker tag rinha-backend-2025-consumidor:latest 442494/rinha-backend-2025-consumidor:latest
cd ..

# build produtor
echo "🔨 Buildando produtor..."
cd produtor
mvn clean package -Pnative -DskipTests
docker build --no-cache -f src/main/docker/Dockerfile.native -t rinha-backend-2025-produtor:latest .
docker tag rinha-backend-2025-produtor:latest 442494/rinha-backend-2025-produtor:latest
cd ..
