#!/bin/bash

set -e

TEST_DIR="rinha-test"
SCRIPT="rinha.js"

if [ ! -d "$TEST_DIR" ]; then
  echo "❌ Diretório '$TEST_DIR' não encontrado."
  exit 1
fi

if [ ! -f "$TEST_DIR/$SCRIPT" ]; then
  echo "❌ Script de teste '$SCRIPT' não encontrado dentro de '$TEST_DIR'."
  exit 1
fi

echo "🚀 Executando testes com K6 em '$TEST_DIR/$SCRIPT'..."
cd "$TEST_DIR"
k6 run -e MAX_REQUESTS=850 "$SCRIPT"
cd -

echo "✅ Teste finalizado."
