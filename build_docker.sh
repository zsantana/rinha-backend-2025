#!/bin/bash
set -e

echo "🔨 Buildando projetos produto/consumidor ..."
mvn clean package -DskipTests

# build consumidor
echo "🔨 Buildando consumidor..."
cd consumidor
docker build --no-cache -f src/main/docker/Dockerfile.jvm -t rinha-backend-2025-consumidor:v2 .
cd ..

# build produtor
echo "🔨 Buildando produtor..."
cd produtor
docker build --no-cache -f src/main/docker/Dockerfile.jvm -t rinha-backend-2025-produtor:v2 .
cd ..
