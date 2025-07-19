#!/bin/bash

# Verifica se o caminho do log foi passado
if [ -z "$1" ]; then
  echo "Uso: $0 caminho/para/arquivo.log"
  exit 1
fi

LOG_FILE="$1"
BUSCA="Erro ao salvar payment no Redis"

# Verifica se o arquivo existe
if [ ! -f "$LOG_FILE" ]; then
  echo "Arquivo de log '$LOG_FILE' não encontrado."
  exit 1
fi

# Busca a string parcial no log (como LIKE)
echo "🔍 Buscando erros no log..."
grep -i "$BUSCA" "$LOG_FILE"

# Verifica se encontrou algo
if [ $? -ne 0 ]; then
  echo "✅ Nenhum erro encontrado relacionado a: '$BUSCA'"
else
  echo "❗ Foram encontrados erros relacionados a: '$BUSCA'"
fi
