mvn clean package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm -t rinha-backend-2025-jvm:latest .
docker tag rinha-backend-2025-jvm:latest 442494/rinha-backend-2025-jvm:latest