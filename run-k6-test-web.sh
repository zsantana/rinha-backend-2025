#!/bin/bash

set -e

export K6_WEB_DASHBOARD=true
export K6_WEB_DASHBOARD_PORT=5665
export K6_WEB_DASHBOARD_PERIOD=2s
export K6_WEB_DASHBOARD_OPEN=true
export K6_WEB_DASHBOARD_EXPORT='report.html'

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
k6 run "$SCRIPT"
cd -

echo "✅ Teste finalizado."
